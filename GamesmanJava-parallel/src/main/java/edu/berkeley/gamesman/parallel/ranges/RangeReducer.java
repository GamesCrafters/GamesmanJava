package edu.berkeley.gamesman.parallel.ranges;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.propogater.common.ConfParser;

public class RangeReducer<S extends GenState> extends
		Reducer<Suffix<S>, MainRecords, Suffix<S>, MainRecords> {
	private GenHasher<S> hasher;
	private S state;
	private final Suffix<S> innerRange = new Suffix<S>();
	private final MainRecords recs = new MainRecords();
	private int innerSufLen;
	private int innerVarLen;

	@Override
	protected void setup(Context context) {
		Configuration conf = context.getConfiguration();
		RangeTree<S> tree = (RangeTree<S>) ConfParser
				.<Suffix<S>, MainRecords, ChildMap, RecordMap, RecordMap, ChildMap> newTree(conf);
		hasher = tree.getHasher();
		state = hasher.newState();
		innerSufLen = tree.outputSuffixLength();
		innerVarLen = hasher.numElements - innerSufLen;
	}

	@Override
	protected void reduce(Suffix<S> key, Iterable<MainRecords> values,
			Context context) throws IOException, InterruptedException {
		if (key.length() > innerSufLen)
			throw new RuntimeException("The inner suffix length: "
					+ innerSufLen + " must be longer than the working one: "
					+ key.length());
		Iterator<MainRecords> iter = values.iterator();
		MainRecords value = iter.next();
		if (iter.hasNext())
			throw new RuntimeException("Should only be one value per key, but "
					+ key + " has more than one");
		if (!value.isEmpty()) {
			int lastStart = 0;
			key.firstPosition(hasher, state);
			while (lastStart < value.length()) {
				innerRange.set(state, innerSufLen);
				recs.clear();
				long numPositionsLong = hasher.step(state, 1, innerVarLen);
				if (numPositionsLong > Integer.MAX_VALUE)
					throw new RuntimeException("Too large for int");
				int numPositions = (int) numPositionsLong;
				int newStart = lastStart + numPositions;
				for (int i = lastStart; i < newStart; i++) {
					recs.add().set(value.get(i));
				}
				context.write(innerRange, recs);
				lastStart = newStart;
			}
			assert lastStart == value.length();
		}
	}
}
