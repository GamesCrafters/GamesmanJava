package edu.berkeley.gamesman.loopyhadoop;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Eric
 * 
 * @param <S>
 *            the gamestate for the game we're solving
 */
public class LoopyPrimitivePassMapper<S extends State> extends
		Mapper<LongWritable, IntWritable, RangeFile, LongWritable> {
	private FileSystem fs;
	private Configuration conf;
	private Game<S> game;
	private S[] childStates;
	private S position;
	private RangeFile[] rangeFiles;
	private LongWritable longWritable;
	private long hashesPerFile;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration hadoopConf = context
					.getConfiguration();
			conf = Configuration.deserialize(hadoopConf
					.get("gamesman.configuration"));
			game = conf.getCheckedGame();
			childStates = game.newStateArray(game.maxChildren());
			position = game.newState();
			fs = FileSystem.get(hadoopConf);
			longWritable = new LongWritable();

			SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path(
					hadoopConf.get("db.map.path")), hadoopConf);
			ArrayList<RangeFile> ranges = new ArrayList<RangeFile>();
			while (true) {
				Range r = new Range();
				Text text = new Text();
				if (!reader.next(r, text))
					break;
				ranges.add(new RangeFile(r, text));
			}
			reader.close();
			
			Collections.sort(ranges);

			rangeFiles = ranges.toArray(new RangeFile[ranges.size()]);

			hashesPerFile = game.numHashes() / ranges.size();
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
	}

	@Override
	public void map(LongWritable positionToMap, IntWritable ignore,
			Context context) {
		long pos = positionToMap.get();

		if (pos != -1) {
			game.hashToState(pos, position);
			int numChildren = game.validMoves(position, childStates);

			for (int i = 0; i < numChildren; i++) {
				long childHash = game.stateToHash(childStates[i]);
				outputToReducer(context, childHash);
			}
		} else {
			Collection<S> startingPositions = game.startingPositions();

			for (S startPosition : startingPositions) {
				long positionHash = game.stateToHash(startPosition);
				outputToReducer(context, positionHash);
			}
		}
	}

	private void outputToReducer(Context context, long positionHash)
			throws Error {
		RangeFile childFile = rangeFiles[getChildFile(positionHash)];
		longWritable.set(positionHash);

		try {
			context.write(childFile, longWritable);
		} catch (IOException e) {
			throw new Error(e);
		} catch (InterruptedException e) {
			throw new Error(e);
		}
	}

	private int getChildFile(long positionHash) {
		int guess = Math.min((int) (positionHash / hashesPerFile), rangeFiles.length - 1);
		//initial guess at the location
		while(rangeFiles[guess].myRange.firstRecord > positionHash)
		{
			guess--;
			//we guessed too high
		}
		
		while(rangeFiles[guess].myRange.firstRecord + rangeFiles[guess].myRange.numRecords < positionHash)
		{
			guess++;
			//guessed too low
		}
		
		return guess;
	}

}
