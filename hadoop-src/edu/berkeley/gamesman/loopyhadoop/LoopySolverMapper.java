package edu.berkeley.gamesman.loopyhadoop;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.game.Undoable;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA. User: dxu Date: 4/12/11 Time: 5:14 PM To change
 * this template use File | Settings | File Templates.
 * 
 * @param <S>
 *            the gamestate for the game we're solving
 */
public class LoopySolverMapper<S extends State> extends
		Mapper<LongWritable, LongWritable, RangeFile, StateRecordPair> {

	private FileSystem fs;
	private Configuration conf;
	private Game<S> game;
	private S[] parentStates;
	private RangeFile[] rangeFiles;
	private long hashesPerFile;
	private Record rec;
	private StateRecordPair srp;

	@SuppressWarnings("unchecked")
	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration hadoopConf = context
					.getConfiguration();
			conf = Configuration.deserialize(hadoopConf
					.get("gamesman.configuration"));
			game = conf.getCheckedGame();
			parentStates = game
					.newStateArray(((Undoable<S>) game).maxParents());
			fs = FileSystem.get(hadoopConf);
			rec = new Record(conf);
			srp = new StateRecordPair();

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
			// System.out.println("\nChild is: " + rec.value.name());
			rec.previousPosition();
			if (rec.remoteness == 0) {
				System.out.println("WTF? REMOTENESS 0 BUG?");
			}
			// System.out.println("Parent is: " + rec.value.name());
			int numParents = 0;
			numParents = ((Undoable<S>) game)
					.possibleParents(pos, parentStates);

			// System.out.println("num parents: " + numParents);
			for (int i = 0; i < numParents; i++) {
				long parentHash = game.stateToHash(parentStates[i]);
				RangeFile parentFile = rangeFiles[(int) (parentHash / hashesPerFile)];
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
