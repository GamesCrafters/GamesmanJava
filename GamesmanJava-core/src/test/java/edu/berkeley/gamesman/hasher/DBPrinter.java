package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class DBPrinter {
	public static void main(String[] args) {
		GenHasher.enableToughAsserts();
		DBHasher dbh = new DBHasher(5);
		CountingState s = dbh.newState();
		do {
			System.out.println(s);
		} while (dbh.step(s) != -1);
	}
}
