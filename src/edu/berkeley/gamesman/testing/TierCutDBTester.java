package edu.berkeley.gamesman.testing;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.TierCutDatabase;
import edu.berkeley.gamesman.game.Connect4;

public class TierCutDBTester {

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		Configuration conf = new Configuration("jobs/Connect4/Connect4_76_Parallel.job");
		Connect4 game = (Connect4) conf.getGame();
		game.setStartingPosition(0);
		TierCutDatabase myTierDb = new TierCutDatabase(conf, 0, game.numHashes(), false, false);
		System.out.println(myTierDb.getFakeNumRecords(0, game.numHashes(), game));
		System.out.println(game.numHashes());
		//System.out.println(game.numHashesForTier(game.numberOfTiers() - 1));
	}
	

}
