package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.TopDownMutaGame;

public class GunGame extends TopDownMutaGame<GunGameState> {
	private class Space {
		final int index;

		Space(int n) {
			index = n;
		}
	}

	private final GunGameState myState;
	private final int gameWidth, gameHeight;
	private final int piecesToWin;
	private final int boardSize;
	private final Space[][] board;

	public GunGame(Configuration conf) {
		super(conf);
		gameWidth = conf.getInteger("gamesman.game.width", 7);
		gameHeight = conf.getInteger("gamesman.game.height", 6);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 5);
		boardSize = gameWidth * gameHeight - 12;
		myState = newState();
		board = new Space[gameWidth][gameHeight];
		int n = 0;
		for (int col = 0; col < gameWidth; col++)
			for (int row = 0; row < gameHeight; row++) {
				if (Math.min(col, gameWidth - (col + 1))
						+ Math.min(row, gameHeight - (row + 1)) > 1)
					board[row][col] = new Space(n++);
			}
	}

	@Override
	public boolean changeMove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getHash() {
		return (myState.spaceHash << myState.numPieces) + myState.colorHash;
	}

	@Override
	public GunGameState getState() {
		return myState;
	}

	@Override
	public boolean makeMove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int maxChildren() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public PrimitiveValue primitiveValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFromString(String pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setToHash(long hash) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setToState(GunGameState pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void undoMove() {
		// TODO Auto-generated method stub

	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GunGameState newState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GunGameState[] newStateArray(int len) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long numHashes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<GunGameState> startingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int maxRemoteness() {
		// TODO Auto-generated method stub
		return 0;
	}

}

class GunGameState implements State {
	int numPieces;
	long spaceHash;
	long colorHash;

	public void set(State s) {
		GunGameState ggs = (GunGameState) s;
		numPieces = ggs.numPieces;
		spaceHash = ggs.spaceHash;
		colorHash = ggs.colorHash;
	}
}