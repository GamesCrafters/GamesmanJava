package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Database;

public class DistributedDatabaseClient extends Database {
	private final Scanner scan;
	private final PrintStream masterWrite;
	private long curLoc;
	private Runtime r = Runtime.getRuntime();
	private byte[] dumbArray = new byte[512];
	private long nextNodeStart;
	private String parentPath;

	public DistributedDatabaseClient(InputStream masterRead,
			PrintStream masterWrite) {
		scan = new Scanner(masterRead);
		this.masterWrite = masterWrite;
	}

	@Override
	public void close() {
		scan.close();
		masterWrite.close();
	}

	@Override
	public void flush() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void getBytes(byte[] arr, int off, int len) {
		try {
			masterWrite.println("fetch files: " + curLoc + " " + len);
			String result = scan.nextLine();
			String[] nodeFiles = result.split(" ");
			String[] nodeFile = nodeFiles[0].split(":");
			String[] nextNodeFile = null;
			long fileStart = Long.parseLong(nodeFile[1]);
			long nextStart = 0;
			long fileLoc = curLoc - fileStart;
			long fileBlocks = fileLoc >> 9;
			int skipBytes = (int) (fileLoc & 511L);
			for (int i = 0; i < nodeFiles.length; i++) {
				if (i > 0) {
					nodeFile = nextNodeFile;
					fileStart = nextStart;
				}
				if (i < nodeFiles.length - 1) {
					nextNodeFile = nodeFiles[i + 1].split(":");
					nextStart = Long.parseLong(nextNodeFile[1].substring(1,
							nextNodeFile[1].length() - 3));
				} else {
					nextStart = curLoc + len;
				}
				String s = "ssh " + nodeFile[0] + " dd if=" + parentPath + "s"
						+ nodeFile[1] + ".db";
				if (i == 0 && fileBlocks > 0)
					s += " skip=" + fileBlocks;
				if (i == nodeFiles.length - 1)
					s += " count=" + ((len + skipBytes + 511) >> 9);
				s += "\n";
				Process p = r.exec(s);
				InputStream byteReader = p.getInputStream();
				if (i == 0) {
					while (skipBytes > 0)
						skipBytes -= byteReader.read(dumbArray, 0, skipBytes);
				}
				while (curLoc < nextStart) {
					int bytesRead = byteReader.read(arr, off,
							(int) (nextStart - curLoc));
					curLoc += bytesRead;
					off += bytesRead;
					len -= bytesRead;
				}
				byteReader.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize(String uri, boolean solve) {
		parentPath = uri + File.separator;
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(long loc) {
		curLoc = loc;
	}
}
