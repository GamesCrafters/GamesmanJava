package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import edu.berkeley.gamesman.database.GZippedFileDatabase;
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
	private Configuration conf;
	private S gameState;
	private StateRecordPair statePair;
	private Record candidateRecord;
	private Record currentRecord;
	private DatabaseHandle readHandle;
	private DatabaseHandle writeHandle;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration hadoopConf = context
					.getConfiguration();
			conf = Configuration.deserialize(hadoopConf
					.get("gamesman.configuration"));
			fs = FileSystem.get(hadoopConf);
			game = conf.getCheckedGame();
			gameState = game.newState();
			candidateRecord = game.newRecord();
			currentRecord = game.newRecord();
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	@Override
	public void reduce(RangeFile rangeToReduce,
			Iterable<StateRecordPair> candidateValues, Context context) {
		try {
			ArrayList<StateRecordPair> sortedCandidates = new ArrayList<StateRecordPair>();

			for (StateRecordPair candidate : candidateValues) {
				sortedCandidates.add(new StateRecordPair(candidate.state,
						candidate.record));
			}

			// for (StateRecordPair candidate : sortedCandidates) {
			// System.out.println("candidate state: " + candidate.state);
			// }

			Collections.sort(sortedCandidates);

			LocalFileSystem lfs = FileSystem.getLocal(context
					.getConfiguration());// we need an lfs

			String dbStringPath = rangeToReduce.myFile.toString();
			String numChildrenStringPath = dbStringPath + "_numChildren";

			String localDBStringPath = dbStringPath + "_local";
			String newLocalDBStringPath = dbStringPath + "_local_new";
			String localNumChildrenStringPath = numChildrenStringPath
					+ "_local";

			Path localDBPath = new Path(localDBStringPath);
			Path newLocalDBPath = new Path(newLocalDBStringPath);
			Path localNumChildrenPath = new Path(localNumChildrenStringPath);

			Path hdfsDBPath = new Path(rangeToReduce.myFile.toString());
			Path hdfsNumChildrenPath = new Path(numChildrenStringPath);

			fs.copyToLocalFile(hdfsDBPath, localDBPath);
			fs.copyToLocalFile(hdfsNumChildrenPath, localNumChildrenPath);

			ArrayFile.Reader numChildrenReader = new ArrayFile.Reader(lfs,
					localNumChildrenStringPath, context.getConfiguration());

			long rangeStart = rangeToReduce.myRange.firstRecord;
			long numRecords = rangeToReduce.myRange.numRecords;

			int[] numChildrenRemaining = new int[(int) numRecords];

			IntWritable temp = new IntWritable();
			for (int n = 0; n < numChildrenRemaining.length; n++) {
				numChildrenReader.get(n, temp);
				numChildrenRemaining[n] = temp.get();
			}

			numChildrenReader.close();

			lfs.delete(localNumChildrenPath, true); // no longer need the local
			// copy, it's in memory

			GZippedFileDatabase database = new GZippedFileDatabase(
					localDBStringPath, conf, rangeStart, numRecords, true,
					false);
			GZippedFileDatabase newDatabase = new GZippedFileDatabase(
					newLocalDBStringPath, conf, rangeStart, numRecords, false,
					true);

			readHandle = database.getHandle(true);
			database.prepareReadRange(readHandle, rangeStart, numRecords);

			writeHandle = newDatabase.getHandle(false);
			newDatabase.prepareWriteRange(writeHandle, rangeStart, numRecords);

			ChangeTracker changeTracker = new ChangeTracker();

			Iterator<StateRecordPair> pairIter = sortedCandidates.iterator();
			statePair = pairIter.next();

			// System.out.println("\nEntering main loop stage 3 reducer\n");
			int numWritten = 0;
			int hashCount = 0;
			for (long i = 0; i < numRecords; i++) {
				long currentLongRecord = database.readNextRecord(readHandle);

				boolean written = false;

				if (rangeStart + i == statePair.state) {
					hashCount++;
					written = decideNextRecord(context, rangeStart,
							numChildrenRemaining, newDatabase, changeTracker,
							pairIter, currentLongRecord);
				}

				if (!written) {
					newDatabase.writeNextRecord(writeHandle, currentLongRecord);
				} else
					numWritten++;
			}

			// System.out.println("Number of records visited: " + hashCount);
			// System.out.println("Number of records written: " + numWritten);
			// System.out.println("Number of records processed: "
			// + sortedCandidates.size());
			// System.out.println();

			database.close();
			newDatabase.close();

			if (changeTracker.changesMadeNumChildren) {
				ArrayFile.Writer arrayWriter = new ArrayFile.Writer(
						context.getConfiguration(), lfs,
						localNumChildrenStringPath, IntWritable.class);

				for (int n = 0; n < numChildrenRemaining.length; n++) {
					IntWritable numChildren = new IntWritable(
							numChildrenRemaining[n]);
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

			if (changeTracker.changesMadeDB) {
				Path tempPath = new Path(dbStringPath + "_" + rand.nextLong());
				// use a random long to prevent collisions in the expensive copy
				// step
				lfs.pathToFile(lfs.getChecksumFile(localDBPath)).delete();

				lfs.delete(localDBPath, true);

				fs.moveFromLocalFile(newLocalDBPath, tempPath);
				// copy the written database to hdfs
				fs.rename(tempPath, hdfsDBPath);
				// rename to complete process
			} else {
				lfs.delete(localDBPath, true);
				lfs.delete(newLocalDBPath, true);
			}

		} catch (IOException e) {
			throw new Error(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private boolean decideNextRecord(Context context, long rangeStart,
			int[] numChildren, GZippedFileDatabase newDatabase,
			ChangeTracker changeTracker, Iterator<StateRecordPair> pairIter,
			long currentLongRecord) throws IOException, InterruptedException {
		boolean write = false;

		long curHash = statePair.state;

		game.hashToState(curHash, gameState);
		game.longToRecord(gameState, currentLongRecord, currentRecord);

		while (curHash == statePair.state) {
			game.longToRecord(gameState, statePair.record, candidateRecord);

			boolean possible = currentRecord.value != Value.IMPOSSIBLE;

			if (possible) {
				if (currentRecord.compareTo(candidateRecord) < 0) {
					// we found a better one!
					// if (currentRecord.value != Value.DRAW) {
					// System.out.println(candidateRecord.value.name()
					// + " is better than "
					// + currentRecord.value.name());
					// }
					currentRecord = candidateRecord.clone();
					write = true;
					changeTracker.changesMadeDB = true;
				} else if (currentRecord.value == Value.DRAW
						&& candidateRecord.value == Value.LOSE) {
					// we need to decrement numChildren!
					int n = (int) (statePair.state - rangeStart);
					numChildren[n]--;

					if (numChildren[n] == 0) {
						currentRecord = candidateRecord.clone();
						write = true;
						// all children are losses, other case couldn't be hit
						changeTracker.changesMadeDB = true;
					} else {
						// we don't need to record the range[n] == 0
						// change to num children, we
						// won't be considering the num children of this
						// position again
						changeTracker.changesMadeNumChildren = true;
					}
				}
			}

			if (pairIter.hasNext()) {
				statePair = pairIter.next();
			} else {
				break;
			}
		}

		// System.out.println("more pairs? " + pairIter.hasNext());

		if (write) {
			long longRecord = game.recordToLong(gameState, currentRecord);
			newDatabase.writeNextRecord(writeHandle, longRecord);
			context.write(new LongWritable(curHash), new LongWritable(
					longRecord));

			return true;
		}

		return false;
	}
}
