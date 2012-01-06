package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Pair;

/**
 * This is the super class for all games which contain their own state, but
 * should be solved via the top-down solver
 * 
 * @author dnspies
 */
public abstract class TopDownMutaGame extends MutaGame {

	/**
	 * The default constructor
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public TopDownMutaGame(Configuration conf) {
		super(conf);
	}

	/**
	 * Makes a move on the board. The possible moves are ordered such that this
	 * will always be the move made when makeMove() is called
	 * 
	 * @return The number of available moves
	 */
	public abstract int makeMove();

	/**
	 * Changes the last move made to the next possible move in the list
	 * 
	 * @return If there are any more moves to be tried
	 */
	public abstract boolean changeMove();

	/**
	 * Undoes the last move made
	 */
	public abstract void undoMove();

	@Override
	public Collection<Pair<String, HashState>> validMoves(HashState pos) {
		setToHash(pos.hash);
		return validMoves();
	}

	private Collection<Pair<String, HashState>> validMoves() {
		List<String> moveStrings = moveNames();
		HashState[] states = new HashState[moveStrings.size()];
		for (int i = 0; i < states.length; i++) {
			states[i] = newState();
		}
		validMoves(states);
		ArrayList<Pair<String, HashState>> validMoves = new ArrayList<Pair<String, HashState>>(
				moveStrings.size());
		int i = 0;
		for (String move : moveStrings) {
			validMoves.add(new Pair<String, HashState>(move, states[i++]));
		}
		return validMoves;
	}

	/**
	 * @return A list of the names of the available moves, in the order they
	 *         would be returned by validMoves
	 */
	public abstract List<String> moveNames();

	@Override
	public int validMoves(HashState pos, HashState[] children) {
		setToHash(pos.hash);
		return validMoves(children);
	}

	private int validMoves(HashState[] children) {
		int numChildren = makeMove();
		for (int child = 0; child < numChildren; child++) {
			children[child].hash = getHash();
			changeMove();
		}
		undoMove();
		return numChildren;
	}
}
