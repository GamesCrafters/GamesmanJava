package edu.berkeley.gamesman.parallel.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.NullOutputFormat;

public class HadoopMaster {
	public static void main(String[] args) throws IOException {
		JobConf conf = new JobConf(HadoopMaster.class);
		conf.setOutputKeyClass(LongWritable.class);
		conf.setOutputValueClass(Text.class);
		conf.setMapperClass(SolveMap.class);
		conf.setReducerClass(SolveReduce.class);
		conf.setInputFormat(SolveInput.class);
		conf.setOutputFormat(NullOutputFormat.class);

		JobClient.runJob(conf);
	}
}
