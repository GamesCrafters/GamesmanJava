package edu.berkeley.gamesman.loopyhadoop;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.Undoable;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: dxu Date: 4/12/11 Time: 5:14 PM To change
 * this template use File | Settings | File Templates.
 * 
 * @param <S>
 *            the gamestate for the game we're solving
 */
public class LoopySolverMapper<S extends State<S>> extends
		Mapper<LongWritable, LongWritable, RangeFile, StateRecordPair> {

	private FileSystem fs;
	private Configuration conf;
	private Game<S> game;
	private S[] parentStates;
	private RangeFileManager<S> rangeFiles;
	private Record rec;
	private StateRecordPair srp;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration hadoopConf = context
					.getConfiguration();
			conf = Configuration.deserialize(hadoopConf
					.get("gamesman.configuration"));
			game = conf.getCheckedGame();
			if (game instanceof Undoable<?>) {
				parentStates = game.newStateArray(((Undoable<?>) game)
						.maxParents());
			}
			fs = FileSystem.get(hadoopConf);
			rec = new Record(conf);
			srp = new StateRecordPair();

			rangeFiles = new RangeFileManager<S>(fs, hadoopConf, game);
		} catch (IOException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (ClassCastException e) {
			throw new Error("Game is not Undoable");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void map(LongWritable positionToMap, LongWritable record,
			Context context) {
		try {
			S pos = game.hashToState(positionToMap.get());
			game.longToRecord(pos, record.get(), rec);
			rec.previousPosition();

			int numParents = 0;
			numParents = ((Undoable<S>) game)
					.possibleParents(pos, parentStates);

			for (int i = 0; i < numParents; i++) {
				long parentHash = game.stateToHash(parentStates[i]);
				RangeFile parentFile = rangeFiles.getFile(parentHash);

				srp.state = parentHash;
				srp.record = game.recordToLong(parentStates[i], rec);

				context.write(parentFile, new StateRecordPair(srp.state,
						srp.record));
			}
		} catch (IOException e) {
			throw new Error(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
