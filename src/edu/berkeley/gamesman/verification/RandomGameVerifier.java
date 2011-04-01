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

	public RandomGameVerifier(Class<? extends GameState> stateClass,
			String database, File out, int stateTotalCount) {
		super(stateClass, database, out, stateTotalCount);
	}

	@Override
	public boolean hasNext() {
		if (this.stateCount == this.totalStateCount)
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

	@Override
	public void printStatusBar() {
		progressBar.updateNumElements(stateCount);
		progressBar.printStatus();
		if (stateCount == totalStateCount) {
			progressBar.finish();
		}
	}
}
