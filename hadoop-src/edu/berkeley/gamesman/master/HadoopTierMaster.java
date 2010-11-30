package edu.berkeley.gamesman.master;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.GamesmanConf;
import edu.berkeley.gamesman.game.TierGame;

public class HadoopTierMaster implements Runnable {
	private final Configuration hadoopConf;
	private final GamesmanConf gamesmanConf;
	private final TierGame game;

	public HadoopTierMaster(String confFile) throws ClassNotFoundException,
			IOException {
		hadoopConf = new Configuration();
		hadoopConf.addResource(new Path(confFile));
		gamesmanConf = new GamesmanConf(hadoopConf);
		game = (TierGame) gamesmanConf.getGame();
	}

	@Override
	public void run() {
		for (int tier = game.numberOfTiers() - 1; tier >= 0; tier--) {
			hadoopConf.setInt("tier", tier);
			solve(tier);
		}
	}

	private void solve(int tier) {
		// TODO Auto-generated method stub
		
	}
}
