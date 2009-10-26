package edu.berkeley.gamesman.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritableList;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritable;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

class SplitDatabaseReduce implements Reducer<IntWritable, SplitDatabaseWritable, IntWritable, SplitDatabaseWritableList> {
	private JobConf jc;

	public void reduce(IntWritable tier, Iterator<SplitDatabaseWritable> dbList, OutputCollector<IntWritable, SplitDatabaseWritableList> outCollector, Reporter reporter) {
		SplitDatabaseWritableList list = new SplitDatabaseWritableList();
		list.setTier(tier.get());
		while (dbList.hasNext()) {
			list.addDatabase(dbList.next());
		}
		try {
			outCollector.collect(tier, list);
		} catch (IOException e) {
			Util.warn("Failed to collect finished databaseList "+tier, e);
		}
	}
	public void configure(JobConf jc) {
		this.jc = jc;
	}
	public void close() {
	}
}
