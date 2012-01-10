package edu.berkeley.gamesman.hadoop.game.connect4;

import java.util.Collection;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hadoop.ranges.Range;
import edu.berkeley.gamesman.hadoop.ranges.RangeTree;
import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class Connect4 extends RangeTree<C4State, C4ModState> {
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

	@Override
	protected CacheMove[] getMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected GameValue getValue(C4State state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<Range<C4State, C4ModState>> getKeyClass() {
		// TODO Auto-generated method stub
		return null;
	}
}
