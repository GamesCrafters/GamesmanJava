package edu.berkeley.gamesman.database;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.ErrorThread;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class RemoteDatabase extends Database {
	private final String user, server, confFile, gamesmanPath, remoteFile;
	private int maxCommandLen = -1;

	public RemoteDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords, DatabaseHeader header) {
		this(uri, conf, solve, firstRecord, numRecords, header, conf
				.getProperty("gamesman.remote.server"), conf
				.getProperty("gamesman.remote.db.uri"));
	}

	public RemoteDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords, DatabaseHeader header,
			String server, String remoteFile) {
		super(uri, conf, solve, firstRecord, numRecords, header);
		user = conf.getProperty("gamesman.remote.user", null);
		this.server = server;
		gamesmanPath = conf.getProperty("gamesman.remote.path");
		String confFile = conf.getProperty("gamesman.remote.confFile", null);
		if (confFile != null && !confFile.startsWith("/")
				&& !confFile.startsWith(gamesmanPath))
			confFile = gamesmanPath + "/" + confFile;
		this.confFile = confFile;
		if (!remoteFile.startsWith("/") && !remoteFile.startsWith(gamesmanPath))
			remoteFile = gamesmanPath + "/" + remoteFile;
		this.remoteFile = remoteFile;
	}

	@Override
	public void close() {
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int numBytes) {
		getRecordsAsBytes(dh, loc, 0, arr, off, numBytes, 0, true);
	}

	@Override
	protected void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		StringBuilder command;
		if (maxCommandLen >= 0)
			command = new StringBuilder(maxCommandLen);
		else
			command = new StringBuilder();
		command.append("ssh ");
		if (user != null) {
			command.append(user);
			command.append("@");
		}
		command.append(server);
		command.append(" java -cp ");
		command.append(gamesmanPath);
		command.append("/bin edu.berkeley.gamesman.database.ReadRecords ");
		if (confFile != null) {
			command.append(confFile);
			command.append(" ");
		}
		command.append(remoteFile);
		command.append(" ");
		long firstRecord = toFirstRecord(byteIndex);
		long lastRecord = toLastRecord(byteIndex + numBytes);
		firstRecord += firstNum;
		if (lastNum > 0)
			lastRecord -= recordsPerGroup - lastNum;
		long numRecords = lastRecord - firstRecord;
		command.append(firstRecord);
		command.append(" ");
		command.append(numRecords);
		if (maxCommandLen < command.length())
			maxCommandLen = command.length();
		try {
			Process p = Runtime.getRuntime().exec(command.toString());
			new ErrorThread(p.getErrorStream(), server).start();
			((RemoteHandle) dh).is = new BufferedInputStream(
					p.getInputStream(), ReadRecords.BUFFER_SIZE);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected int getBytes(DatabaseHandle dh, byte[] arr, int off, int maxLen,
			boolean overwriteEdgesOk) {
		if (!overwriteEdgesOk) {
			return super.getBytes(dh, arr, off, maxLen, false);
		} else {
			final int numBytes = (int) Math.min(maxLen, dh.lastByteIndex
					- dh.location);
			try {
				readFully(((RemoteHandle) dh).is, arr, off, numBytes);
				dh.location += numBytes;
				if (dh.location == dh.lastByteIndex)
					((RemoteHandle) dh).is.close();
			} catch (IOException e) {
				throw new Error(e);
			}
			return numBytes;
		}
	}

	@Override
	public RemoteHandle getHandle() {
		return new RemoteHandle(recordGroupByteLength);
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}

	// public static void main(String[] args) throws IOException {
	// byte[] headerBytes = new byte[18];
	// FileInputStream fis = new FileInputStream(args[0]);
	// readFully(fis, headerBytes, 0, 18);
	// System.out.write(headerBytes);
	// if (args.length > 1 && Boolean.parseBoolean(args[1])) {
	// byte[] confBytes = Configuration.loadBytes(fis);
	// System.out.write(confBytes);
	// }
	// fis.close();
	// System.out.flush();
	// }

	private static Pair<DatabaseHeader, Configuration> remoteHeaderConf(
			String user, String host, String file, boolean withConf) {
		try {
			int numBytes = withConf ? 22 : 18;
			String commandString = "ssh "
					+ (user == null ? host : (user + "@" + host)) + " dd if="
					+ file + " count=";
			byte[] headerBytes = new byte[numBytes];
			String command = commandString + 1;
			assert Util.debug(DebugFacility.DATABASE, command);
			Process p = Runtime.getRuntime().exec(command);
			new ErrorThread(p.getErrorStream(), host).start();
			InputStream is = p.getInputStream();
			Database.readFully(is, headerBytes, 0, numBytes);
			is.close();
			DatabaseHeader dh = new DatabaseHeader(headerBytes);
			if (withConf) {
				int confLength = 0;
				for (int i = 18; i < 22; i++) {
					confLength <<= 8;
					confLength |= headerBytes[i];
				}
				command = commandString + ((numBytes + confLength + 511) >> 9);
				assert Util.debug(DebugFacility.DATABASE, command);
				p = Runtime.getRuntime().exec(command);
				new ErrorThread(p.getErrorStream(), host).start();
				is = p.getInputStream();
				Database.readFully(is, headerBytes, 0, numBytes);
				byte[] confBytes = new byte[confLength];
				Database.readFully(is, confBytes, 0, confLength);
				is.close();
				ByteArrayInputStream bais = new ByteArrayInputStream(confBytes);
				Properties props = new Properties();
				props.load(bais);
				Configuration conf = new Configuration(props);
				return new Pair<DatabaseHeader, Configuration>(dh, conf);
			} else
				return new Pair<DatabaseHeader, Configuration>(dh, null);
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	public static Pair<DatabaseHeader, Configuration> remoteHeaderConf(
			String user, String host, String file) {
		return remoteHeaderConf(user, host, file, true);
	}

	public static DatabaseHeader remoteHeader(String user, String host,
			String file) {
		return remoteHeaderConf(user, host, file, false).car;
	}

}

class RemoteHandle extends DatabaseHandle {
	BufferedInputStream is;

	public RemoteHandle(int recordGroupByteLength) {
		super(recordGroupByteLength);
	}
}