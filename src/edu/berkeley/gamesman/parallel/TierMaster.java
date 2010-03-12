package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.database.DistributedDatabase;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.hasher.TieredHasher;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * The main class to run on the master node for parallel solves.<br />
 * WARNING! Does not work on Windows (Run on Cygwin).
 * 
 * @author dnspies
 */
public class TierMaster {
	private static final long TIMEOUT = 10000;
	private static final float MULTIPLE = (float) 1.8;
	private static final String FETCH_LINE = "fetch files: ";
	private static final String END_LINE = "finished with files: ";
	private static final Comparator<Pair<Long, String>> PAIR_COMPARE = new Comparator<Pair<Long, String>>() {

		public int compare(Pair<Long, String> o1, Pair<Long, String> o2) {
			return o1.car.compareTo(o2.car);
		}

	};
	private long[] splits;
	private LinkedList<Integer> remainingTasks = new LinkedList<Integer>();
	private LinkedList<Integer> solving = new LinkedList<Integer>();
	private Configuration conf;
	private int tier;
	private final Runtime r = Runtime.getRuntime();
	private final String jobFile;
	private ArrayList<Pair<Long, String>> tierFileList;
	private ArrayList<Pair<Long, String>> lastFileList;
	private CountDownLatch cdl;
	private final Object lock = new Object();
	private final File dbFile;
	private final PrintStream dbWriter;

	private class NodeWatcher implements Runnable {
		private final String slaveName;
		private boolean failed;
		public long lastMessage;
		public Process myProcess;
		public CountDownLatch myLatch = null;

		// Should be the same as CDL when waiting but funky parallelization
		// stuff could cause it to be different (in which case it will have
		// already been tripped and will only be set momentarily)

		public NodeWatcher(String slaveName) {
			this.slaveName = slaveName;
		}

		public synchronized void run() {
			int mySplit = 0;
			while (true) {
				try {
					boolean breakNow = false;
					synchronized (lock) {
						mySplit = nextSplit();
						if (mySplit < 0)
							if (solving.size() == 0)
								breakNow = true;
							else
								myLatch = cdl;
						else
							solving.add(mySplit);
					}
					if (breakNow) {
						cdl.countDown();
						break;
					} else if (myLatch != null) {
						myLatch.await();
						lastMessage = System.currentTimeMillis();
						// Must be called before the next statement so the
						// checker thread doesn't think I've been held up
						// blocking for input
						myLatch = null;
						continue;
					}
					lastMessage = System.currentTimeMillis();
					failed = false;
					long memory = conf.getLong("gamesman.memory", 0);
					String command = "ssh "
							+ slaveName
							+ " java"
							+ (memory > 0 ? " -Xmx" + (memory + 100000000) : "")
							+ " -cp GamesmanJava/bin edu.berkeley.gamesman.parallel.TierSlave "
							+ "GamesmanJava/" + jobFile + " " + tier + " "
							+ splits[mySplit] + " "
							+ (splits[mySplit + 1] - splits[mySplit]);
					myProcess = r.exec(command);
					new Thread() {

						@Override
						public void run() {
							failed = false;
							Scanner errScan = new Scanner(myProcess
									.getErrorStream());
							while (errScan.hasNext()) {
								System.err.println(slaveName + ": "
										+ errScan.nextLine());
								failed = true;
							}
							errScan.close();
						}
					}.start();
					Scanner scan = new Scanner(myProcess.getInputStream());
					PrintStream ps = new PrintStream(myProcess
							.getOutputStream());
					String readIn;
					while (scan.hasNext()) {
						readIn = scan.nextLine();
						if (failed)
							break;
						if (readIn.startsWith(FETCH_LINE)) {
							String[] needs = readIn.substring(
									FETCH_LINE.length()).split(" ");
							String response = DistributedDatabase.getFileList(
									lastFileList, Long.parseLong(needs[0]),
									Integer.parseInt(needs[1]));
							ps.println(response);
							ps.flush();
						} else if (readIn.startsWith(END_LINE)) {
							if (!failed) {
								addFiles(slaveName, readIn.substring(
										END_LINE.length()).split(" "));
								solving.remove(new Integer(mySplit));
							}
						} else
							System.out.println(slaveName + ": " + readIn);
						lastMessage = System.currentTimeMillis();
						readIn = "";
					}
					if (failed || solving.contains(mySplit)) {
						System.err.println(slaveName + " failed to complete");
						addBack(mySplit);
						wait(TIMEOUT);
						// A ten-second time-out for bad behavior ensures that
						// the poorest performers aren't given preference. It's
						// surprising how much it happens without this statement
					}
					scan.close();
					ps.close();
				} catch (IOException e) {
					System.err.println(slaveName + " failed to complete");
					e.printStackTrace();
					addBack(mySplit);
					try {
						wait(TIMEOUT);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class WatchChecker implements Runnable {
		private NodeWatcher myWatcher;
		private Thread myThread;
		private final static long INTERVAL = 300000;

		public void initialize(NodeWatcher n, Thread t) {
			myWatcher = n;
			myThread = t;
		}

		public synchronized void run() {
			while (myThread.isAlive()) {
				try {
					wait(INTERVAL);
					if (System.currentTimeMillis() - myWatcher.lastMessage > INTERVAL) {
						if (myWatcher.myLatch != null) {
							CountDownLatch waitingLatch = null;
							synchronized (lock) {
								if (myWatcher.myLatch != null)
									waitingLatch = myWatcher.myLatch;
							}
							if (waitingLatch != null)
								waitingLatch.await();
						} else {
							if (myThread.isAlive()) {
								System.err.println("Killing process on "
										+ myWatcher.slaveName);
								myWatcher.failed = true;
								myWatcher.myProcess.destroy();
							}
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void addBack(int mySplit) {
		synchronized (lock) {
			solving.remove(new Integer(mySplit));
			remainingTasks.addFirst(mySplit);
			if (remainingTasks.size() == 1) {
				CountDownLatch oldDown = cdl;
				cdl = new CountDownLatch(1);
				oldDown.countDown();
			}
		}
	}

	private synchronized void addFiles(String slaveName, String[] fileStarts) {
		for (int i = 0; i < fileStarts.length; i++)
			tierFileList.add(new Pair<Long, String>(Long
					.parseLong(fileStarts[i]), slaveName));
	}

	private synchronized int nextSplit() {
		if (remainingTasks.size() == 0)
			return -1;
		else
			return remainingTasks.removeFirst();
	}

	private final NodeWatcher[] watchers;
	private final WatchChecker[] checkers;
	private final Thread[] nodeThreads;

	/**
	 * @param jobFile
	 *            The location of the job file within GamesmanJava (eg
	 *            jobs/myPSolve.job)
	 * @param slavesFile
	 *            The file containing the addresses of the slaves
	 * @throws ClassNotFoundException
	 *             If the configuration throws a ClassNotFoundException when
	 *             instantiating
	 * @throws IOException
	 *             Too many ways to count
	 */
	public TierMaster(String jobFile, String slavesFile)
			throws ClassNotFoundException, IOException {
		this.jobFile = jobFile;
		File slavesList = new File(slavesFile);
		Scanner scan = new Scanner(slavesList);
		ArrayList<String> slaveNames = new ArrayList<String>();
		while (scan.hasNext())
			slaveNames.add(scan.nextLine().trim());
		scan.close();
		watchers = new NodeWatcher[slaveNames.size()];
		checkers = new WatchChecker[slaveNames.size()];
		nodeThreads = new Thread[slaveNames.size()];
		for (int i = 0; i < slaveNames.size(); i++) {
			watchers[i] = new NodeWatcher(slaveNames.get(i));
			checkers[i] = new WatchChecker();
		}
		conf = new Configuration(Configuration.readProperties(jobFile));
		dbFile = new File(conf.getProperty("gamesman.db.uri"));
		dbFile.createNewFile();
		dbWriter = new PrintStream(dbFile);
		byte[] confInfo = conf.store();
		int confLength = confInfo.length;
		for (int i = 24; i >= 0; i -= 8) {
			dbWriter.write(confLength >>> i);
		}
		dbWriter.write(confInfo);
		dbWriter.println();
	}

	/**
	 * This method is completely useless... Unless you're interested in solving
	 * a tiered game across multiple machines.
	 */
	public void solve() {
		TieredGame<? extends State> game = (TieredGame<? extends State>) conf
				.getGame();
		int numTiers = game.numberOfTiers();
		tierFileList = new ArrayList<Pair<Long, String>>();
		long startTime = System.currentTimeMillis();
		for (tier = numTiers - 1; tier >= 0; tier--) {
			long tierOffset = game.hashOffsetForTier(tier);
			long tierLength = ((TieredHasher<? extends State>) conf.getHasher())
					.numHashesForTier(tier);
			splits = Util.groupAlignedTasks((int) (watchers.length * MULTIPLE),
					tierOffset, tierLength, conf.recordsPerGroup);
			for (int i = 0; i < splits.length - 1; i++)
				remainingTasks.add(i);
			cdl = new CountDownLatch(1);
			for (int i = 0; i < watchers.length; i++) {
				nodeThreads[i] = new Thread(watchers[i]);
				checkers[i].initialize(watchers[i], nodeThreads[i]);
				Thread checkThread = new Thread(checkers[i]);
				checkThread.setDaemon(true);
				nodeThreads[i].start();
				checkThread.start();
			}
			for (int i = 0; i < watchers.length; i++) {
				try {
					nodeThreads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Collections.sort(tierFileList, PAIR_COMPARE);
			dbWriter.println(tierFileList);
			lastFileList = tierFileList;
			tierFileList = new ArrayList<Pair<Long, String>>();
		}
		dbWriter.close();
		long totalTime = System.currentTimeMillis() - startTime;
		System.out.println("Took " + Util.millisToETA(totalTime) + " to solve");
	}

	/**
	 * @param args
	 *            The job file and the slavesList file
	 * @throws ClassNotFoundException
	 *             If the configuration throws a ClassNotFoundException when
	 *             instantiating
	 * @throws IOException
	 *             Too many ways to count
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		TierMaster tm = new TierMaster(args[0], args[1]);
		tm.solve();
	}
}
