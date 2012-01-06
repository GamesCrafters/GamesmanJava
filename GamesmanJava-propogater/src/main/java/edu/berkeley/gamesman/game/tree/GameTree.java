package edu.berkeley.gamesman.game.tree;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.writable.WritableSettableComparable;

public abstract class GameTree<STATE extends WritableSettableComparable<STATE>>
		extends Tree<STATE, GameRecord> {

	@Override
	public final boolean getInitialValue(STATE position, GameRecord toFill) {
		GameValue primitiveValue = getPrimitiveValue(position);
		if (primitiveValue == null) {
			toFill.set(initialNonPrimitivePosition(position));
			return true;
		} else {
			toFill.set(primitiveValue, 0);
			return false;
		}
	}

	protected GameRecord initialNonPrimitivePosition(STATE position) {
		return GameRecord.DRAW;
	}

	protected abstract GameValue getPrimitiveValue(STATE position);

	@Override
	public final void travelUp(GameRecord tVal, STATE child, STATE parent,
			GameRecord toFill) {
		toFill.previousPosition(tVal);
	}

	@Override
	public final Class<GameRecord> getValClass() {
		return GameRecord.class;
	}
}
