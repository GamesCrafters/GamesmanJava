package edu.berkeley.gamesman.parallel;

import org.apache.hadoop.conf.Configuration;

public class GamesmanParser {
	public static String getGameName(Configuration conf) {
		return conf.get("gamesman.hadoop.game.name");
	}
}
