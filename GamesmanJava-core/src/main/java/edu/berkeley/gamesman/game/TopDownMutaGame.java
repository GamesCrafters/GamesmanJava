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
	 * @param conf The configuration object
	 */
	public TopDownMutaGame(Configuration conf) {
		super(conf);
	}

	// Game Logic

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

	/**
	 * @return A list of the names of the available moves from the game's own
	 * state, in the order they would be returned by validMoves
	 * @see #validMoves(HashState[])
	 */
	public abstract List<String> moveNames();

	/**
	 * Gets a collection of valid moves from the current state
	 *
	 * @return A collection of pairs of move names and states
	 */
	protected Collection<Pair<String, HashState>> validMoves() {
		List<String> moveStrings = moveNames();

		HashState[] states = new HashState[moveStrings.size()];
		for (int i = 0; i < states.length; i++) {
			states[i] = newState();
		}
		validMoves(states);

		ArrayList<Pair<String, HashState>> validMoves =
				new ArrayList<Pair<String, HashState>>(moveStrings.size());
		int i = 0;
		for (String move : moveStrings) {
			validMoves.add(new Pair<String, HashState>(move, states[i++]));
		}

		return validMoves;
	}

	/**
	 * Gets a collection of valid moves from the given state
	 *
	 * <p>Calling this method could affect the internal game state
	 *
	 * @param pos The board state to start from
	 * @return A collection of pairs of move names and states
	 */
	@Override
	public Collection<Pair<String, HashState>> validMoves(HashState pos) {
		setFromHash(pos.hash);
		return validMoves();
	}

	/**
	 * Gets the available moves from the games' own state
	 *
	 * @return The number of available moves
	 * @see #moveNames()
	 */
	private int validMoves(HashState[] children) {
		int numChildren = makeMove();
		for (int child = 0; child < numChildren; child++) {
			children[child].hash = getHash();
			changeMove();
		}
		undoMove();
		return numChildren;
	}

	/**
	 * Gets the available moves from the given state
	 *
	 * <p>Calling this method could affect the internal game state
	 *
	 * @param pos      The board state to start from
	 * @param children The array to store all valid board states one move forward
	 * @return The number of available moves
	 */
	@Override
	public int validMoves(HashState pos, HashState[] children) {
		setFromHash(pos.hash);
		return validMoves(children);
	}
}
