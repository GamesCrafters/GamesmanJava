package edu.berkeley.gamesman.master;

import java.io.IOException;

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
	Configuration conf;
	FileSystem fs;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration conf = context
					.getConfiguration();
			this.conf = new GamesmanConf(conf);
			fs = FileSystem.get(conf);
			tier.set(conf.getInt("tier", -1));
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	public void map(Range key, NullWritable value, Context context)
			throws IOException {
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
