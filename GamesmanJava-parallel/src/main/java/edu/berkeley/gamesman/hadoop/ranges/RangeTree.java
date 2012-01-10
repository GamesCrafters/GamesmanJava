package edu.berkeley.gamesman.hadoop.ranges;

import java.util.Collection;
import java.util.HashSet;

import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.GenState;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.list.WritableArray;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public abstract class RangeTree<S extends GenState, T extends GenKey<S, T>>
		extends Tree<Range<T>, RangeRecords> {
	@Override
	public Collection<Range<T>> getRoots() {
		Collection<T> startingPositions = getStartingPositions();
		HashSet<Range<T>> containingRanges = new HashSet<Range<T>>();
		for (T t : startingPositions) {
			Range<T> containingRange = makeContainingRange(t);
			containingRanges.add(containingRange);
		}
		return containingRanges;
	}

	private Range<T> makeContainingRange(T t) {
		Range<T> range = newRange();
		makeContainingRange(t, range);
		return range;
	}

	private void makeContainingRange(T t, Range<T> range) {
		range.setSuffix(t, suffixLength());
	}

	protected abstract int suffixLength();

	protected abstract Range<T> newRange();

	protected abstract Collection<T> getStartingPositions();

	@Override
	public void getChildren(Range<T> position, WritableList<Range<T>> toFill) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getInitialValue(Range<T> position, RangeRecords toFill) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void travelUp(RangeRecords tVal, Range<T> child, Range<T> parent,
			RangeRecords toFill) {
		// TODO Auto-generated method stub

	}

	@Override
	public Class<Range<T>> getKeyClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<RangeRecords> getValClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean combine(WritableArray<RangeRecords> children,
			RangeRecords toFill) {
		// TODO Auto-generated method stub
		return false;
	}

	protected abstract int getVarianceLength();

	protected abstract GenHasher<S> getHasher();
}
