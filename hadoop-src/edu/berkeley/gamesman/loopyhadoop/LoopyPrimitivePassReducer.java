package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
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
	private IntWritable zero = new IntWritable(0);

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
			FileDatabase database = new FileDatabase(stringPath);
			DatabaseHandle readHandle = database.getHandle(true);
			DatabaseHandle writeHandle = database.getHandle(false);
			Record record = game.newRecord();
			S gameState = game.newState();

			/*
			 * all hashes are going to be in the same file so this goes outside
			 * the loop
			 */
			boolean changesMade = false;
			for (LongWritable hash : hashes) {
				long longHash = hash.get();
				game.hashToState(longHash, gameState);
				long recordHash = database.readRecord(readHandle, longHash);
				game.longToRecord(gameState, recordHash, record);
				/* get the record associated with this hash */

				boolean visited = record.value != Value.IMPOSSIBLE;

				if (!visited) {
					Value primitiveValue = game.primitiveValue(game
							.hashToState(longHash));
					switch (primitiveValue) {
					case UNDECIDED:
						// value is not primitive
						record.value = Value.DRAW;
						break;
					case WIN:
					case LOSE:
					case TIE:
						// do the same for all these cases, all primitive
						// TODO: append to a primitive file for this range
						record.value = primitiveValue;
						record.remoteness = 0;
						// we have to deal with this during the solve for
						// non-primitives?
						break;
					default:
						throw new Error("WTF did primitive value return?");
					}

					recordHash = game.recordToLong(gameState, record);
					database.writeRecord(writeHandle, longHash, recordHash);
					// write this record to the database
					changesMade = true;
					context.write(hash, zero);
				}
			}

			database.close();

			if (changesMade) {
				Path tempPath = new Path(stringPath + "_" + rand.nextLong());
				// use a random long to prevent collisions in the expensive copy
				// step
				fs.moveFromLocalFile(path, tempPath);
				// copy the written database to hdfs
				fs.rename(tempPath, path);
				// rename to complete process
			}
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
