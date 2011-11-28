package edu.berkeley.gamesman.verification;

import java.util.Iterator;
import java.util.List;

import edu.berkeley.gamesman.core.Value;

/**
 * Represents a state or position of a game.
 * 
 * @author adegtiar
 * @author rchengyue
 */
public abstract class GameState implements Cloneable {

	/**
	 * Generates a list of valid moves originating from this <tt>GameState</tt>.
	 * 
	 * @param filterInvalidMoves
	 *            true if want to filter out the states with invalid columns
	 * 
	 * @return a list of valid moves.
	 */
	public abstract List<Move> generateMoves(boolean filterInvalidMoves);

	/**
	 * Checks if the <tt>GameState</tt> is a primitive state (win, lose, or
	 * tie).
	 * 
	 * @return whether or not the state is primitive.
	 */
	public abstract boolean isPrimitive();

	/**
	 * Calculates the value of a <tt>GameState</tt> (if the state is primitive).
	 * 
	 * @return the value of this state, or null if the state is not primitive;
	 */

	public abstract Value getValue();

	/**
	 * Modifies the <tt>GameState</tt> by performing the given move on it.
	 * 
	 * @param move
	 *            the move to perform.
	 */
	public abstract void doMove(Move move);

	/**
	 * Modifies the <tt>GameState</tt> by performing the undoing move on it.
	 * 
	 * @param move
	 *            the move to undo.
	 */
	public abstract void undoMove(Move move);

	/**
	 * Returns the String representation of the board.
	 * 
	 * @return the String representation of the board.
	 */
	public abstract String getBoardString();

	/**
	 * Generates the String representations of all the current
	 * <tt>GameState</tt> children states.
	 * 
	 * @return iterator of the String representations of the children states.
	 */
	public abstract Iterator<String> generateChildren();

	public abstract int getHeight();

	public abstract int getWidth();

	public abstract int getInARow();

	public abstract GameState clone();
}
