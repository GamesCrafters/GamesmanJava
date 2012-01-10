package edu.berkeley.gamesman.hadoop.game.connect4;

import java.util.Collection;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hadoop.ranges.GenKey;
import edu.berkeley.gamesman.hadoop.ranges.Range;
import edu.berkeley.gamesman.hadoop.ranges.RangeTree;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.Move;

public class Connect4 extends RangeTree<C4State> {

	@Override
	protected int suffixLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected Collection<GenKey<C4State>> getStartingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected GameValue getValue(C4State state) {
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
	protected Move[] getMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<Range<C4State>> getKeyClass() {
		// TODO Auto-generated method stub
		return null;
	}

}
