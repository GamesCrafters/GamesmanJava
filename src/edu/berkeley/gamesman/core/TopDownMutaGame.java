package edu.berkeley.gamesman.core;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.util.Pair;

public abstract class TopDownMutaGame<S> extends Game<S> {

	public TopDownMutaGame(Configuration conf) {
		super(conf);
	}

	@Override
	public String displayState(S pos) {
		setToState(pos);
		return displayState();
	}

	public abstract String displayState();

	@Override
	public S hashToState(long hash) {
		setToHash(hash);
		return getState();
	}

	public abstract void setToHash(long hash);

	public abstract S getState();

	@Override
	public PrimitiveValue primitiveValue(S pos) {
		setToState(pos);
		return primitiveValue();
	}

	public abstract PrimitiveValue primitiveValue();

	public abstract void setToState(S pos);

	@Override
	public long stateToHash(S pos) {
		setToState(pos);
		return getHash();
	}

	public abstract long getHash();

	@Override
	public String stateToString(S pos) {
		setToState(pos);
		return toString();
	}

	@Override
	public S stringToState(String pos) {
		setToState(pos);
		return getState();
	}

	public abstract void setToState(String pos);

	public abstract boolean makeMove();

	public abstract boolean changeMove();

	public abstract void undoMove();

	@Override
	public Collection<Pair<String, S>> validMoves(S pos) {
		boolean made = makeMove();
		int i = 0;
		ArrayList<Pair<String, S>> validMoves = new ArrayList<Pair<String, S>>();
		while (made) {
			validMoves.add(new Pair<String, S>(Integer.toString(i++),
					getState()));
			made = changeMove();
		}
		undoMove();
		return validMoves;
	}

	public abstract int maxMoves();

	/**
	 * @return The maximum number of children for any position
	 */
	public abstract int maxChildren();
}
