package edu.berkeley.gamesman.parallel.tier;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.GZippedFileSystemDatabase;
import edu.berkeley.gamesman.database.SplitDBMaker;
import edu.berkeley.gamesman.database.SplitDatabase;
import edu.berkeley.gamesman.database.SplitDatabase.DatabaseDescriptor;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.parallel.RangeFile;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class HadoopTierReducer extends
		Reducer<IntWritable, RangeFile, IntWritable, FileStatus> {
	Configuration conf;
	FileSystem fs;
	private final IntWritable tier = new IntWritable();
	private SplitDBMaker writeDb;
	private String dbUri;
	private Path dbPath;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration conf = context
					.getConfiguration();
			this.conf = Configuration.deserialize(conf
					.get("gamesman.configuration"));
			fs = FileSystem.get(conf);
			tier.set(conf.getInt("tier", -1));
			if (tier.get() == -1)
				throw new Error("No tier specified");
			dbUri = this.conf.getProperty("gamesman.hadoop.tierDb");
			dbUri = dbUri + "_" + tier.get() + ".db";
			dbPath = new Path(dbUri);
			TierGame g = (TierGame) this.conf.getGame();
			long tierStart = g.hashOffsetForTier(tier.get());
			long tierHashes = g.numHashesForTier(tier.get());
			writeDb = new SplitDBMaker(dbUri + "_local", this.conf, tierStart,
					tierHashes);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public void reduce(IntWritable key, Iterable<RangeFile> values,
			Context context) throws IOException {
		Iterator<RangeFile> it = values.iterator();
		LinkedList<SplitDatabase.DatabaseDescriptor> descriptorList = new LinkedList<SplitDatabase.DatabaseDescriptor>();
		while (it.hasNext()) {
			RangeFile temp = it.next();
			long firstRecord = temp.myRange.firstRecord;
			long numRecords = temp.myRange.numRecords;
			String uri = temp.myFile.getPath().toString();
			descriptorList.add(new DatabaseDescriptor(
					GZippedFileSystemDatabase.class.getName(), uri,
					firstRecord, numRecords));
		}
		Collections.sort(descriptorList);
		for (DatabaseDescriptor dd : descriptorList) {
			writeDb.addDb(dd);
		}
		writeDb.close();
		Path dbLocalPath = new Path(dbUri + "_local");
		fs.copyFromLocalFile(dbLocalPath, dbPath);
		new File(dbUri + "_local").delete();
	}
}
