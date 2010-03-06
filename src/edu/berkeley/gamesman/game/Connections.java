package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;

/**
 * The game Connections
 * 
 * @author dnspies
 */
public class Connections extends ConnectGame {

	private int boardSide;
	private int boardSize;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public void initialize(Configuration conf) {
		super.initialize(conf);
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
