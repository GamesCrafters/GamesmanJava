package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayFile;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.RangeFile;

/**
 * @author Eric The reducer for the solver stage of loopy hadoop
 * @param <S>
 *            the gamestate for the game we're solving
 */
public class LoopySolverReducer<S extends State> extends
		Reducer<RangeFile, StateRecordPair, LongWritable, LongWritable> {
	private FileSystem fs;
	private Game<S> game;
	private final Random rand = new Random();

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration hadoopConf = context
					.getConfiguration();
			Configuration conf = Configuration.deserialize(hadoopConf
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
	public void reduce(RangeFile rangeToReduce,
			Iterable<StateRecordPair> candidateValues, Context context) {
		try {//TODO: is lfs needed?
			LocalFileSystem lfs = FileSystem.getLocal(context
					.getConfiguration());// we need an lfs

			String dbStringPath = lfs
					.pathToFile(rangeToReduce.myFile.getPath()).getPath();
			String numChildrenStringPath = dbStringPath + "_numChildren";

			String localDBStringPath = dbStringPath + "_local";
			String localNumChildrenStringPath = numChildrenStringPath
					+ "_local";

			Path localDBPath = new Path(localDBStringPath);
			Path localNumChildrenPath = new Path(localNumChildrenStringPath);

			Path hdfsDBPath = rangeToReduce.myFile.getPath();
			Path hdfsNumChildrenPath = new Path(numChildrenStringPath);

			fs.copyToLocalFile(hdfsDBPath, localDBPath);
			fs.copyToLocalFile(hdfsNumChildrenPath, localNumChildrenPath);

			ArrayFile.Reader numChildrenReader = new ArrayFile.Reader(lfs,
					localNumChildrenStringPath, context.getConfiguration());

			long rangeStart = rangeToReduce.myRange.firstRecord;
			long numRecords = rangeToReduce.myRange.numRecords;

			int[] range = new int[(int) numRecords];

			IntWritable temp = new IntWritable();
			for (int n = 0; n < range.length; n++) {
				numChildrenReader.get(n, temp);
				range[n] = temp.get();
			}

			numChildrenReader.close();

			lfs.delete(localNumChildrenPath, true); // no longer need the local
			// copy, it's in memory

			FileDatabase database = new FileDatabase(localDBStringPath);

			DatabaseHandle readHandle = database.getHandle(true);
			DatabaseHandle writeHandle = database.getHandle(false);

			Record candidateRecord = game.newRecord();
			Record currentRecord = game.newRecord();
			S gameState = game.newState();

			boolean changesMadeDB = false;
			boolean changesMadeNumChildren = false;
			for (StateRecordPair statePair : candidateValues) {
				game.hashToState(statePair.state, gameState);
				long currentLongRecord = database.readRecord(readHandle,
						statePair.state);
				game.longToRecord(gameState, statePair.record,
						candidateRecord);
				game.longToRecord(gameState, currentLongRecord, currentRecord);

				boolean possible = currentRecord.value != Value.IMPOSSIBLE;
				if (possible) {
					if (currentRecord.compareTo(candidateRecord) < 0) {
						// we found a better one!
						database.writeRecord(writeHandle, statePair.state,
								statePair.record);
						context.write(new LongWritable(statePair.state),
								new LongWritable(statePair.record));
						// output this to context
						changesMadeDB = true;
					} else if (currentRecord.value == Value.DRAW
							&& candidateRecord.value == Value.LOSE) {
						// we need to decrement numChildren!
						int n = (int) (statePair.state - rangeStart);
						range[n]--;// decrement num children

						if (range[n] == 0) {
							// this position is now a lose
							database.writeRecord(writeHandle, statePair.state,
									statePair.record);
							context.write(new LongWritable(statePair.state),
									new LongWritable(statePair.record));
							// output this to context
							changesMadeDB = true;
						} else {
							// we don't need to record this last change, we
							// won't be considering the num children of this
							// position again
							changesMadeNumChildren = true;
						}
					}
				}
			}

			if (changesMadeNumChildren) {
				ArrayFile.Writer arrayWriter = new ArrayFile.Writer(context
						.getConfiguration(), lfs, localNumChildrenStringPath,
						IntWritable.class);

				for (int n = 0; n < range.length; n++) {
					IntWritable numChildren = new IntWritable(range[n]);
					// put in the new value

					arrayWriter.append(numChildren);
				}

				arrayWriter.close();

				// now save the new file on hdfs
				Path tempPath = new Path(numChildrenStringPath + "_"
						+ rand.nextLong());
				// use a random long to prevent collisions in the expensive copy
				// step

				fs.moveFromLocalFile(localNumChildrenPath, tempPath);
				// copy the written array file to hdfs

				if (fs.exists(hdfsNumChildrenPath))
					fs.delete(hdfsNumChildrenPath, true);

				fs.rename(tempPath, hdfsNumChildrenPath);
				// rename to complete process
			}

			if (changesMadeDB) {
				Path tempPath = new Path(dbStringPath + "_" + rand.nextLong());
				// use a random long to prevent collisions in the expensive copy
				// step
				lfs.pathToFile(lfs.getChecksumFile(localDBPath)).delete();

				fs.moveFromLocalFile(localDBPath, tempPath);
				// copy the written database to hdfs
				fs.rename(tempPath, hdfsDBPath);
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
