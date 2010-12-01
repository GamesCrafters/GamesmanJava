package edu.berkeley.gamesman.master;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHeader;
import edu.berkeley.gamesman.database.DistributedDatabase;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.solver.TierSolver;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.GamesmanConf;

public class HadoopTierMapper extends
		Mapper<Range, NullWritable, IntWritable, RangeFile> {
	private final RangeFile mapValue = new RangeFile();
	private final IntWritable tier = new IntWritable();
        private TierSolver solver;
        private TierGame game;
        private FileDatabase readDb;

	Configuration conf;
	FileSystem fs;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration conf = context
					.getConfiguration();
			this.conf = new GamesmanConf(conf);
                        game = (TierGame) this.conf.getGame();

			fs = FileSystem.get(conf);
			tier.set(conf.getInt("tier", -1));
                        solver = new TierSolver(this.conf);


                        //TODO: BRIAN HAS NO HEAD!!!!!!!!!!!






		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	public void map(Range key, NullWritable value, Context context)
			throws IOException {
                    long firstHash = key.firstRecord;
                    long numHashes = key.numRecords;
                    byte[] headBytes = new byte[18];
		    Database.readFully(System.in, headBytes, 0, 18);
		    DatabaseHeader head = new DatabaseHeader(headBytes);
                    int prevTier = tier.get() + 1;
                    String name = conf.toString() + "_" + tier.get() + "_" + key.firstRecord;
                    if (prevTier < game.numberOfTiers()) {
				long firstTierRecord = game.hashOffsetForTier(prevTier);
				long numTierRecords = game.numHashesForTier(prevTier);
				readDb = new FileDatabase(name, conf, true, firstHash, numHashes, head.getHeader(
						firstTierRecord, numTierRecords));
				solver.setReadDb(readDb);
			} else {
				readDb = null;
			}



            int t = tier.get();
            int threads = conf.getInteger("gamesman.threads", 1);
            List<WorkUnit> list = null;
	    WorkUnit wu = solver.prepareSolve(conf, t, firstHash, numHashes);

            wu.conquer();
		// TODO: Add tier slave stuff

            
		FileStatus finalFile = null;
		boolean successful = false;
		while (!successful) {
			try {
				mapValue.set(key, finalFile);
				context.write(tier, mapValue);
				successful = true;
			} catch (InterruptedException e) {
				successful = false;
				e.printStackTrace();
			}
		}
	}
}
