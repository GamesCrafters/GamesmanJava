package edu.berkeley.gamesman.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.hadoop.util.SplitDatabaseWritable;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class TierMap<S> implements Mapper<LongWritable, LongWritable, LongWritable, SplitDatabaseWritable> {

	public static abstract class MapReduceDatabase extends Database {
		public MapReduceDatabase() {
		}
		public MapReduceDatabase(FileSystem fs) {
			this.fs = fs;
		}

		public void setFilesystem (FileSystem fs) {
			this.fs = fs;
		}
		void setDelegate(TierMap tmr) {
			this.delegate = tmr;
		}

		public void setOutputDirectory (Path dir) {
			outputFilenameBase = dir;
			// dir contains a trailing slash
		}

		protected TierMap delegate;
		protected FileSystem fs;
		protected Path outputFilenameBase;
	}

	protected TieredGame<S> game;
	protected Hasher<S> hasher;
	protected Solver solver;
	protected MapReduceDatabase db;

	int tier;

	private Reporter reporter;
	private OutputCollector<LongWritable, SplitDatabaseWritable> outRec;

	public static Configuration config;
	public static JobConf jobconf;

	public void configure(JobConf conf) {
		//Class<TieredGame<Object>> gc = null;
		//Class<Database> gd = null;
		//Class<Hasher<?>> gh = null;
		final String base = "edu.berkeley.gamesman.";
		//Properties props = new Properties(System.getProperties());

		try {
			config = Configuration.load(Util.decodeBase64(conf.get("configuration_data")));
			jobconf = conf;
			db = Util.typedInstantiate(base+"database."+config.getProperty("gamesman.database"), MapReduceDatabase.class);
		} catch (ClassNotFoundException e) {
			Util.fatalError("failed to load configuration class!", e);
			return;
		}
		
		//db.initialize(conf.get("dburi"),config);
		try {
			db.setFilesystem(FileSystem.get(jobconf));
		} catch (IOException e) {
			Util.fatalError("Unable to get filesystem", e);
		}
		db.setOutputDirectory(FileOutputFormat.getWorkOutputPath(jobconf));
		db.initialize(FileOutputFormat.getOutputPath(conf).toString(), config);
		tier = Integer.parseInt(jobconf.get("tier"));
		game = Util.checkedCast(config.getGame());
		hasher = Util.checkedCast(config.getHasher());
		try {
			solver = Util.typedInstantiate(base+"solver."+config.getProperty("gamesman.solver"), Solver.class);
		} catch (ClassNotFoundException e) {
			Util.fatalError("failed to load configuration class!", e);
			return;
		}

		assert Util.debugFormat(DebugFacility.HADOOP, "Hadoop is ready to work! (%s, %s)", game.describe(), hasher.toString());
	}

	public void close() throws IOException {
		db.close();
	}

	public void map(LongWritable startTask, LongWritable endTask,
			OutputCollector<LongWritable, SplitDatabaseWritable> outRec,
			Reporter reporter) throws IOException {
		this.reporter = reporter;
		this.outRec = outRec;
		solver.prepareSolve(config, tier, startTask.get(), endTask.get());
	}

	public void started(int tier, long task, String filename, long startRecord, long stopRecord) {
		reporter.setStatus("Started "+tier+"/"+task+" ["+startRecord+"-"+stopRecord+"]");
	}
	public void finished(int tier, long task, String filename, long startRecord, long stopRecord) {
		reporter.progress();
		SplitDatabaseWritable w = new SplitDatabaseWritable(tier);
		w.set(filename, startRecord, stopRecord);
		try {
			outRec.collect(new LongWritable(task), w);
		} catch (IOException e) {
			Util.warn("Failed to collect finished database "+tier+"/"+task+"@"+filename, e);
		}
	}

};

