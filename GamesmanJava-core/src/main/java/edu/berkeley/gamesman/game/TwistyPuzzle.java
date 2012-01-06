package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;

/**
 * A superclass for twisty puzzles
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The state for the puzzle
 */
public abstract class TwistyPuzzle<S extends State<S>> extends Game<S>
		implements FinitePrimitives<S>, Undoable<S> {

	public TwistyPuzzle(Configuration conf) {
		super(conf);
	}

	@Override
	public int getPlayerCount() {
		return 1;
	}

	@Override
	public long recordStates() {
		return remotenessStates() + 1;
	}

	public abstract int remotenessStates();

	@Override
	public long recordToLong(S recordState, Record fromRecord) {
		if (fromRecord.value == Value.UNDECIDED)
			return 0;
		else
			return fromRecord.remoteness + 1;
	}

	@Override
	public void longToRecord(S recordState, long record, Record toStore) {
		if (record == 0)
			toStore.value = Value.UNDECIDED;
		else {
			toStore.value = Value.WIN;
			toStore.remoteness = (int) (record - 1);
		}
	}

	@Override
	public Collection<S> getPrimitives() {
		return startingPositions();
	}

	@Override
	public int possibleParents(S pos, S[] parents) {
		return validMoves(pos, parents);
	}

	@Override
	public int maxParents() {
		return maxChildren();
	}

	@Override
	public boolean hasValue() {
		return false;
	}
}
