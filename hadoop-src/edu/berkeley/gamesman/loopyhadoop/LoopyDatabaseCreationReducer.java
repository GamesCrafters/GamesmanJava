package edu.berkeley.gamesman.loopyhadoop;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class LoopyDatabaseCreationReducer extends
		Reducer<IntWritable, FileStatus, IntWritable, NullWritable>
{

}
