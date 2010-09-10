package edu.berkeley.gamesman.parallel.hadoop;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.FileSystemDatabase;
import edu.berkeley.gamesman.database.GZippedFileSystemDatabase;
import edu.berkeley.gamesman.database.SplitDatabase;
import edu.berkeley.gamesman.game.TierGame;

public class SolveReduce extends MapReduceBase implements
		Reducer<IntWritable, LocFileWritable, IntWritable, LocFileWritable> {
	Configuration conf;
	SplitDatabase curTierDb;
	FileSystem hdfs;

	public void configure(JobConf job) {
		String prevTierPath = job.get("previousTier");
		try {
			hdfs = FileSystem.get(job);
			conf = FileSystemDatabase.readConfiguration(hdfs, new Path(
					prevTierPath));
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public void reduce(IntWritable tierWritable,
			Iterator<LocFileWritable> files,
			OutputCollector<IntWritable, LocFileWritable> output,
			Reporter reporter) throws IOException {
		TierGame tg = (TierGame) conf.getGame();
		int tier = tierWritable.get();
		String prevTierString = conf.getProperty("gamesman.db.uri") + ".tmp";
		SplitDatabase curTierDb = SplitDatabase.openSplitDatabase(
				prevTierString, conf, true, tg.hashOffsetForTier(tier), tg
						.numHashesForTier(tier), true);
		LinkedList<LocFileWritable> fileList = new LinkedList<LocFileWritable>();
		while (files.hasNext()) {
			fileList.add(files.next());
		}
		Collections.sort(fileList);
		Iterator<LocFileWritable> it = fileList.iterator();
		LocFileWritable cur = it.next();
		LocFileWritable next = it.next();
		while (cur.compareTo(next) < 0) {
			curTierDb.addDb(GZippedFileSystemDatabase.class.getName(), cur.file
					.getPath().toString(), cur.loc.get(), next.loc.get()
					- cur.loc.get());
			cur = next;
			if (it.hasNext())
				next = it.next();
			else
				next = new LocFileWritable(tg.hashOffsetForTier(tier + 1), null);
		}
		curTierDb.close();
		Path prevTierPath = new Path(prevTierString);
		hdfs.delete(prevTierPath, false);
		hdfs.copyFromLocalFile(prevTierPath, prevTierPath);
	}
}
