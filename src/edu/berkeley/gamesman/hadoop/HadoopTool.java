package edu.berkeley.gamesman.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.Tool;

public class HadoopTool extends Configured implements Tool {

	public int run(String[] arg0) throws Exception {
		Configuration conf = getConf();
		JobConf job = new JobConf(conf,TierMapReduce.class);
		
		job.setJobName("Tier Map-Reduce");
		job.setMapperClass(TierMapReduce.class);
		job.setReducerClass(TierMapReduce.class);
		
		JobClient.runJob(job);
		
		return 0;
	}

}
