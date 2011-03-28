package edu.berkeley.gamesman.verification;

import java.io.File;
import java.util.List;
import java.util.Random;

/**
 * Verifies the game tree by randomly executing moves. 
 * 
 * @author adegtiar
 * @author rchengyue
 */
public class RandomGameVerifier extends GameVerifier {

	/**
	 * The number of <tt>GameState</tt> to verify.
	 */
	private final int totalStateCount = 10000;

	public RandomGameVerifier(GameState gameState,
			String database, File out) {
		super(gameState, database, out);
	}

	@Override
	public boolean hasNext() {
		if (this.stateCount == totalStateCount)
			return false;
		return true;
	}

	@Override
	public GameState next() {
		if (currentGameState == null) {
			this.stateCount++;
			currentGameState = this.getInitialGameState();
			return currentGameState;
		}
		
		GameState toReturn = currentGameState;
		if (this.currentGameState.isPrimitive()) {
			currentGameState = this.getInitialGameState();
		} else {
			List<Move> currentMoves = currentGameState.generateMoves();
			currentGameState.doMove(currentMoves.get(new Random()
					.nextInt(currentMoves.size())));
		}
		this.stateCount++;
		return toReturn;
	}
}
