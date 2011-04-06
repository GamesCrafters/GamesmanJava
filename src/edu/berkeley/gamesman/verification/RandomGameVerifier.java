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
			String database, String outputFileName, int totalStateCount,
			int totalTimeCount) {
		super(stateClass, database, outputFileName, totalStateCount,
				totalTimeCount);
	}

	@Override
	public boolean hasNext() {
		if (progressBarType == ProgressBarType.STATE
				&& this.stateCount == this.totalStateCount
				|| progressBarType == ProgressBarType.TIME
				&& System.currentTimeMillis() / 1000 - initialTime >= totalTimeCount)
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
