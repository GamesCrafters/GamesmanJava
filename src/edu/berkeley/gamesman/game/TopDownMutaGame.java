package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Pair;

/**
 * This is the super class for all top-down mutable games
 * 
 * @author dnspies
 * 
 * @param <S>
 *            The state for this game
 */
public abstract class TopDownMutaGame<S extends State> extends Game<S> {

	public TopDownMutaGame(Configuration conf) {
		super(conf);
	}

	@Override
	public String displayState(S pos) {
		setToState(pos);
		return displayState();
	}

	/**
	 * "Pretty-print" the current State for display to the user
	 * 
	 * @return a pretty-printed string
	 */
	public abstract String displayState();

	@Override
	public void hashToState(long hash, S state) {
		setToHash(hash);
		state.set(getState());
	}

	/**
	 * Sets the board position to the passed hash
	 * 
	 * @param hash
	 *            The hash to match
	 */
	public abstract void setToHash(long hash);

	/**
	 * @return The current state of the game
	 */
	public abstract S getState();

	@Override
	public Value primitiveValue(S pos) {
		setToState(pos);
		return primitiveValue();
	}

	/**
	 * @return The primitive value of the current position
	 */
	public abstract Value primitiveValue();

	/**
	 * Sets the board to the passed state
	 * 
	 * @param pos
	 *            A state to set
	 */
	public abstract void setToState(S pos);

	@Override
	public long stateToHash(S pos) {
		setToState(pos);
		return getHash();
	}

	/**
	 * @return The hash of the current position
	 */
	public abstract long getHash();

	@Override
	public String stateToString(S pos) {
		setToState(pos);
		return toString();
	}

	@Override
	public S stringToState(String pos) {
		setFromString(pos);
		return getState();
	}

	/**
	 * Sets the board to the position passed in string form
	 * 
	 * @param pos
	 *            The position to set to
	 */
	public abstract void setFromString(String pos);

	/**
	 * Makes a move on the board. The possible moves are ordered such that this
	 * will always be the move made when makeMove() is called
	 * 
	 * @return Whether there are any possible moves to be made
	 */
	public abstract boolean makeMove(S curState);

	/**
	 * Changes the last move made to the next possible move in the list
	 * 
	 * @return If there are any more moves to be tried
	 */
	public abstract boolean changeMove(S curState);

	/**
	 * Undoes the last move made
	 */
	public abstract void undoMove(S curState);

	@Override
	public Collection<Pair<String, S>> validMoves(S pos) {
		setToState(pos);
		return validMoves();
	}

	/**
	 * @return All the possible moves for the current position
	 */
	public Collection<Pair<String, S>> validMoves() {
		boolean made = makeMove(null);
		int i = 0;
		ArrayList<Pair<String, S>> validMoves = new ArrayList<Pair<String, S>>();
		while (made) {
			validMoves.add(new Pair<String, S>(Integer.toString(i++),
					getState()));
			made = changeMove(null);
		}
		undoMove(null);
		return validMoves;
	}

	public int validMoves(S pos, S[] childArray) {
		boolean made = makeMove(null);
		int i;
		for (i = 0; made; i++) {
			childArray[i] = getState();
			made = changeMove(null);
		}
		undoMove(null);
		return i;
	}
}
