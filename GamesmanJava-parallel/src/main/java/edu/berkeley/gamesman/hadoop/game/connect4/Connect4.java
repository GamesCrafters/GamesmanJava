package edu.berkeley.gamesman.hadoop.game.connect4;

import java.util.Collection;

import edu.berkeley.gamesman.hadoop.ranges.Range;
import edu.berkeley.gamesman.hadoop.ranges.RangeRecords;
import edu.berkeley.gamesman.hadoop.ranges.RangeTree;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.propogater.writable.list.WritableArray;

public class Connect4 extends RangeTree<C4State, C4ModState> {

	@Override
	protected Range<C4ModState> newRange() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Collection<C4ModState> getStartingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int getVarianceLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected GenHasher<C4State> getHasher() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int suffixLength() {
		// TODO Auto-generated method stub
		return 0;
	}
}
