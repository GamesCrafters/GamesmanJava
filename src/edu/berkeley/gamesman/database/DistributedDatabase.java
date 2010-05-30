package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.parallel.ErrorThread;
import edu.berkeley.gamesman.util.Pair;

public class DistributedDatabase extends Database {
	private final Scanner nodeNameStream;
	private final PrintStream requestStream;
	private static final Comparator<Pair<String, Long>> nodeListCompare = new Comparator<Pair<String, Long>>() {
		public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
			return o1.cdr.compareTo(o2.cdr);
		}
	};
	private final ArrayList<Pair<String, Long>> fileList;
	private final ArrayList<Pair<String, Long>> lastTier;
	private final ArrayList<Pair<String, Long>> thisTier;
	private final String parentUri;
	private final String user;
	private final String gamesmanPath;
	private final String jobFile;
	private final boolean zipped;
	private final boolean zippedTransfer;
	private final File myFile;
	private int tierNum;

	public DistributedDatabase(InputStream in, PrintStream out,
			String parentUri, Configuration conf) {
		super(null, conf, false, 0, -1);
		nodeNameStream = new Scanner(in);
		requestStream = out;
		fileList = null;
		thisTier = null;
		lastTier = null;
		user = conf.getProperty("gamesman.user", null);
		gamesmanPath = conf.getProperty("gamesman.path");
		String jobFile = conf.getProperty("gamesman.confFile");
		if (!jobFile.startsWith("/"))
			jobFile = gamesmanPath + "/" + jobFile;
		if (!parentUri.startsWith("/"))
			parentUri = gamesmanPath + "/" + parentUri;
		this.parentUri = parentUri;
		this.jobFile = jobFile;
		zipped = conf.getBoolean("gamesman.immediateZip", false);
		zippedTransfer = conf.getBoolean("gamesman.zippedTransfer", false);
		myFile = null;
	}

	public DistributedDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords) {
		super(uri, conf, solve, firstRecord, numRecords);
		nodeNameStream = null;
		requestStream = null;
		myFile = new File(uri);
		TierGame g = (TierGame) conf.getGame();
		gamesmanPath = conf.getProperty("gamesman.path");
		if (myFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(myFile);
				skipHeader(fis);
				String listString = new Scanner(fis).nextLine();
				fis.close();
				fileList = parseList(listString);
			} catch (IOException e) {
				throw new Error(e);
			}
			if (solve) {
				thisTier = new ArrayList<Pair<String, Long>>();
				int lastTierNum = g.hashToTier(fileList.get(0).cdr);
				tierNum = lastTierNum - 1;
				int result = Collections.binarySearch(fileList,
						new Pair<String, Long>(null, g
								.hashOffsetForTier(lastTierNum + 1)),
						nodeListCompare);
				if (result < 0)
					result = -result - 1;
				lastTier = new ArrayList<Pair<String, Long>>();
				lastTier.addAll(fileList.subList(0, result));
			} else {
				thisTier = null;
				lastTier = null;
			}
			String parentUri = conf.getProperty("gamesman.slaveDbFolder");
			if (!parentUri.startsWith("/"))
				parentUri = gamesmanPath + "/" + parentUri;
			this.parentUri = parentUri;
			user = conf.getProperty("gamesman.user", null);
		} else if (solve) {
			fileList = new ArrayList<Pair<String, Long>>();
			thisTier = new ArrayList<Pair<String, Long>>();
			lastTier = new ArrayList<Pair<String, Long>>();
			parentUri = null;
			user = null;
			tierNum = g.numberOfTiers() - 1;
		} else {
			throw new Error("File does not exist: " + uri);
		}
		String jobFile = conf.getProperty("gamesman.confFile");
		if (!jobFile.startsWith("/"))
			jobFile = gamesmanPath + "/" + jobFile;
		this.jobFile = jobFile;
		zipped = conf.getProperty("gamesman.db.compression", "none").equals(
				"gzip");
		zippedTransfer = conf.getBoolean("gamesman.zippedTransfer", false);
	}

	private ArrayList<Pair<String, Long>> parseList(String stringFiles) {
		stringFiles = stripOuter(stringFiles, "[", "]");
		String[] fileSplits = stringFiles
				.substring(1, stringFiles.length() - 1).split(", ");
		ArrayList<Pair<String, Long>> nodeFiles = new ArrayList<Pair<String, Long>>();
		for (int i = 0; i < fileSplits.length; i++) {
			String pairString = stripOuter(fileSplits[i], "(", ")");
			int splitPoint = pairString.lastIndexOf(".");
			nodeFiles.add(new Pair<String, Long>(pairString.substring(0,
					splitPoint), Long.parseLong(pairString
					.substring(splitPoint + 1))));
		}
		return nodeFiles;
	}

	@Override
	protected void closeDatabase() {
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		long firstRecord = toFirstRecord(loc);
		long firstByte = toByte(firstRecord);
		long lastRecord = toLastRecord(loc + len);
		long lastByte = toByte(lastRecord);
		getRecordsAsBytes(dh, firstRecord, arr, off,
				(int) (lastRecord - firstRecord), true,
				(int) (loc - firstByte), (int) (lastByte - (loc + len)));
	}

	// overwriteEdgesOk = true
	// skipFirstBytes = 0, skipLastBytes=0;
	private int getRecordsAsBytes(final DatabaseHandle dh,
			final long firstRecord, final byte[] arr, int off,
			final int numRecords) {
		ArrayList<Pair<String, Long>> nodeFiles;
		if (solve) {
			String stringFiles;
			synchronized (this) {
				requestStream.println("get " + firstRecord + "-" + numRecords);
				stringFiles = nodeNameStream.nextLine();
			}
			nodeFiles = parseList(stringFiles);
		} else
			nodeFiles = getNodes(firstRecord, numRecords);
		long nextStart = firstRecord;
		int remainNum = 0;
		int totalBytes = 0;
		for (int i = 0; i < nodeFiles.size(); i++) {
			long nodeStart = nodeFiles.get(i).cdr;
			long thisStart = nextStart;
			if (i < nodeFiles.size() - 1)
				nextStart = nodeFiles.get(i + 1).cdr;
			else
				nextStart = firstRecord + numRecords;
			try {
				StringBuilder command = new StringBuilder("ssh ");
				if (user != null) {
					command.append(user);
					command.append("@");
				}
				command.append(nodeFiles.get(i).car);
				command.append(" java -cp ");
				command.append(gamesmanPath);
				command
						.append("/bin edu.berkeley.gamesman.database.ReadRecords ");
				command.append(jobFile);
				command.append(" ");
				command.append(parentUri);
				command.append("/s");
				command.append(nodeStart);
				command.append(".db");
				if (zipped)
					command.append(".gz");
				command.append(" ");
				command.append(thisStart);
				command.append(" ");
				command.append(nextStart - thisStart);
				Process p = Runtime.getRuntime().exec(command.toString());
				p.waitFor();
				InputStream is = p.getInputStream();
				if (zippedTransfer)
					is = new GZIPInputStream(is);
				new ErrorThread(p.getErrorStream(), nodeFiles.get(i).car)
						.start();
				if (remainNum > 0) {
					byte[] edgeGroup = dh.getRecordGroupBytes();
					readFully(is, edgeGroup, 0, recordGroupByteLength);
					long newGroup = longRecordGroup(edgeGroup, 0);
					dh.releaseBytes(edgeGroup);
					long oldGroup = longRecordGroup(arr, off
							- recordGroupByteLength);
					long resultGroup = splice(oldGroup, newGroup, remainNum);
					toUnsignedByteArray(resultGroup, arr, off
							- recordGroupByteLength);
					thisStart += recordsPerGroup - remainNum;
				}
				int numBytes = (int) numBytes(thisStart, nextStart - thisStart);
				readFully(is, arr, off, numBytes);
				off += numBytes;
				totalBytes += numBytes;
				is.close();
				remainNum = toNum(nextStart);
			} catch (IOException e) {
				throw new Error(e);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return totalBytes;
	}

	private void getRecordsAsBytes(final DatabaseHandle dh, long firstRecord,
			final byte[] arr, int off, int numRecords,
			final boolean overwriteEdgesOk, int skipFirstBytes,
			int skipLastBytes) {
		byte[] firstGroup = null, lastGroup = null;
		int firstNum = -1, lastNum = -1;
		long lastRecord = firstRecord + numRecords;
		if (!overwriteEdgesOk) {
			firstNum = toNum(firstRecord);
			lastNum = toNum(firstRecord + numRecords);
			if (firstNum > 0 && skipFirstBytes == 0)
				skipFirstBytes = recordGroupByteLength;
			if (lastNum > 0 && skipLastBytes == 0)
				skipLastBytes = recordGroupByteLength;
		}
		if (skipFirstBytes > 0) {
			if (firstNum < 0)
				firstNum = toNum(firstRecord);
			firstGroup = dh.getRecordGroupBytes();
			getRecordsAsBytes(dh, firstRecord, firstGroup, 0, recordsPerGroup
					- firstNum);
			if (!overwriteEdgesOk) {
				long oldGroup = longRecordGroup(arr, off - skipFirstBytes);
				long newGroup = longRecordGroup(firstGroup, 0);
				long resultGroup = splice(oldGroup, newGroup, firstNum);
				toUnsignedByteArray(resultGroup, firstGroup, 0);
			}
			if (skipFirstBytes == recordGroupByteLength) {
				System
						.arraycopy(firstGroup, 0, arr, off,
								recordGroupByteLength);
			} else {
				System.arraycopy(firstGroup, skipFirstBytes, arr, off,
						recordGroupByteLength - skipFirstBytes);
				off -= skipFirstBytes;
			}
			off += recordGroupByteLength;
			numRecords -= recordsPerGroup - firstNum;
			firstRecord += recordsPerGroup - firstNum;
			skipFirstBytes = 0;
			dh.releaseBytes(firstGroup);
		}
		if (skipLastBytes > 0) {
			if (lastNum < 0)
				lastNum = toNum(lastRecord);
			if (lastNum == 0)
				lastNum = recordsPerGroup;
			numRecords -= lastNum;
		}
		int numBytes = getRecordsAsBytes(dh, firstRecord, arr, off, numRecords);
		if (skipLastBytes > 0) {
			firstRecord = lastRecord - lastNum;
			off += numBytes;
			numRecords = lastNum;
			lastGroup = dh.getRecordGroupBytes();
			getRecordsAsBytes(dh, firstRecord, lastGroup, 0, lastNum);
			if (!overwriteEdgesOk) {
				long oldGroup = longRecordGroup(lastGroup, 0);
				long newGroup = longRecordGroup(arr, off);
				long resultGroup = splice(oldGroup, newGroup, lastNum);
				toUnsignedByteArray(resultGroup, lastGroup, 0);
			}
			System.arraycopy(lastGroup, 0, arr, off, skipLastBytes);
			dh.releaseBytes(lastGroup);
		}
	}

	@Override
	protected void getRecordsAsBytes(DatabaseHandle dh, long firstRecord,
			byte[] arr, int off, int numRecords, boolean overwriteEdgesOk) {
		getRecordsAsBytes(dh, firstRecord, arr, off, numRecords,
				overwriteEdgesOk, 0, 0);
	}

	private static final String stripOuter(String string, String open,
			String close) {
		string = string.trim();
		while (string.startsWith(open) && string.endsWith(close)) {
			string = string.substring(open.length(), string.length()
					- close.length());
			string.trim();
		}
		return string;
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new UnsupportedOperationException();
	}

	public synchronized void addFile(String node, long firstRecord) {
		thisTier.add(new Pair<String, Long>(node, firstRecord));
	}

	public void nextTier() {
		Collections.sort(thisTier, nodeListCompare);
		lastTier.clear();
		lastTier.addAll(thisTier);
		fileList.addAll(0, thisTier);
		thisTier.clear();
		tierNum--;
		try {
			FileOutputStream fos = new FileOutputStream(myFile);
			store(fos);
			PrintStream ps = new PrintStream(fos);
			ps.println(fileList.toString());
			ps.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getTier() {
		return tierNum;
	}

	public ArrayList<Pair<String, Long>> getNodes(long firstRecord,
			long numRecords) {
		ArrayList<Pair<String, Long>> searchFrom;
		if (solve)
			searchFrom = lastTier;
		else
			searchFrom = fileList;
		int place = Collections.binarySearch(searchFrom,
				new Pair<String, Long>(null, firstRecord), nodeListCompare);
		if (place < 0)
			place = -place - 2;
		ArrayList<Pair<String, Long>> nodeList = new ArrayList<Pair<String, Long>>();
		long lastRecord = firstRecord + numRecords;
		Pair<String, Long> node = searchFrom.get(place);
		while (node.cdr < lastRecord) {
			nodeList.add(node);
			++place;
			if (place >= nodeList.size())
				break;
			node = searchFrom.get(place);
		}
		return nodeList;
	}

	public static void main(String[] args) throws ClassNotFoundException {
		Configuration conf = new Configuration(args[0]);
		DistributedDatabase db = (DistributedDatabase) Database.openDatabase(
				args[1], conf, true);
		db.addFile(args[2], 0);
		db.nextTier();
		db.close();
	}
}
