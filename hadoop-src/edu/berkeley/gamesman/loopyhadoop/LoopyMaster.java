package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.Game;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.parallel.Input;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;

public class LoopyMaster<S extends State> implements Runnable {
	private final org.apache.hadoop.conf.Configuration hadoopConf;
	private final Configuration gamesmanConf;
	private final FileSystem fs;
	private Path dbMapPath;
	private final Game<S> game;

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
		game = gamesmanConf.getCheckedGame();
		fs = FileSystem.get(hadoopConf);
	}

	@Override
	public void run() {
		try {
			createDatabase();
			markLegalPositions();
			solve();
		} catch (IOException e) {
			throw new Error("Our program asploded :(.", e);
		}
	}

	private void createDatabase() throws IOException {
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
		String pathString = "Loopy_Hadoop_Solve_DB_Directory_"
				+ gamesmanConf.getGame().getClass().getSimpleName();
		Path sequenceFileDir = getPath(pathString);

		FileOutputFormat.setOutputPath(j, sequenceFileDir);

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

	private Path getPath(String pathString) {
		Path sequenceFileDir;
		String dir = gamesmanConf.getProperty("workingDirectory", "");

		if (dir.isEmpty()) {
			sequenceFileDir = new Path(pathString);
		} else {
			sequenceFileDir = new Path(dir + "/" + pathString);
		}
		return sequenceFileDir;
	}

	private void markLegalPositions() throws IOException {

		Path sequenceFileInputDir = getPath("Loopy_Hadoop_Solve_Input_Stage2_"
				+ gamesmanConf.getGame().getClass().getSimpleName());
		Path sequenceFileOutputDir = getPath("Loopy_Hadoop_Solve_Output_Stage2_"
				+ gamesmanConf.getGame().getClass().getSimpleName());
		Path primitiveOutputDir = getPath("Loopy_Hadoop_Primitives_"
				+ gamesmanConf.getGame().getClass().getSimpleName());
		fs.mkdirs(primitiveOutputDir);
		fs.mkdirs(sequenceFileInputDir);

		hadoopConf.set("primitive.output", primitiveOutputDir.toString());

		Path sequenceFileInputFile = new Path(sequenceFileInputDir,
				"Starting_Positions_Flag");
		
		SequenceFile.Writer writer = new SequenceFile.Writer(fs, hadoopConf,
				sequenceFileInputFile, LongWritable.class, IntWritable.class);
		
		writer.append(new LongWritable(-1), new IntWritable(0));
		
		writer.close();

		int n = 1;
		while (directoryHasKVPairs(sequenceFileInputDir)) {
			Job j = new Job(hadoopConf, "Find legal positions pass: " + (n++));
			j.setJarByClass(LoopyPrimitivePassMapper.class);
			j.setMapperClass(LoopyPrimitivePassMapper.class);
			j.setMapOutputKeyClass(RangeFile.class);
			j.setMapOutputValueClass(LongWritable.class);
			j.setReducerClass(LoopyPrimitivePassReducer.class);
			j.setInputFormatClass(SequenceFileInputFormat.class);
			j.setOutputFormatClass(SequenceFileOutputFormat.class);
			j.setOutputKeyClass(LongWritable.class);
			j.setOutputValueClass(IntWritable.class);

			FileInputFormat.setInputPaths(j, sequenceFileInputDir);
			FileOutputFormat.setOutputPath(j, sequenceFileOutputDir);

			try {
				j.waitForCompletion(true);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			fs.delete(sequenceFileInputDir, true);
			fs.rename(sequenceFileOutputDir, sequenceFileInputDir);
		}

		fs.delete(sequenceFileInputDir, true);
		// we're not feeding it anymore, so kill the dir
	}

	private boolean directoryHasKVPairs(Path sequenceFileDir) {
		try {
			FileStatus[] files = fs.listStatus(sequenceFileDir);
			LongWritable key = new LongWritable();
			IntWritable value = new IntWritable();

			for (FileStatus file : files) {
				SequenceFile.Reader reader = new SequenceFile.Reader(fs, file
						.getPath(), hadoopConf);
				// everything in the input directory should be a sequence file

				if (reader.next(key, value)) {// if the file has any kv pairs,
					// we return true
					return true;
				}
			}

			return false;
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private void solve() {
		// TODO Auto-generated method stub

	}
}