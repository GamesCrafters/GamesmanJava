package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.parallel.ErrorThread;
import edu.berkeley.gamesman.parallel.TierSlave;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A class for reading records (via ssh/dd) from a bunch of different files
 * distributed across multiple machines.<br />
 * WARNING! Does not work on Windows (Run on Cygwin)
 * 
 * @author dnspies
 */
public class DistributedDatabase extends Database {
	private final Scanner scan;
	private final PrintStream masterWrite;
	private Runtime r = Runtime.getRuntime();
	private byte[] dumbArray = new byte[512];
	private String parentPath;
	private ArrayList<ArrayList<Pair<Long, String>>> files;
	private int tier;
	private int lastZippedTier = -1;
	private boolean zipped = false;

	/**
	 * Default constructor (For read-only configuration)
	 */
	public DistributedDatabase() {
		scan = null;
		masterWrite = null;
	}

	/**
	 * Solving constructor
	 * 
	 * @param masterRead
	 *            The input stream which tells hosts and starting values
	 * @param masterWrite
	 *            The output stream to send name requests
	 */
	public DistributedDatabase(InputStream masterRead, PrintStream masterWrite) {
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
	public void getBytes(DatabaseHandle dh, long location, byte[] arr, int off,
			int len) {
		fetchBytes(location, arr, off, len);
	}

	private long fetchBytes(long location, byte[] arr, int off, int len) {
		int firstTier, lastTier;
		long nextTierOffset = 0;
		TierGame g = null;
		if (solve) {
			firstTier = lastTier = tier;
		} else {
			g = (TierGame) conf.getGame();
			for (int count = 0; count < recordGroupByteLength; count++) {
				arr[off + count] = 0;
			}
			firstTier = g.hashToTier(location / recordGroupByteLength
					* recordsPerGroup);
			lastTier = g.hashToTier((location + len)
					/ recordGroupByteLength * recordsPerGroup - 1);
			nextTierOffset = g.hashOffsetForTier(firstTier);
		}
		for (int tier = firstTier; tier <= lastTier; tier++) {
			boolean combineFirst;
			try {
				String result;
				if (solve) {
					combineFirst = false;
					synchronized (this) {
						masterWrite.println("fetch files: " + location + " "
								+ len);
						result = scan.nextLine();
					}
				} else {
					combineFirst = nextTierOffset / recordsPerGroup
							* recordGroupByteLength == location;
					nextTierOffset = g.hashOffsetForTier(tier + 1);
					result = getFileList(files.get(tier), location, len);
				}
				String[] nodeFiles = result.split(" ");
				String[] nodeFile = nodeFiles[0].split(":");
				String[] nextNodeFile = null;
				long fileStart = Long.parseLong(nodeFile[1]);
				long nextStart = 0;
				long fileLoc = location - fileStart + 4;
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
					} else if (tier == lastTier) {
						nextStart = location + len;
					} else {
						int nextTierMod = (int) (nextTierOffset % recordsPerGroup);
						nextStart = (nextTierOffset / recordsPerGroup + (nextTierMod == 0 ? 0
								: 1))
								* recordGroupByteLength;
						if (nextTierMod == 0)
							for (int byteCount = 0; byteCount < recordGroupByteLength; byteCount++)
								arr[(int) (nextStart - location) + off
										+ byteCount] = 0;
					}
					if (lastZippedTier >= 0 && tier >= lastZippedTier
							|| (zipped && TierSlave.jobFile != null)) {
						String gamesmanPath = conf.getProperty("gamesman.path");
						StringBuilder sb = new StringBuilder("ssh ");
						sb.append(nodeFile[0]);
						sb.append(" java -cp ");
						sb.append(gamesmanPath);
						sb.append(File.separator);
						sb
								.append("bin edu.berkeley.gamesman.parallel.ReadZippedBytes ");
						sb.append(TierSlave.jobFile);
						sb.append(" ");
						sb.append(tier);
						sb.append(" ");
						sb.append(nodeFile[1]);
						sb.append(" ");
						sb.append(location);
						sb.append(" ");
						sb.append(nextStart - location);
						Process p = r.exec(sb.toString());
						InputStream byteReader = p.getInputStream();
						new ErrorThread(p.getErrorStream(), nodeFile[0] + ":"
								+ nodeFile[1]).start();
						while (location < nextStart) {
							byte[] group = null;
							if (combineFirst) {
								group = new byte[recordGroupByteLength];
								for (int count = 0; count < recordGroupByteLength; count++)
									group[count] = arr[off + count];
							}
							int bytesRead = byteReader.read(arr, off,
									(int) (nextStart - location));
							if (combineFirst) {
								for (int count = 0; count < recordGroupByteLength; count++)
									arr[off + count] += group[count];
							}
							if (bytesRead == -1)
								Util.fatalError(nodeFile[0] + ":" + nodeFile[1]
										+ ": No more bytes available");
							location += bytesRead;
							off += bytesRead;
							len -= bytesRead;
						}
					} else if (zipped) {
						GZippedFileDatabase myZipBase = new GZippedFileDatabase();
						DatabaseHandle dh = myZipBase.getHandle();
						myZipBase.initialize(nodeFile[0] + ":" + parentPath
								+ "t" + tier + File.separator + "s"
								+ nodeFile[1] + ".db.gz", conf, false);
						myZipBase.getBytes(dh, location - fileStart, arr, off,
								(int) (nextStart - location));
						off += nextStart - location;
						len -= nextStart - location;
						location = nextStart;
					} else {
						StringBuilder sb = new StringBuilder("ssh ");
						sb.append(nodeFile[0]);
						sb.append(" dd if=");
						sb.append(parentPath);
						sb.append("t");
						sb.append(tier);
						sb.append(File.separator);
						sb.append("s");
						sb.append(nodeFile[1]);
						sb.append(".db");
						if (i == 0 && fileBlocks > 0) {
							sb.append(" skip=");
							sb.append(fileBlocks);
						}
						if (tier == lastTier && i == nodeFiles.length - 1) {
							sb.append(" count=");
							sb.append((len + skipBytes + 511) >> 9);
						}
						sb.append("\n");
						Process p = r.exec(sb.toString());
						InputStream byteReader = p.getInputStream();
						while (skipBytes > 0) {
							int bytesRead = byteReader.read(dumbArray, 0,
									skipBytes);
							if (bytesRead == -1)
								Util.fatalError(nodeFile[0] + ":" + nodeFile[1]
										+ ": No more bytes available");
							skipBytes -= bytesRead;
						}
						skipBytes = 4;
						while (location < nextStart) {
							int bytesRead = byteReader.read(arr, off,
									(int) (nextStart - location));
							if (bytesRead == -1)
								Util.fatalError(nodeFile[0] + ":" + nodeFile[1]
										+ ": No more bytes available");
							location += bytesRead;
							off += bytesRead;
							len -= bytesRead;
						}
						byteReader.close();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (tier != lastTier) {
				if (!solve && nextTierOffset % recordsPerGroup != 0) {
					location -= recordGroupByteLength;
					off -= recordGroupByteLength;
					len += recordGroupByteLength;
				} else
					arr[off] = 0;
			}
		}
		return location;
	}

	@Override
	public long getRecord(DatabaseHandle dh, long recordIndex) {
		return super.getRecord(dh, recordIndex);
	}

	@Override
	public void initialize(String uri, boolean solve) {
		if (solve) {
			parentPath = uri + File.separator;
			lastZippedTier = conf.getInteger("gamesman.db.lastZippedTier", -1);
		} else {
			try {
				files = new ArrayList<ArrayList<Pair<Long, String>>>();
				FileInputStream fis = new FileInputStream(uri);
				conf = Configuration.load(fis);
				Scanner scan = new Scanner(fis);
				scan.nextLine();
				while (scan.hasNext())
					files.add(0, parseArray(scan.nextLine()));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			parentPath = conf.getProperty("gamesman.slaveDbFolder")
					+ File.separator;
			zipped = conf.getProperty("gamesman.db.compression", "none")
					.equals("gzip");
			String gamesmanPath = conf.getProperty("gamesman.path");
			TierSlave.jobFile = gamesmanPath + File.separator
					+ conf.getProperty("gamesman.confFile", null);
		}
	}

	/**
	 * @param line
	 *            The toString print-out of this ArrayList
	 * @return The ArrayList
	 */
	public static ArrayList<Pair<Long, String>> parseArray(String line) {
		line = line.substring(1, line.length() - 1);
		String[] els = line.split(", ");
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

	/**
	 * Sets the tier for read-only mode. This avoids group conflicts at tier
	 * edges. When in read-only mode, make sure call this before calling
	 * getBytes
	 * 
	 * @param tier
	 *            The tier
	 */
	public void setTier(int tier) {
		this.tier = tier;
	}

	/**
	 * @param tierFiles
	 *            A list of pairs of the byte of the first position in a file
	 *            followed by the host it's on for this entire tier
	 * @param byteNum
	 *            The byte to start from
	 * @param len
	 *            The total number of bytes
	 * @return A string with a list of only the necessary files for solving the
	 *         given range (separated by spaces. host/starting position
	 *         separated by ':')
	 */
	public static String getFileList(ArrayList<Pair<Long, String>> tierFiles,
			long byteNum, int len) {
		int low = 0, high = tierFiles.size();
		int guess = (low + high) / 2;
		while (high - low > 1) {
			if (tierFiles.get(guess).car < byteNum) {
				low = guess;
			} else if (tierFiles.get(guess).car > byteNum) {
				high = guess;
			} else {
				low = guess;
				break;
			}
			guess = (low + high) / 2;
		}
		guess = low;
		long end = byteNum + len;
		Pair<Long, String> p = tierFiles.get(guess);
		String s = p.cdr + ":" + p.car;
		for (guess++; guess < tierFiles.size()
				&& (p = tierFiles.get(guess)).car < end; guess++)
			s += " " + p.cdr + ":" + p.car;
		return s;
	}

	/**
	 * Just for testing
	 * 
	 * @param i
	 *            the index into files
	 * @return files.get(i);
	 */
	public ArrayList<Pair<Long, String>> getFiles(int i) {
		return files.get(i);
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}
}
