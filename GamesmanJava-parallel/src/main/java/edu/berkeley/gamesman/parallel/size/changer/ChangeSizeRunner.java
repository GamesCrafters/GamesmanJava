package edu.berkeley.gamesman.parallel.size.changer;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.parallel.ranges.ChildMap;
import edu.berkeley.gamesman.parallel.ranges.MainRecords;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.parallel.ranges.Suffix;
import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.solver.TaskRunner;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;

public class ChangeSizeRunner {
	public static <S extends GenState, GR extends FixedLengthWritable> void main(
			String[] args) throws IOException, InterruptedException,
			ClassNotFoundException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		Configuration conf = parser.getConfiguration();
		String[] remainArgs = parser.getRemainingArgs();
		Path confPath = new Path(remainArgs[0]);
		ConfParser.addParameters(conf, confPath, true);
		Path inputFolder = new Path(remainArgs[1]);
		Path outputFolder = new Path(remainArgs[2]);
		int newVarLen = Integer.parseInt(remainArgs[3]);
		conf.setInt("new.var.len", newVarLen);
		ChangeSizeRunner.<S, GR> runJob(conf, inputFolder, outputFolder);
	}

	private static <S extends GenState, GR extends FixedLengthWritable> void runJob(
			Configuration conf, Path inputFolder, Path outputFolder)
			throws IOException, InterruptedException, ClassNotFoundException {
		RangeTree<S, GR> tree = (RangeTree<S, GR>) ConfParser
				.<Suffix<S>, MainRecords<GR>, ChildMap, WritableTreeMap<GR>, WritableTreeMap<GR>, ChildMap> newTree(conf);
		tree.prepareRun(conf);
		Job j = new Job(conf);
		j.setMapperClass(ChangeSizeMapper.class);
		j.setInputFormatClass(SequenceFileInputFormat.class);
		j.setOutputFormatClass(MapFileOutputFormat.class);
		Class<? extends Partitioner<Suffix<S>, MainRecords<GR>>> partitionerClass = ConfParser
				.<Suffix<S>, MainRecords<GR>> getCleanupPartitionerClass(conf);
		j.setPartitionerClass(partitionerClass);
		FileInputFormat.setInputPaths(j, inputFolder);
		FileOutputFormat.setOutputPath(j, outputFolder);
		TaskRunner.enableCompression(j, tree.getCleanupCompressionType());
		j.waitForCompletion(true);
	}
}
