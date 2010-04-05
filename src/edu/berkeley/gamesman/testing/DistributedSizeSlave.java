package edu.berkeley.gamesman.testing;

import java.io.File;
import java.io.FileNotFoundException;

public class DistributedSizeSlave {
	public static void main(final String[] args) {
		final String tier = args[0];
		Thread[] checkThreads = new Thread[10];
		final long[] totalLength = new long[10];
		for (int i = 0; i < 10; i++) {
			final int myCount = i;
			totalLength[i] = 0;
			checkThreads[i] = new Thread() {
				public void run() {
					int start = (args.length - 1) * myCount / 10 + 1;
					int end = (args.length - 1) * (myCount + 1) / 10 + 1;
					for (int i = start; i < end; i++) {
						File myFile = new File(
								"/var/folders/zz/zzzivhrRnAmviuee+++UUE++662/database76/t"
										+ tier + "/s" + args[i] + ".db.gz");
						if (myFile.exists()) {
							totalLength[myCount] += myFile.length();
						} else {
							new FileNotFoundException(myFile.getPath())
									.printStackTrace();
						}
					}
				}
			};
			checkThreads[i].start();
		}
		for (int i = 0; i < 10; i++) {
			while (checkThreads[i].isAlive())
				try {
					checkThreads[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		long sum = 0L;
		for (int i = 0; i < 10; i++) {
			sum += totalLength[i];
		}
		System.out.println(sum);
	}
}
