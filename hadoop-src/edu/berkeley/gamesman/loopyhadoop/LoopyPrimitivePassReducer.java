package edu.berkeley.gamesman.loopyhadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

public class LoopyPrimitivePassReducer<S extends State> extends
		Reducer<RangeFile, LongWritable, LongWritable, IntWritable> {
	private FileSystem fs;
	private Configuration conf;
	private Game<S> game;
	private DataInput dataIn;
	private DataOutput dataOut;

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
		try {
			for (LongWritable hash : hashes) {
				rangeFile.readFields(dataIn);
				boolean visited = false;// TODO: assign this correctly

				if (!visited) {
					Value primitiveValue = game.primitiveValue(game
							.hashToState(hash.get()));
					switch (primitiveValue) {
					case UNDECIDED:
						// value is not primitive
						// TODO: handle undecided
						break;
					case WIN:
					case LOSE:
					case TIE:
						// do the same for all these cases, all primitive
						// TODO: handle primitive
						break;
					default:
						throw new Error("WTF did primitive value return?");
					}
				}
			}
		} catch (IOException e) {
			throw new Error(e);
		}
	}
}
