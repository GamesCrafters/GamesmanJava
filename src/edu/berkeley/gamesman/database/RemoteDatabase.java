package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.parallel.ErrorThread;
import edu.berkeley.gamesman.util.Util;

/**
 * An encapsulating class for reading a database by calling the OS's ssh and
 * then dd for the necessary bytes.<br />
 * WARNING! Does not work on Windows (Run on Cygwin instead).
 * 
 * @author dnspies
 */
public class RemoteDatabase extends Database {
	private Runtime r = Runtime.getRuntime();
	private byte[] dumbArray = new byte[512];
	private String filePath;
	private String host;
	private long offset;
	private long fileSize = -1;

	protected RemoteDatabase() {
	}

	@Override
	public void close() {
	}

	@Override
	public synchronized void getBytes(DatabaseHandle dh, long location,
			byte[] arr, int off, int len) {
		try {
			long fileLoc = location + offset;
			long fileBlocks = fileLoc >> 9;
			int skipBytes = (int) (fileLoc & 511L);
			StringBuilder sb = new StringBuilder("ssh ");
			sb.append(host);
			sb.append(" dd if=");
			sb.append(filePath);
			if (fileBlocks > 0) {
				sb.append(" skip=");
				sb.append(fileBlocks);
			}
			sb.append(" count=");
			sb.append((len + skipBytes + 511) >> 9);
			sb.append("\n");
			Process p = r.exec(sb.toString());
			InputStream byteReader = p.getInputStream();
			new ErrorThread(p.getErrorStream(), host + ":" + filePath).start();
			while (skipBytes > 0) {
				int bytesRead = byteReader.read(dumbArray, 0, skipBytes);
				if (bytesRead == -1)
					Util.fatalError("No more bytes available");
				skipBytes -= bytesRead;
			}
			while (len > 0) {
				int bytesRead = byteReader.read(arr, off, len);
				if (bytesRead == -1)
					Util.fatalError("No more bytes available");
				fileLoc += bytesRead;
				off += bytesRead;
				len -= bytesRead;
			}
			byteReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize(String uri, boolean solve) {
		String[] hostLoc = uri.split(":");
		host = hostLoc[0].trim();
		filePath = hostLoc[1].trim();
		try {
			int confLength;
			String infoGetter = "ssh " + host + " dd if=" + filePath
					+ " count=1";
			Process p = r.exec(infoGetter);
			InputStream is = p.getInputStream();
			new ErrorThread(p.getErrorStream(), host + ":" + filePath).start();
			confLength = 0;
			for (int i = 0; i < 4; i++) {
				confLength <<= 8;
				confLength |= is.read();
			}
			if (conf == null) {
				byte[] confBytes = new byte[confLength];
				int off = 0, len = Math.min(508, confLength);
				while (len > 0) {
					int bytesRead = is.read(confBytes, off, len);
					off += bytesRead;
					len -= bytesRead;
				}
				is.close();
				if (confLength > 508) {
					int moreBlocks = (confLength + 3) >>> 9;
					p = r.exec("ssh " + host + " dd if=" + filePath
							+ " skip=1 count=" + moreBlocks);
					is = p.getInputStream();
					new ErrorThread(p.getErrorStream(), host + ":" + filePath)
							.start();
					len = confLength - 508;
					while (len > 0) {
						int bytesRead = is.read(confBytes, off, len);
						off += bytesRead;
						len -= bytesRead;
					}
				}
				is.close();
				conf = Configuration.load(confBytes);
			}
			offset = confLength + 4;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param loc
	 *            The actual file position to seek to (rather than the byte
	 *            associated with a given record)
	 */
	public void seekInFile(long loc) {
		seek(loc - offset);
	}

	/**
	 * @return The size of the file
	 */
	public long fileSize() {
		if (fileSize < 0) {
			try {
				String infoGetter = "ssh " + host + " ls -l " + filePath;
				Process p;
				p = r.exec(infoGetter);
				Scanner scan = new Scanner(p.getInputStream());
				new ErrorThread(p.getErrorStream(), host + ":" + filePath)
						.start();
				for (int i = 0; i < 4; i++) {
					scan.next();
				}
				fileSize = scan.nextLong();
				scan.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return fileSize;
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}
}
