package edu.berkeley.gamesman.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.DistributedDatabase;
import edu.berkeley.gamesman.game.TieredGame;
import edu.berkeley.gamesman.parallel.ErrorThread;
import edu.berkeley.gamesman.util.Pair;

public class DistributedSizeMaster {
	public static void main(String[] args) throws IOException {
		Runtime r = Runtime.getRuntime();
		DistributedDatabase dd = new DistributedDatabase();
		dd.initialize(args[0], false);
		Configuration conf = dd.getConfiguration();
		int tiers = ((TieredGame<?>) conf.getGame()).numberOfTiers();
		LinkedList<Thread> watchers = new LinkedList<Thread>();
		ArrayList<HashMap<String, Long>> values = new ArrayList<HashMap<String, Long>>();
		for (int tier = 0; tier < tiers; tier++) {
			System.out.println("Tier " + tier);
			ArrayList<Pair<Long, String>> fileList = dd.getFiles(tier);
			HashMap<String, StringBuilder> hm = new HashMap<String, StringBuilder>();
			final HashMap<String, Long> myValues = new HashMap<String, Long>();
			values.add(myValues);
			for (Pair<Long, String> p : fileList) {
				StringBuilder sb = hm.get(p.cdr);
				if (sb == null) {
					sb = new StringBuilder(
							"ssh "
									+ p.cdr
									+ " java -cp GamesmanJava/bin edu.berkeley.gamesman.testing.DistributedSizeSlave "
									+ tier);
					hm.put(p.cdr, sb);
					myValues.put(p.cdr, 0L);
				}
				sb.append(" ");
				sb.append(p.car);
			}
			for (final String slaveName : hm.keySet()) {
				final String command = hm.get(slaveName).toString();
				final Process p = r.exec(command);
				new ErrorThread(p.getErrorStream(), slaveName).start();
				watchers.add(new Thread() {
					public void run() {
						Scanner scan = new Scanner(p.getInputStream());
						myValues.put(slaveName, myValues.get(slaveName)
								+ Long.parseLong(scan.nextLine()));
					}
				});
				watchers.getLast().start();
			}
			Iterator<Thread> iter = watchers.iterator();
			while (iter.hasNext()) {
				Thread next = iter.next();
				while (next.isAlive())
					try {
						next.join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
			}
		}
		System.out.println(values);
	}
}
