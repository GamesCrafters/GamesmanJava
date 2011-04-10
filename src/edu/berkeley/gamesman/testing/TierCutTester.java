package edu.berkeley.gamesman.testing;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.TierCutDatabase;
import edu.berkeley.gamesman.game.Connect4;


public class TierCutTester {
	
	public static void main(String args[]) throws ClassNotFoundException {
		
		Configuration conf = new Configuration(Configuration.readProperties("jobs/Connect4/Connect4_54.job"));
		Connect4 game = (Connect4) conf.getGame();
		int tiers = game.numberOfTiers();
		long hashes = 0;
		int numTiersCut = 1;
		for (int i=0; i< tiers; i++) {
			if (i != tiers - 1 &&  i % (numTiersCut + 1) == 0) {
				hashes += game.numHashesForTier(i);
			}
		}
		System.out.println("Total Number Hashes " + game.numHashes());
		System.out.println("Only Last Tier Cut " + (game.numHashes() - game.numHashesForTier(tiers-1)));
		System.out.println("Every Other Tier Cut " +  hashes);
		hashes = 0;
		numTiersCut = 2;
		for (int i=0; i< tiers; i++) {
			if (i != tiers - 1 &&  i % (numTiersCut + 1) == 0) {
				hashes += game.numHashesForTier(i);
			}
		}
		System.out.println("2 Tiers cut inbetween each stored tier " +  hashes);
		hashes = 0;
		numTiersCut = 3;
		for (int i=0; i< tiers; i++) {
			if (i != tiers - 1 &&  i % (numTiersCut + 1) == 0) {
				hashes += game.numHashesForTier(i);
			}
		}
		System.out.println("3 Tiers cut inbetween each stored tier " +  hashes);
		hashes = 0;
		numTiersCut = 4;
		for (int i=0; i< tiers; i++) {
			if (i != tiers - 1 &&  i % (numTiersCut + 1) == 0) {
				hashes += game.numHashesForTier(i);
			}
		}
		System.out.println("4 Tiers cut inbetween each stored tier " +  hashes);
		hashes = 0;
		numTiersCut = 5;
		for (int i=0; i< tiers; i++) {
			if (i != tiers - 1 &&  i % (numTiersCut + 1) == 0) {
				hashes += game.numHashesForTier(i);
			}
		}
		System.out.println("5 Tiers cut inbetween each stored tier " +  hashes);
		TierCutDatabase tcDb;
		try {
			boolean reading = false;
			boolean writing = true;
			tcDb = new TierCutDatabase(conf, 0, game.numHashes(), reading, writing);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		//} catch (ClassNotFoundException e1) {
		//	throw new RuntimeException(e1);
		//}
	}

}
