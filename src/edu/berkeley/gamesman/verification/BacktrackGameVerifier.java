package edu.berkeley.gamesman.verification;

import java.io.File;
import java.util.List;
import java.util.Stack;

/**
 * Does an in-order traversal of the entire game tree. Efficiently generates new
 * states by making and undo moves instead of instantiating new states.
 * 
 * Note: do not call any <tt>GameVerifier</tt> methods between
 * <tt>hasNext()</tt> and <tt>next()</tt>.
 * 
 * @author adegtiar
 */
public class BacktrackGameVerifier extends GameVerifier {
	/**
	 * A stack of previous move lists up to the current state. The first of a
	 * list is the previous move. Anything after that is a sibling move that
	 * could be made instead.
	 */
	private Stack<List<Move>> previousMoves;

	/**
	 * The stored result of <tt>hasNext()</tt>.
	 */
	private boolean mHasNext;

	/**
	 * Whether or not <tt>hasNext()</tt> has been called (and its result stored)
	 * for the current state.
	 */
	private boolean mChecked;

	public BacktrackGameVerifier(GameState gameState, String database, File out) {
		super(gameState, database, out);
		previousMoves = new Stack<List<Move>>();
	}

	@Override
	public boolean hasNext() {
		if (mChecked)
			return mHasNext;

		mHasNext = false;
		if (currentGameState == null) {
			currentGameState = getInitialGameState();
			mHasNext = true;
		} else {
			List<Move> availableMoves = currentGameState.generateMoves();
	
			if (!availableMoves.isEmpty()) {
				// We can continue down the tree. Add the child moves and do one.
				previousMoves.add(availableMoves);
				currentGameState.doMove(availableMoves.get(0));
				mHasNext = true;
			} else {
				// We are at a leaf.
				while (!previousMoves.isEmpty()) {
					// Backtrack until we can move to a new state.
					availableMoves = previousMoves.peek();
	
					// Undo previous move
					currentGameState.undoMove(availableMoves.remove(0));
	
					// Find another move to do...
					if (!availableMoves.isEmpty()) {
						// Found one. Do it and leave it on top
						currentGameState.doMove(availableMoves.get(0));
						mHasNext = true;
						break;
					} else {
						// No more child moves.
						previousMoves.pop();
					}
				}
			}
		}
		
		mChecked = true;
		return mHasNext;
	}

	@Override
	public GameState next() {
		if (!mChecked)
			throw new IllegalStateException(
					"Called next() without called hasNext()");
		mChecked = false;

		return currentGameState;
	}

}