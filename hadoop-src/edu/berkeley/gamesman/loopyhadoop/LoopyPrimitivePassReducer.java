package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class LoopyPrimitivePassReducer<S extends State> extends
		Reducer<RangeFile, LongWritable, LongWritable, IntWritable> {
	private FileSystem fs;
	private Configuration conf;
	private Game<S> game;
	private final Random rand = new Random();

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration hadoopConf = context
					.getConfiguration();
			conf = Configuration.deserialize(hadoopConf
					.get("gamesman.configuration"));
			fs = FileSystem.get(hadoopConf);
			game = conf.getCheckedGame();
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	@Override
	public void reduce(RangeFile rangeFile, Iterable<LongWritable> hashes,
			Context context) {
		Path path = rangeFile.myFile.getPath();
		try {
			fs.copyToLocalFile(path, path);
			LocalFileSystem lfs = new LocalFileSystem();
			String stringPath = lfs.pathToFile(path).getPath();
			String newPath = stringPath + "_new";
			FileDatabase readDatabase = (FileDatabase) Database
					.openDatabase(stringPath);
			FileDatabase writeDatabase = (FileDatabase) Database.openDatabase(
					newPath, conf, rangeFile.myRange.firstRecord,
					rangeFile.myRange.numRecords, false, true);
			DatabaseHandle readHandle = readDatabase.getHandle(true);
			DatabaseHandle writeHandle = readDatabase.getHandle(false);
			Record record = game.newRecord();
			S gameState = game.newState();

			// How to read values out of the database
			long exampleHash = 0L;
			gameState = game.hashToState(exampleHash);
			long recordHash = readDatabase.readRecord(readHandle, exampleHash);
			game.longToRecord(gameState, recordHash, record);

			// Value is now stored in record. To check if it's been seen:
			boolean seenAlready = record.value == Value.IMPOSSIBLE;

			// To get primitive value for this position, do:
			Value primitive = game.primitiveValue(gameState);
			// If it's not primitive, this will return Value.UNDECIDED

			// Setting record fields
			record.value = primitive; // Or Draw if primitive is UNDECIDED
			record.remoteness = 0;
			// If remoteness doesn't make sense (such as with DRAW or
			// IMPOSSIBLE), then don't worry about it

			// To store values you just do:
			recordHash = game.recordToLong(gameState, record);
			writeDatabase.writeRecord(writeHandle, exampleHash, recordHash);

			// When you're all done
			readDatabase.close();
			writeDatabase.close();

			// And if you made any changes:
			Path tempPath = new Path(stringPath + "_" + rand.nextLong());
			fs.moveFromLocalFile(new Path(newPath), tempPath);
			fs.rename(tempPath, path);

		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}
}
