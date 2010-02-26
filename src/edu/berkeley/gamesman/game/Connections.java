package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;

public class Connections extends ConnectGame {

	private final int boardSide;
	private final int boardSize;

	public Connections(Configuration conf) {
		super(conf);
		boardSide = conf.getInteger("game.sideLength", 4);
		boardSize = boardSide * (boardSide - 1) * 2;
	}

	@Override
	protected int getBoardSize() {
		return boardSize;
	}

	@Override
	protected char[] getCharArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setToCharArray(char[] myPieces) {
		// TODO Auto-generated method stub

	}

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isWin(char c) {
		// TODO Auto-generated method stub
		return false;
	}
}
