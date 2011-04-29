package edu.berkeley.gamesman.verification;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Verifies the game tree by randomly executing moves.
 * 
 * @author adegtiar
 * @author rchengyue
 */
public class RandomGameVerifier extends GameVerifier {

	private int mCurrentDepth;
	private int nFirstMovesChecked;
	private boolean useProbSolving = Connect4CmdLineParser.probabilistic;
	
	private void checkedState() {
		if (mCurrentDepth < 2)
			nFirstMovesChecked++;
	}

	/**
	 * @param stateClass
	 * @param database
	 * @param outputFileName
	 * @param totalStateCount
	 * @param totalTimeCount
	 */
	public RandomGameVerifier(Class<? extends GameState> stateClass,
			String database, String outputFileName, int totalStateCount,
			int totalTimeCount) {
		super(stateClass, database, outputFileName, totalStateCount,
				totalTimeCount);
	}

	@Override
	public boolean hasNext() {
		boolean hasNext = super.hasNext();
		if (!hasNext && Connect4CmdLineParser.debugging) {
			System.out.println("\nNumber of states skipped: " + nSkipped);
			System.out.println("Number of times first two tiers checked: " + nFirstMovesChecked);
		}
		return hasNext;
	}

	@Override
	public GameState next() {
		if (currentGameState == null) {
			currentGameState = this.getInitialGameState();
			return currentGameState;
		}

		GameState toReturn = currentGameState;
		if (this.currentGameState.isPrimitive()) {
			currentGameState = this.getInitialGameState();
			runCount++;
			mCurrentDepth = 0;
		} else {
			List<Move> currentMoves = currentGameState.generateMoves();
			currentGameState.doMove(currentMoves.get(new Random()
					.nextInt(currentMoves.size())));
			mCurrentDepth++;
		}
		return toReturn;
	}

	@Override
	protected boolean verifyGameState() throws IOException {
		if (!useProbSolving) {
			checkedState();
			return super.verifyGameState();
		}
		else {
			double p_verified = getProbabilityStateVerified();
			if (Math.random() > p_verified) {
				checkedState();
				return super.verifyGameState();
			}
			else {
				nSkipped++;
				return true;
			}
		}
	}

	/**
	 * Calculates the approximated probability that the current state has
	 * already been verified. This is just:
	 * P	= 1 - P(not verified by run n)
	 * 	  	= 1 - P(not verified on any run)^n
	 * 		= 1 - (1 - P(verified on any run))^n
	 * 		= 1 - (1 - (1 / width)^depth)^n
	 * 
	 * @return the approximate probability that the current state has already
	 *         been verified.
	 */
	private double getProbabilityStateVerified() {
		double p_verified_on_any_run = 1 / Math.pow(
				getApproximateBranchingFactor(), mCurrentDepth);
		return 1 - Math.pow(1 - p_verified_on_any_run, runCount);
	}
	
	private double getApproximateBranchingFactor() {
		return currentGameState.getWidth();
	}

}
