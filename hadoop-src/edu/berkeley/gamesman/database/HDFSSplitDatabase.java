package edu.berkeley.gamesman.database;

import java.io.IOException;

import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;

public class HDFSSplitDatabase extends SplitDatabase {

	public HDFSSplitDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException, ClassNotFoundException {
		super(HDFSInfo.getHDFS().open(new Path(uri)), uri, conf, firstRecordIndex,
				numRecords, reading, writing);
	}
}
