package edu.berkeley.gamesman.hadoop;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritableList;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritable;
import edu.berkeley.gamesman.util.Util;

/**
 * Reducer for the hadoop solver. The Reducer is reducing a lot of small
 * database chunks down to a single database file that lists all the chunks.
 * 
 * The reducer will only ever have one job (per tier), and is really just
 * collecting the output of all the mappers into one file.
 * 
 * @author Patrick Horn
 */
@SuppressWarnings("deprecation")
public class SplitDatabaseReduce implements Reducer<IntWritable, SplitDatabaseWritable, IntWritable, SplitDatabaseWritableList> {
	public void reduce(IntWritable tier, Iterator<SplitDatabaseWritable> dbList, OutputCollector<IntWritable, SplitDatabaseWritableList> outCollector, Reporter reporter) {
		SplitDatabaseWritableList list = new SplitDatabaseWritableList(tier.get());
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
	}
	public void close() {
	}
}
