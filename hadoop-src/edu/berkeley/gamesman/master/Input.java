package edu.berkeley.gamesman.master;

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;

import edu.berkeley.gamesman.game.TierGame;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: Nov 30, 2010
 * Time: 10:03:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class Input implements InputFormat<LongWritable, LongWritable> {
    public class Reader implements RecordReader<LongWritable, LongWritable> {
	private Split mySplit;
	private boolean read = false;

	public Reader(Split s) {
		mySplit = s;
	}

	public void close() throws IOException {
	}

	public LongWritable createKey() {
		return new LongWritable();
	}

	public LongWritable createValue() {
		return new LongWritable();
	}

	public long getPos() throws IOException {
		if (read)
			return mySplit.getLength();
		else
			return 0L;
	}

	public float getProgress() throws IOException {
		if (read)
			return 1F;
		else
			return 0F;
	}
	//First is the starting hash and num is the number of hashes to read
	public boolean next(LongWritable first, LongWritable num)
			throws IOException {
		if (read)
			return false;
		first.set(mySplit.first);
		num.set(mySplit.num);
		read = true;
		return true;
	}
    }

    public InputSplit[] getSplits(JobConf conf, int splits) throws IOException {
	int tier= conf.getInt("tier", -1);
	if (tier < 0)
		throw new Error("No tier specified");
	int numMachines = 8; //= conf.getInteger(“numMachines”, 8); 	//Default of 8
	TierGame game = null; //= conf.getGame();
	long numPos = game.numHashesForTier(tier);
	splits = numMachines;
	InputSplit[] is = new InputSplit[splits];
	long First = game.hashOffsetForTier(tier);
	long increment = (int) (((float) numPos) / numMachines);
	for (int i = 0; i < splits; i++) {
		long next = First + increment;   		//Not sure if hashes work this way
		is[i] = new Split(First, increment);
		First = next;
	}
	return is;
    }

    public RecordReader<LongWritable, LongWritable> getRecordReader(
		InputSplit is, JobConf conf, Reporter reporter) throws IOException {
	Reader rr = new Reader((Split) is);
	return rr;
}

}
