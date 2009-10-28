package edu.berkeley.gamesman.hadoop.util;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;

/**
 * SplitDatabaseOutputFormat writes a list of databases to a file.
 * This class doesn't do anything out of the ordinary, and basically calls
 * SplitDatabaseWritableList.write().
 * 
 * @see SplitDatabaseWritableList
 * @author Patrick Horn
 */
@SuppressWarnings("deprecation")
public class SplitDatabaseOutputFormat implements
		OutputFormat<IntWritable, SplitDatabaseWritableList> {
	public RecordWriter<IntWritable, SplitDatabaseWritableList> getRecordWriter(
			FileSystem fs, JobConf jc, String name, Progressable progress)
			throws IOException {

		final FSDataOutputStream out = fs.create(new Path(name));

		return new RecordWriter<IntWritable, SplitDatabaseWritableList>() {

			public void close(Reporter arg0) throws IOException {
				out.flush();
				out.close();
			}

			public void write(IntWritable tier, SplitDatabaseWritableList rec)
					throws IOException {
				rec.write(out);
			}

		};
	}

	public void checkOutputSpecs(FileSystem fs, JobConf jc) {
	}
}