package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.parallel.Input;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;

public class LoopyMaster implements Runnable {
	private final org.apache.hadoop.conf.Configuration hadoopConf;
	private final Configuration gamesmanConf;
	private final FileSystem fs;
	private Path dbMapPath;

	public static void main(String[] args) throws IOException {
		GenericOptionsParser gop = new GenericOptionsParser(args);
		LoopyMaster loopyMaster = new LoopyMaster(gop);
		loopyMaster.run();
	}

	public LoopyMaster(GenericOptionsParser gop) throws IOException {
		String[] unparsedArgs = gop.getRemainingArgs();
		try {
			gamesmanConf = new Configuration(unparsedArgs[0]);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		hadoopConf = gop.getConfiguration();
		hadoopConf.set("gamesman.configuration", gamesmanConf.serialize());
		fs = FileSystem.get(hadoopConf);
	}

	@Override
	public void run() {
		try {
			createDatabase();
			FileStatus primitives = markLegalPositions();
			solve(primitives);
		} catch (IOException e) {
			throw new Error("Our program asploded :(.", e);
		}
	}

	private void createDatabase() throws IOException {
		// TODO Auto-generated method stub
		Job j = new Job(hadoopConf, "Initial database creation");
		j.setJarByClass(LoopyDatabaseCreationMapper.class);
		j.setMapperClass(LoopyDatabaseCreationMapper.class);
		j.setMapOutputKeyClass(IntWritable.class);
		j.setMapOutputValueClass(RangeFile.class);
		j.setReducerClass(LoopyDatabaseCreationReducer.class);
		j.setInputFormatClass(Input.class);
		j.setOutputFormatClass(SequenceFileOutputFormat.class);
		j.setOutputKeyClass(Range.class);
		j.setOutputValueClass(FileStatus.class);

		Path sequenceFileDir = new Path("Loopy_Hadoop_Solve_"
				+ gamesmanConf.getGame().getClass().getSimpleName());
		SequenceFileOutputFormat.setOutputPath(j, sequenceFileDir);

		try {
			j.waitForCompletion(true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FileStatus[] files = fs.listStatus(sequenceFileDir);
		if (files.length != 1) {
			throw new Error("bad files.length: " + files.length);
		}

		dbMapPath = files[0].getPath();

		SequenceFile.Reader r = new SequenceFile.Reader(fs, dbMapPath,
				hadoopConf);
		System.out.println("Key = " + r.getKeyClassName());
		System.out.println("Value = " + r.getValueClassName());
		Range range = new Range();
		FileStatus fileStatus = new FileStatus();
		while (r.next(range, fileStatus)) {
			System.out.println(range.firstRecord + "-"
					+ (range.firstRecord + range.numRecords - 1) + ": "
					+ fileStatus.getPath().toString());
		}

		hadoopConf.set("db.map.path", dbMapPath.toString());
	}

	private FileStatus markLegalPositions() {
		return null;
		// TODO Auto-generated method stub

	}

	private void solve(FileStatus primitives) {
		// TODO Auto-generated method stub

	}
}