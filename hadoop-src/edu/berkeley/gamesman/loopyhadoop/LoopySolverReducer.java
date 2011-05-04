package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayFile;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
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
	private HadoopDBModifier dbModifier;

	private LongWritable positionOutput = new LongWritable();
	private LongWritable recordOutput = new LongWritable();

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

			Collections.sort(sortedCandidates);

			String numChildrenStringPath = rangeToReduce.myFile.toString() + "_numChildren";
			String numChildrenTempStringPath = numChildrenStringPath + "_"
					+ rand.nextLong();

			Path hdfsNumChildrenPath = new Path(numChildrenStringPath);
			Path hdfsNumChildrenTempPath = new Path(numChildrenTempStringPath);

			ArrayFile.Reader numChildrenReader = new ArrayFile.Reader(fs,
					numChildrenStringPath, context.getConfiguration());

			ArrayFile.Writer numChildrenWriter = new ArrayFile.Writer(context
					.getConfiguration(), fs, numChildrenTempStringPath,
					IntWritable.class, CompressionType.BLOCK, null);

			long rangeStart = rangeToReduce.myRange.firstRecord;
			long numRecords = rangeToReduce.myRange.numRecords;
			
			dbModifier = new HadoopDBModifier(rangeToReduce, FileSystem.getLocal(context
					.getConfiguration()), fs, conf);

			// File stuff prepared, move on to interesting part

			Iterator<StateRecordPair> pairIter = sortedCandidates.iterator();
			statePair = pairIter.next();

			IntWritable numChildren = new IntWritable();

			for (long i = 0; i < numRecords; i++) {
				long currentLongRecord = dbModifier.readNextRecord();

				boolean written = false;

				numChildrenReader.next(numChildren);

				if (rangeStart + i == statePair.state) {
					written = decideNextRecord(context, numChildren, pairIter, currentLongRecord);
				}

				numChildrenWriter.append(numChildren);

				if (!written) {
					// write the old record
					dbModifier.writeNextRecord(currentLongRecord);
				}
			}

			// Cleanup file stuff
			numChildrenReader.close();
			numChildrenWriter.close();

			fs.delete(hdfsNumChildrenPath, true);
			fs.rename(hdfsNumChildrenTempPath, hdfsNumChildrenPath);
			// get the numChildren file right

			dbModifier.closeAndClean();
		} catch (IOException e) {
			throw new Error(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private boolean decideNextRecord(Context context, IntWritable numChildren,
			Iterator<StateRecordPair> pairIter, long currentLongRecord)
			throws IOException, InterruptedException {
		boolean write = false;

		long curHash = statePair.state;

		game.hashToState(curHash, gameState);
		game.longToRecord(gameState, currentLongRecord, currentRecord);

		boolean possible = currentRecord.value != Value.IMPOSSIBLE;
		boolean primitive = game.primitiveValue(gameState) != Value.UNDECIDED;

		while (curHash == statePair.state) {
			game.longToRecord(gameState, statePair.record, candidateRecord);

			if (possible && !primitive) {
				if (currentRecord.compareTo(candidateRecord) < 0) {
					// we found a better one!
					currentRecord.set(candidateRecord);
					write = true;
				} else if (currentRecord.value == Value.DRAW
						&& candidateRecord.value == Value.LOSE) {
					// we need to decrement numChildren!
					numChildren.set(numChildren.get() - 1);

					if (numChildren.get() == 0) {
						currentRecord.set(candidateRecord);
						write = true;
						// all children are losses, other case couldn't be hit
					}
				}
			}

			if (pairIter.hasNext()) {
				statePair = pairIter.next();
			} else {
				break;
			}
		}

		if (write) {
			long longRecord = game.recordToLong(gameState, currentRecord);
			dbModifier.writeNextRecord(longRecord);
			positionOutput.set(curHash);
			recordOutput.set(longRecord);
			context.write(positionOutput, recordOutput);

			return true;
		}

		return false;
	}
}
