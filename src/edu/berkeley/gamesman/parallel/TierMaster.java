package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.SplitDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.util.Pair;

public class TierMaster implements Runnable {
	private class TimeOutChecker extends Thread {
		private static final long WAIT_TIME = 600000L;
		private final Thread myThread;
		private final NodeRunnable myRunnable;

		public TimeOutChecker(Thread t, NodeRunnable nr) {
			myThread = t;
			myRunnable = nr;
		}

		@Override
		public synchronized void run() {
			long lastPrinted = System.currentTimeMillis();
			while (myThread.isAlive()) {
				try {
					wait(WAIT_TIME);
					if (myThread.isAlive()) {
						long nextPrinted = myRunnable.lastPrinted;
						if (nextPrinted <= lastPrinted) {
							// TODO: Indicate failure
						} else
							lastPrinted = nextPrinted;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private class NodeRunnable implements Runnable {
		public long lastPrinted;
		private final String name;

		public NodeRunnable(String name) {
			this.name = name;
		}

		public void run() {
			try {
				lastPrinted = System.currentTimeMillis();
				String command = "ssh "
						+ (user == null ? name : (user + "@" + name));
				command += " java -cp " + path
						+ "/bin edu.berkeley.gamesman.parallel.TierSlave ";
				command += slaveJobFile + " " + tier;
				Process p = Runtime.getRuntime().exec(command);
				InputStream es = p.getErrorStream();
				// TODO Start Error-check thread
				OutputStream os = p.getOutputStream();
				InputStream is = p.getInputStream();
				Pair<Long, Long> slice = getSlice();
				while (slice != null) {
					os.write(dbTrack.getHeader(slice.car, slice.cdr).toBytes());
					os.flush();
					PrintStream ps = new PrintStream(os);
					Scanner scan = new Scanner(is);
					String next = scan.next();
					while (!next.equals("finished:")) {
						if (next.equals("fetch:")) {
							long firstRecord = scan.nextLong();
							long numRecords = scan.nextLong();
							ps.println(prevTierDb.makeStream(firstRecord,
									numRecords));
						}
					}
					curTierDb.insertDb(innerDbType, scan.next(), scan
							.nextLong(), scan.nextLong());
					slice = getSlice();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private final Configuration conf;
	private final NodeRunnable[] nodes;
	private final String user;
	private final String path;
	private final String slaveJobFile;
	private final String innerDbType;
	private final SplitDatabase dbTrack;
	private SplitDatabase prevTierDb;
	private SplitDatabase curTierDb;
	private int tier;

	public TierMaster(Configuration conf, String[] nodeNames)
			throws ClassNotFoundException {
		this.conf = conf;
		user = conf.getProperty("gamesman.remote.user");
		path = conf.getProperty("gamesman.remote.path");
		String slaveJobFile = conf.getProperty("gamesman.parallel.slave.job");
		Configuration slaveConf = new Configuration(slaveJobFile);
		if (!(slaveJobFile.startsWith("/") || slaveJobFile.startsWith(path)))
			slaveJobFile = path + "/" + slaveJobFile;
		this.slaveJobFile = slaveJobFile;
		innerDbType = slaveConf.getProperty("gamesman.database");
		dbTrack = new SplitDatabase(conf);
		TierGame g = (TierGame) conf.getGame();
		nodes = new NodeRunnable[nodeNames.length];
		for (int i = 0; i < nodeNames.length; i++) {
			nodes[i] = new NodeRunnable(nodeNames[i]);
		}
	}

	public Pair<Long, Long> getSlice() {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) throws ClassNotFoundException,
			FileNotFoundException {
		String jobFile = args[0];
		String nodeFile = args[1];
		Configuration conf = new Configuration(jobFile);
		ArrayList<String> nodes = new ArrayList<String>();
		Scanner scan = new Scanner(new File(nodeFile));
		while (scan.hasNext())
			nodes.add(scan.next());
		scan.close();
		TierMaster tm = new TierMaster(conf, nodes.toArray(new String[nodes
				.size()]));
		tm.run();
	}

	public void run() {
		TierGame g = (TierGame) conf.getGame();
		Thread[] joinThreads = new Thread[nodes.length];
		for (tier = g.numberOfTiers() - 1; tier >= 0; tier--) {
			curTierDb = new SplitDatabase(conf, g.hashOffsetForTier(tier), g
					.numHashesForTier(tier));
			int i = 0;
			for (NodeRunnable nr : nodes) {
				Thread t = new Thread(nr);
				joinThreads[i++] = t;
				TimeOutChecker toc = new TimeOutChecker(t, nr);
				t.start();
				toc.start();
			}
			for (Thread t : joinThreads) {
				while (t.isAlive())
					try {
						t.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
			dbTrack.addDatabasesFirst(curTierDb);
			prevTierDb = curTierDb;
		}
	}
}
