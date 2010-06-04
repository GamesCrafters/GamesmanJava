package edu.berkeley.gamesman.database;

import java.io.BufferedInputStream;
import java.io.IOError;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.parallel.ErrorThread;

public class RemoteDatabase extends Database {
	private final String user, server, confFile, gamesmanPath, remoteFile;
	private static final Runtime runtime = Runtime.getRuntime();
	private int maxCommandLen = -1;

	public RemoteDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords) {
		this(uri, conf, solve, firstRecord, numRecords, conf
				.getProperty("gamesman.remote.server"), conf
				.getProperty("gamesman.remote.db.uri"));
	}

	public RemoteDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords, String server, String remoteFile) {
		super(uri, conf, solve, firstRecord, numRecords);
		user = conf.getProperty("gamesman.remote.user", null);
		this.server = server;
		gamesmanPath = conf.getProperty("gamesman.remote.path");
		String confFile = conf.getProperty("gamesman.remote.confFile", null);
		if (confFile != null && !confFile.startsWith("/"))
			confFile = gamesmanPath + "/" + confFile;
		this.confFile = confFile;
		if (!remoteFile.startsWith("/"))
			remoteFile = gamesmanPath + "/" + remoteFile;
		this.remoteFile = remoteFile;
	}

	@Override
	protected void closeDatabase() {
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
		command.append(byteIndex);
		command.append(" ");
		command.append(firstNum);
		command.append(" ");
		command.append(numBytes);
		command.append(" ");
		command.append(lastNum);
		if (maxCommandLen < command.length())
			maxCommandLen = command.length();
		try {
			Process p = runtime.exec(command.toString());
			new ErrorThread(p.getErrorStream(), server).start();
			((RemoteHandle) dh).is = new BufferedInputStream(
					p.getInputStream(), ReadRecords.BUFFER_SIZE);
		} catch (IOException e) {
			throw new IOError(e);
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
				throw new IOError(e);
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

}

class RemoteHandle extends DatabaseHandle {
	BufferedInputStream is;

	public RemoteHandle(int recordGroupByteLength) {
		super(recordGroupByteLength);
	}
}