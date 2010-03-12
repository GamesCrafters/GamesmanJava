package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.parallel.TierMaster;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class DistributedDatabaseClient extends Database {
	private final Scanner scan;
	private final PrintStream masterWrite;
	private long curLoc;
	private Runtime r = Runtime.getRuntime();
	private byte[] dumbArray = new byte[512];
	private String parentPath;
	private ArrayList<ArrayList<Pair<Long, String>>> files;
	private boolean solve;
	private int tier;

	public DistributedDatabaseClient() {
		scan = null;
		masterWrite = null;
	}

	public DistributedDatabaseClient(InputStream masterRead,
			PrintStream masterWrite) {
		scan = new Scanner(masterRead);
		this.masterWrite = masterWrite;
	}

	@Override
	public void close() {
		if (solve) {
			scan.close();
			masterWrite.close();
		}
	}

	@Override
	public void flush() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void getBytes(byte[] arr, int off, int len) {
		try {
			String result;
			if (solve) {
				masterWrite.println("fetch files: " + curLoc + " " + len);
				result = scan.nextLine();
			} else {
				result = TierMaster.getFileList(files.get(tier), curLoc, len);
			}
			String[] nodeFiles = result.split(" ");
			String[] nodeFile = nodeFiles[0].split(":");
			String[] nextNodeFile = null;
			long fileStart = Long.parseLong(nodeFile[1]);
			long nextStart = 0;
			long fileLoc = curLoc - fileStart + 4;
			long fileBlocks = fileLoc >> 9;
			int skipBytes = (int) (fileLoc & 511L);
			for (int i = 0; i < nodeFiles.length; i++) {
				if (i > 0) {
					nodeFile = nextNodeFile;
					fileStart = nextStart;
				}
				if (i < nodeFiles.length - 1) {
					nextNodeFile = nodeFiles[i + 1].split(":");
					nextStart = Long.parseLong(nextNodeFile[1]);
				} else {
					nextStart = curLoc + len;
				}
				String s = "ssh " + nodeFile[0] + " dd if=" + parentPath
						+ File.separator + "s" + nodeFile[1] + ".db";
				if (i == 0 && fileBlocks > 0)
					s += " skip=" + fileBlocks;
				if (i == nodeFiles.length - 1)
					s += " count=" + ((len + skipBytes + 511) >> 9);
				s += "\n";
				Process p = r.exec(s);
				InputStream byteReader = p.getInputStream();
				if (i == 0) {
					while (skipBytes > 0) {
						int bytesRead = byteReader
								.read(dumbArray, 0, skipBytes);
						if (bytesRead == -1)
							Util.fatalError("No more bytes available");
						skipBytes -= bytesRead;
					}
				}
				while (curLoc < nextStart) {
					int bytesRead = byteReader.read(arr, off,
							(int) (nextStart - curLoc));
					if (bytesRead == -1)
						Util.fatalError("No more bytes available");
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
		this.solve = solve;
		if (solve) {
			parentPath = uri + File.separator;
		} else {
			try {
				files = new ArrayList<ArrayList<Pair<Long, String>>>();
				FileInputStream fis = new FileInputStream(uri);
				int fileLength = 0;
				for (int i = 24; i >= 0; i -= 8) {
					fileLength <<= 8;
					fileLength |= fis.read();
				}
				byte[] cBytes = new byte[fileLength];
				fis.read(cBytes);
				if (conf == null) {
					conf = Configuration.load(cBytes);
					conf.db = this;
				}
				Scanner scan = new Scanner(fis);
				scan.nextLine();
				while (scan.hasNext())
					files.add(0, parseArray(scan.nextLine()));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			parentPath = conf.getProperty("gamesman.slaveDbFolder");
		}
	}

	private ArrayList<Pair<Long, String>> parseArray(String nextLine) {
		nextLine = nextLine.substring(1, nextLine.length() - 1);
		String[] els = nextLine.split(", ");
		ArrayList<Pair<Long, String>> result = new ArrayList<Pair<Long, String>>(
				els.length);
		for (int i = 0; i < els.length; i++) {
			els[i] = els[i].substring(1, els[i].length() - 1);
			int split = els[i].indexOf(".");
			Long resLong = new Long(els[i].substring(0, split));
			String resNode = els[i].substring(split + 1);
			result.add(new Pair<Long, String>(resLong, resNode));
		}
		return result;
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(long loc) {
		curLoc = loc;
	}

	public void setTier(int tier) {
		this.tier = tier;
	}
}