package edu.berkeley.gamesman.parallel.tier;

import java.io.IOException;
import java.util.List;

import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.parallel.Input;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.*;

/**
 * Created by IntelliJ IDEA. User: user Date: Nov 30, 2010 Time: 10:03:34 AM To
 * change this template use File | Settings | File Templates.
 */
public class TierInput extends Input {
	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException
	{
		Configuration conf = job.getConfiguration();
		int tier = conf.getInt("tier", -1);
		edu.berkeley.gamesman.core.Configuration gc;
		try
		{
			gc = edu.berkeley.gamesman.core.Configuration.deserialize(conf
					.get("gamesman.configuration"));
		} catch (ClassNotFoundException e)
		{
			throw new Error(e);
		}
		if (tier < 0)
			throw new Error("No tier specified");
		TierGame game = (TierGame) gc.getGame();
		final long firstHash = game.hashOffsetForTier(tier);
		final long numHashes = game.numHashesForTier(tier);

		return getSplits(conf, firstHash, numHashes);
	}
}
