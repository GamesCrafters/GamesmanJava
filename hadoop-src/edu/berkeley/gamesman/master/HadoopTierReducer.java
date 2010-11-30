package edu.berkeley.gamesman.master;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class HadoopTierReducer extends
		Reducer<IntWritable, RangeFile, IntWritable, FileStatus> {
	public void reduce(IntWritable key, Iterable<RangeFile> values,
			Context context) throws IOException {
		// TODO Create file specific to tier key which contains names of files
		// with records.
	}
}
