package edu.berkeley.gamesman.parallel.loopytier;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Eric
 * 
 * @param <S>
 *            the gamestate for the game we're solving
 */
public class LoopyPrimitivePassMapper<S extends State<S>> extends
		Mapper<LongWritable, IntWritable, RangeFile, LongWritable> {
	private FileSystem fs;
	private Configuration conf;
	private Game<S> game;
	private S[] childStates;
	private S position;
	private RangeFileManager<S> rangeFiles;
	private LongWritable longWritable;

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

			rangeFiles = new RangeFileManager<S>(fs, hadoopConf, game);
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

		try {
			RangeFile childFile = rangeFiles.getFile(positionHash);
			longWritable.set(positionHash);
			
			context.write(childFile, longWritable);
		} catch (IOException e) {
			throw new Error(e);
		} catch (InterruptedException e) {
			throw new Error(e);
		}
	}
}
