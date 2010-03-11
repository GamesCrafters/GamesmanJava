package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.hasher.TieredHasher;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class TierMaster {
	private static final float MULTIPLE = (float) 1.8;
	private static final String FETCH_LINE = "fetch files: ";
	private static final String END_LINE = "finished with files: ";
	private static final Comparator<Pair<Long, String>> PAIR_COMPARE = new Comparator<Pair<Long, String>>() {

		public int compare(Pair<Long, String> o1, Pair<Long, String> o2) {
			return o1.car.compareTo(o2.car);
		}

	};
	private long[] splits;
	private Configuration conf;
	private int tier;
	private int curSplit = 0;
	private final Runtime r = Runtime.getRuntime();
	private final String jobFile;
	private ArrayList<Pair<Long, String>> tierFileList;
	private ArrayList<Pair<Long, String>> lastFileList;

	private class NodeWatcher implements Runnable {
		private final String slaveName;

		public NodeWatcher(String slaveName) {
			this.slaveName = slaveName;
		}

		public void run() {
			while (curSplit < splits.length - 1) {
				try {
					int mySplit = nextSplit();
					if (mySplit >= splits.length - 1)
						break;
					long memory = conf.getLong("gamesman.memory", 0);
					String command = "ssh "
							+ slaveName
							+ " java"
							+ (memory > 0 ? " -Xmx" + (memory + 100000000) : "")
							+ " -cp GamesmanJava/bin edu.berkeley.gamesman.parallel.TierSlave "
							+ "GamesmanJava/" + jobFile + " " + tier + " "
							+ splits[mySplit] + " "
							+ (splits[mySplit + 1] - splits[mySplit]);
					final Process p = r.exec(command);
					new Thread() {
						@Override
						public void run() {
							Scanner errScan = new Scanner(p.getErrorStream());
							while (errScan.hasNext()) {
								System.err.println(errScan.nextLine());
							}
							errScan.close();
						}
					}.start();
					Scanner scan = new Scanner(p.getInputStream());
					PrintStream ps = new PrintStream(p.getOutputStream());
					String readIn;
					while (scan.hasNext()) {
						readIn = scan.nextLine();
						if (readIn.startsWith(FETCH_LINE)) {
							String[] needs = readIn.substring(
									FETCH_LINE.length()).split(" ");
							String response = getFileList(Long
									.parseLong(needs[0]), Integer
									.parseInt(needs[1]));
							ps.println(response);
							ps.flush();
						} else if (readIn.startsWith(END_LINE)) {
							addFiles(slaveName, readIn.substring(
									END_LINE.length()).split(" "));
						} else
							System.out.println(readIn);
						readIn = "";

					}
					scan.close();
					ps.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String getFileList(long byteNum, int len) {
		int low = 0, high = lastFileList.size();
		int guess = (low + high) / 2;
		while (high - low > 1) {
			if (lastFileList.get(guess).car < byteNum) {
				low = guess;
			} else if (lastFileList.get(guess).car > byteNum) {
				high = guess;
			} else {
				low = guess;
				break;
			}
			guess = (low + high) / 2;
		}
		guess = low;
		long end = byteNum + len;
		Pair<Long, String> p = lastFileList.get(guess);
		String s = p.cdr + ":" + p.car;
		for (guess++; guess < lastFileList.size()
				&& (p = lastFileList.get(guess)).car < end; guess++)
			s += " " + p.cdr + ":" + p.car;
		return s;
	}

	private void addFiles(String slaveName, String[] fileStarts) {
		for (int i = 0; i < fileStarts.length; i++)
			tierFileList.add(new Pair<Long, String>(Long
					.parseLong(fileStarts[i]), slaveName));
	}

	public synchronized int nextSplit() {
		return curSplit++;
	}

	private final NodeWatcher[] watchers;

	public TierMaster(String jobFile, String slavesFile)
			throws FileNotFoundException, ClassNotFoundException {
		this.jobFile = jobFile;
		File slavesList = new File(slavesFile);
		Scanner scan = new Scanner(slavesList);
		ArrayList<String> slaveNames = new ArrayList<String>();
		while (scan.hasNext())
			slaveNames.add(scan.nextLine().trim());
		scan.close();
		watchers = new NodeWatcher[slaveNames.size()];
		for (int i = 0; i < slaveNames.size(); i++) {
			watchers[i] = new NodeWatcher(slaveNames.get(i));
		}
		conf = new Configuration(Configuration.readProperties(jobFile));
	}

	public void solve() {
		TieredGame<? extends State> game = (TieredGame<? extends State>) conf
				.getGame();
		int numTiers = game.numberOfTiers();
		tierFileList = new ArrayList<Pair<Long, String>>();
		for (tier = numTiers - 1; tier >= 0; tier--) {
			long tierOffset = game.hashOffsetForTier(tier);
			long tierLength = ((TieredHasher<? extends State>) conf.getHasher())
					.numHashesForTier(tier);
			splits = Util.groupAlignedTasks((int) (watchers.length * MULTIPLE),
					tierOffset, tierLength, conf.recordsPerGroup);
			curSplit = 0;
			Thread[] myThreads = new Thread[watchers.length];
			for (int i = 0; i < watchers.length; i++) {
				myThreads[i] = new Thread(watchers[i]);
				myThreads[i].start();
			}
			for (int i = 0; i < watchers.length; i++) {
				try {
					myThreads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Collections.sort(tierFileList, PAIR_COMPARE);
			lastFileList = tierFileList;
			tierFileList = new ArrayList<Pair<Long, String>>();
		}
	}

	public static void main(String[] args) throws FileNotFoundException,
			ClassNotFoundException {
		TierMaster tm = new TierMaster(args[0], args[1]);
		tm.solve();
	}

	// This is a copy of Util.parseBoolean().
	// It needs to be here to avoid loading the Util class before we're ready to
	private static boolean parseBoolean(String s) {
		return s != null && !s.equalsIgnoreCase("false")
				&& !s.equalsIgnoreCase("0");
	}
}
