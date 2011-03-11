package edu.berkeley.gamesman.parallel.tier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class ClearJunk {
	public static class JunkReader extends
			RecordReader<IntWritable, IntWritable> {
		private JunkSplit mySplit = new JunkSplit();
		private final IntWritable myKey = new IntWritable(1);
		private final IntWritable myValue = new IntWritable(1);
		private boolean read = false;

		@Override
		public void initialize(InputSplit split, TaskAttemptContext context)
				throws IOException, InterruptedException {
			mySplit.set((JunkSplit) split);
		}

		@Override
		public boolean nextKeyValue() throws IOException, InterruptedException {
			if (read)
				return false;
			else {
				read = true;
				return true;
			}
		}

		@Override
		public IntWritable getCurrentKey() throws IOException,
				InterruptedException {
			if (read)
				return myKey;
			else
				return null;
		}

		@Override
		public IntWritable getCurrentValue() throws IOException,
				InterruptedException {
			if (read)
				return myValue;
			else
				return null;
		}

		@Override
		public float getProgress() throws IOException, InterruptedException {
			return 0F;
		}

		@Override
		public void close() throws IOException {
		}

	}

	public static class JunkMapper extends
			Mapper<IntWritable, IntWritable, IntWritable, IntWritable> {
		public void map(IntWritable key, IntWritable value, Context context) {
			try {
				File f = new File("/scratch/ttt_fold");
				for (File f2 : f.listFiles())
					f2.delete();
				f.delete();
			} catch (SecurityException se) {
				se.printStackTrace();
			}
			try {
				File f = new File("/scratch/y_fold");
				for (File f2 : f.listFiles())
					f2.delete();
				f.delete();
			} catch (SecurityException se) {
				se.printStackTrace();
			}
		}
	}

	public static class JunkInput extends InputFormat<IntWritable, IntWritable>
			implements Writable {

		@Override
		public List<InputSplit> getSplits(JobContext context)
				throws IOException, InterruptedException {
			ArrayList<InputSplit> al = new ArrayList<InputSplit>(36);
			for (int i = 0; i < 36; i++) {
				al.add(new JunkSplit());
			}
			return al;
		}

		@Override
		public RecordReader<IntWritable, IntWritable> createRecordReader(
				InputSplit split, TaskAttemptContext context)
				throws IOException, InterruptedException {
			return new JunkReader();
		}

		@Override
		public void write(DataOutput out) throws IOException {
		}

		@Override
		public void readFields(DataInput in) throws IOException {
		}

	}

	public static class JunkSplit extends InputSplit implements Writable {

		@Override
		public long getLength() throws IOException, InterruptedException {
			return 0;
		}

		public void set(JunkSplit split) {
		}

		@Override
		public String[] getLocations() throws IOException, InterruptedException {
			return new String[0];
		}

		@Override
		public void write(DataOutput out) throws IOException {
		}

		@Override
		public void readFields(DataInput in) throws IOException {
		}

	}

	public static void main(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		Configuration conf = gop.getConfiguration();
		Job job = new Job(conf);
		job.setMapperClass(JunkMapper.class);
		Path p = new Path("/tmp/tempOutputDir");
		FileSystem fs = FileSystem.get(conf);
		if (fs.exists(p))
			fs.delete(p, true);
		FileOutputFormat.setOutputPath(job, p);
		job.setInputFormatClass(JunkInput.class);
		job.setJarByClass(JunkMapper.class);
		job.waitForCompletion(true);
		fs.delete(p, true);
	}
}
