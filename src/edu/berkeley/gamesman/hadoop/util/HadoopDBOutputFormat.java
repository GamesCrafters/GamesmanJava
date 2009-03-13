package edu.berkeley.gamesman.hadoop.util;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;

public class HadoopDBOutputFormat extends
		FileOutputFormat<BigIntegerWritable, RecordWritable> implements
		OutputFormat<BigIntegerWritable, RecordWritable> {

	@Override
	public RecordWriter<BigIntegerWritable, RecordWritable> getRecordWriter(
			FileSystem fs, JobConf jc, String name, Progressable progress)
			throws IOException {
		
		final FSDataOutputStream out = fs.create(new Path(getWorkOutputPath(jc),"slice"));
		
		return new RecordWriter<BigIntegerWritable, RecordWritable>(){

			public void close(Reporter arg0) throws IOException {
				out.flush();
				out.close();
			}

			public void write(BigIntegerWritable loc, RecordWritable rec)
					throws IOException {
				System.out.println("writing "+loc+ " -> "+rec);
				rec.get().writeStream(out);
			}
			
		};
	}

}