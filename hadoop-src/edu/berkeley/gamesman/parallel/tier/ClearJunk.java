package edu.berkeley.gamesman.parallel.tier;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class ClearJunk {
	public static class JunkMapper extends
			Mapper<IntWritable, IntWritable, IntWritable, IntWritable> {
		public void map(IntWritable key, IntWritable value, Context context) {
			File f = new File("/scratch/ttt_fold");
			f.delete();
		}
	}

	public static class JunkReducer extends
			Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {

	}

	public static void main(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		Configuration conf = gop.getConfiguration();
		Job job = new Job(conf);
		job.setMapperClass(JunkMapper.class);
		job.setReducerClass(JunkReducer.class);
		Path p = new Path("/tmp/tempOutputDir");
		FileOutputFormat.setOutputPath(job, p);
		job.waitForCompletion(true);
	}
}
