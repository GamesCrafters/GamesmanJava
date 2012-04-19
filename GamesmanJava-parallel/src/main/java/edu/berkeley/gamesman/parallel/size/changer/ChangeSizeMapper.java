package edu.berkeley.gamesman.parallel.size.changer;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.parallel.ranges.ChildMap;
import edu.berkeley.gamesman.parallel.ranges.MainRecords;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.parallel.ranges.Suffix;
import edu.berkeley.gamesman.parallel.writable.WritableTreeMap;
import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.writable.FixedLengthWritable;

public class ChangeSizeMapper<S extends GenState, GR extends FixedLengthWritable>
		extends Mapper<Suffix<S>, MainRecords<GR>, Suffix<S>, MainRecords<GR>> {

	private RangeTree<S, GR> tree;
	private int newSuffLen;
	private int newVarLen;
	private Suffix<S> suffix;
	private int[] sufArr;
	private S pos;
	private MainRecords<GR> newValue;

	@Override
	public void setup(Context context) {
		Configuration conf = context.getConfiguration();
		newVarLen = conf.getInt("new.var.length", -1);
		if (newVarLen == -1)
			throw new RuntimeException("No var len set");
		tree = (RangeTree<S, GR>) ConfParser
				.<Suffix<S>, MainRecords<GR>, ChildMap, WritableTreeMap<GR>, WritableTreeMap<GR>, ChildMap> newTree(conf);
		newSuffLen = tree.getHasher().numElements - newVarLen;
		sufArr = new int[newSuffLen];
		suffix = new Suffix<S>(sufArr);
		pos = tree.getHasher().newState();
		newValue = new MainRecords<GR>(conf);
	}

	@Override
	public void map(Suffix<S> key, MainRecords<GR> value, Context context)
			throws IOException, InterruptedException {
		GenHasher<S> hasher = tree.getHasher();

		key.firstPosition(hasher, pos);
		int curPlace = 0;

		while (curPlace < key.length()) {
			suffix.set(pos, newSuffLen);
			long longStepSize = hasher.step(pos, 1, newVarLen);
			if (longStepSize > Integer.MAX_VALUE)
				throw new RuntimeException("Too large: " + longStepSize);
			int stepSize = (int) longStepSize;
			newValue.setCopyOfRange(value, curPlace, stepSize);
			context.write(suffix, newValue);
			curPlace += stepSize;
		}

		if (curPlace != key.length())
			throw new RuntimeException("Sizes are wrong");
	}

}
