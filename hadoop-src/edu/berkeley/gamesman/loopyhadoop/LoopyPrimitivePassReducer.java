package edu.berkeley.gamesman.loopyhadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayFile;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * @author Eric
 * 
 * @param <S>
 *            the game to performt he primitive pass reduce for
 */
public class LoopyPrimitivePassReducer<S extends State> extends
		Reducer<RangeFile, LongWritable, LongWritable, IntWritable> {
	private FileSystem fs;
	private Configuration conf;
	private Game<S> game;
	private final Random rand = new Random();
	private IntWritable zero = new IntWritable(0);
	private LongWritable hashLongWritable = new LongWritable(0);
	private LongWritable recordLongWritable = new LongWritable(0);
	private Path primitivePath;
	private S[] childStates;
	private S position;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration hadoopConf = context
					.getConfiguration();
			conf = Configuration.deserialize(hadoopConf
					.get("gamesman.configuration"));
			fs = FileSystem.get(hadoopConf);
			game = conf.getCheckedGame();
			childStates = game.newStateArray(game.maxChildren());
			position = game.newState();
			primitivePath = new Path(hadoopConf.get("primitive.output"));
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	// thank you hadoop
	@Override
	public void reduce(RangeFile rangeFile, Iterable<LongWritable> hashes,
			Context context) {
		ArrayList<Long> sortedHashes = new ArrayList<Long>();

		for (LongWritable hash : hashes) {
			sortedHashes.add(hash.get());
		}

		Collections.sort(sortedHashes);

		writeNumChildren(rangeFile, sortedHashes, context);
		markDatabase(rangeFile, context, sortedHashes);
	}

	private void writeNumChildren(RangeFile rangeFile,
			ArrayList<Long> sortedHashes, Context context) {
		try {
			long rangeStart = rangeFile.myRange.firstRecord;
			long numRecords = rangeFile.myRange.numRecords;

			Path rangeFileDBPath = new Path(rangeFile.myFile.toString());

			String numChildrenStringPath = rangeFileDBPath.toString()
					+ "_numChildren";

			Path numChildrenPath = new Path(numChildrenStringPath);

			ArrayFile.Reader arrayReader = null;
			if (fs.exists(numChildrenPath)) {
				arrayReader = new ArrayFile.Reader(fs, numChildrenStringPath,
						context.getConfiguration());
			}

			String tempStringPath = numChildrenStringPath + "_"
					+ rand.nextLong();

			ArrayFile.Writer arrayWriter = new ArrayFile.Writer(context
					.getConfiguration(), fs, tempStringPath, IntWritable.class,
					CompressionType.BLOCK, null);

			Iterator<Long> hashIter = sortedHashes.iterator();
			long nextHash = hashIter.next();

			IntWritable numChildren = new IntWritable(0);
			for (long n = 0; n < numRecords; n++) {
				numChildren.set(0);
				
				if(arrayReader != null)
				{
					arrayReader.next(numChildren);
				}
				
				if (nextHash == rangeStart + n) {// we're writing to a hash that
					// we're filling in here
					game.hashToState(nextHash, position);
					numChildren.set(game.validMoves(position, childStates));

					long prevHash = nextHash;
					while (prevHash == nextHash && hashIter.hasNext()) {
						// skip duplicates
						nextHash = hashIter.next();
					}
				}

				arrayWriter.append(numChildren);
			}

			if(arrayReader != null)
			{
				arrayReader.close();
			}
			arrayWriter.close();

			if (fs.exists(numChildrenPath))
				fs.delete(numChildrenPath, true);

			fs.rename(new Path(tempStringPath), numChildrenPath);
			// rename to complete process

		} catch (IOException io) {
			throw new Error(io);
		}
	}

	private void markDatabase(RangeFile rangeFile, Context context,
			ArrayList<Long> sortedHashes) throws Error {
		try {
			long rangeStart = rangeFile.myRange.firstRecord;
			long numRecords = rangeFile.myRange.numRecords;

			Path path = new Path(rangeFile.myFile.toString());

			LocalFileSystem lfs = FileSystem.getLocal(context
					.getConfiguration());

			String stringPath = lfs.pathToFile(path).getPath();
			String localStringPath = stringPath + "_local";
			String newLocalStringPath = localStringPath + "_new";

			Path newLocalPath = new Path(newLocalStringPath);
			Path localPath = new Path(localStringPath);
			fs.copyToLocalFile(path, localPath);

			GZippedFileDatabase database = new GZippedFileDatabase(
					localStringPath, conf, rangeStart, numRecords, true, false);
			GZippedFileDatabase newDatabase = new GZippedFileDatabase(
					newLocalStringPath, conf, rangeStart, numRecords, false,
					true);

			DatabaseHandle readHandle = database.getHandle(true);
			database.prepareReadRange(readHandle, rangeStart, numRecords);

			DatabaseHandle writeHandle = newDatabase.getHandle(false);
			newDatabase.prepareWriteRange(writeHandle, rangeStart, numRecords);

			Record record = game.newRecord();
			S gameState = game.newState();

			String primitivePathString = "range"
					+ rangeFile.myRange.firstRecord
					+ "to"
					+ (rangeFile.myRange.firstRecord
							+ rangeFile.myRange.numRecords - 1)
					+ context.getConfiguration().get("stage2_remoteness");

			Path primitiveFileWrite = new Path(primitivePath,
					primitivePathString);

			SequenceFile.Writer primitiveFileWriter = SequenceFile
					.createWriter(fs, context.getConfiguration(),
							primitiveFileWrite, LongWritable.class,
							LongWritable.class);

			/*
			 * all hashes are going to be in the same file so this goes outside
			 * the loop
			 */
			boolean changesMade = false;

			Iterator<Long> hashIter = sortedHashes.iterator();
			long nextHash = hashIter.next();

			for (long n = 0; n < numRecords; n++) {
				long recordLong = database.readNextRecord(readHandle);				
				if (rangeStart + n == nextHash) {

					game.hashToState(nextHash, gameState);

					game.longToRecord(gameState, recordLong, record);
					/* get the record associated with this hash */

					boolean visited = record.value != Value.IMPOSSIBLE;

					if (!visited) {
						hashLongWritable.set(nextHash);

						Value primitiveValue = game.primitiveValue(gameState);
						if (primitiveValue == Value.UNDECIDED) {
							// value is not primitive
							record.value = Value.DRAW;

							recordLong = game.recordToLong(gameState, record);

							context.write(hashLongWritable, zero);
							// only continue for non-primitives
						} else // primitive
						{							
							record.value = primitiveValue;
							record.remoteness = 0;

							recordLong = game.recordToLong(gameState, record);

							recordLongWritable.set(recordLong);
							primitiveFileWriter.append(hashLongWritable,
									recordLongWritable);
						}
						
						newDatabase.writeNextRecord(writeHandle, recordLong);
		
						changesMade = true;
					} else {
						newDatabase.writeNextRecord(writeHandle, recordLong);
					}

					long prevHash = nextHash;
					while (prevHash == nextHash && hashIter.hasNext()) {
						// skip duplicates
						nextHash = hashIter.next();
					}
				} else {
					newDatabase.writeNextRecord(writeHandle, recordLong);
					// writes to a gzipped database have to be sequential so
					// copy in the gap
				}
			}

			primitiveFileWriter.close();
			
			database.close();
			newDatabase.close();

			if (changesMade) {
				Path tempPath = new Path(stringPath + "_" + rand.nextLong());
				// use a random long to prevent collisions in the expensive copy
				// step
				lfs.delete(localPath, true);

				fs.moveFromLocalFile(newLocalPath, tempPath);
				// copy the written database to hdfs
				fs.rename(tempPath, path);
				// rename to complete process
			} else {
				lfs.delete(localPath, true);
				lfs.delete(newLocalPath, true);
			}
		} catch (IOException e) {
			throw new Error(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
