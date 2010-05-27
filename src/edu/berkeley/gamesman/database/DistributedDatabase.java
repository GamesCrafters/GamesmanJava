package edu.berkeley.gamesman.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import edu.berkeley.gamesman.core.Configuration;
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
	}

	public DistributedDatabase(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords) {
		super(uri, conf, solve, firstRecord, numRecords);
		nodeNameStream = null;
		requestStream = null;
		if (solve) {
			fileList = new ArrayList<Pair<String, Long>>();
			thisTier = new ArrayList<Pair<String, Long>>();
			lastTier = new ArrayList<Pair<String, Long>>();
			parentUri = null;
			user = null;
		} else {
			try {
				FileInputStream fis = new FileInputStream(uri);
				skipHeader(fis);
				String listString = new Scanner(fis).nextLine();
				fis.close();
				fileList = parseList(listString);
			} catch (IOException e) {
				throw new Error(e);
			}
			thisTier = null;
			lastTier = null;
			parentUri = conf.getProperty("gamesman.slaveDbFolder");
			user = conf.getProperty("gamesman.user", null);
		}
		gamesmanPath = conf.getProperty("gamesman.path");
		jobFile = conf.getProperty("gamesman.confFile");
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
		getRecordsAsBytes(dh, firstRecord, arr, off, (int) (toLastRecord(loc
				+ len) - firstRecord), true);
	}

	@Override
	protected void getRecordsAsBytes(DatabaseHandle dh, long firstRecord,
			byte[] arr, int off, int numRecords, boolean overwriteEdgesOk) {
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
		for (int i = 0; i < nodeFiles.size(); i++) {
			long nodeStart = nodeFiles.get(i).cdr;
			long thisStart = nextStart;
			if (i < nodeFiles.size() - 1)
				nextStart = nodeFiles.get(i + 1).cdr;
			else
				nextStart = firstRecord + numRecords;
			try {
				StringBuilder command = new StringBuilder("ssh ");
				if (user != null)
					command.append(user + "@");
				command.append(nodeFiles.get(i).car);
				command.append(" java -cp ");
				command.append(gamesmanPath + "/bin ");
				command.append("edu.berkeley.gamesman.parallel.ReadRecords ");
				command.append(jobFile + " ");
				command.append(parentUri + "/s" + nodeStart + ".db");
				if (zipped)
					command.append(".gz");
				command.append(" " + thisStart + " " + nextStart);
				Process p = Runtime.getRuntime().exec(command.toString());
				InputStream is = p.getInputStream();
				if (zippedTransfer)
					is = new GZIPInputStream(is);
				new ErrorThread(p.getErrorStream(), nodeFiles.get(i).car)
						.start();
				int bytesToRead = (int) numBytes(thisStart, nextStart);
				while (bytesToRead > 0) {
					int bytesRead = is.read(arr, off, bytesToRead);
					bytesToRead -= bytesRead;
					off += bytesRead;
				}
				is.close();
			} catch (IOException e) {
				throw new Error(e);
			}
		}
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
		fileList.addAll(thisTier);
		thisTier.clear();
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
}
