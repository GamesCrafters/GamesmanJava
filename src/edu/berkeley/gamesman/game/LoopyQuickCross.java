package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.hasher.ChangedIterator;
import edu.berkeley.gamesman.hasher.DartboardHasher;

public class LoopyQuickCross extends LoopyMutaGame {
	private final int width;
	private final int height;
	private final int boardSize;
	private final int piecesToWin;
	private char[][] Board;
	private String whoseMove;
	private ArrayList<String> undoMoveHistory = new ArrayList<String>();	
	
	public LoopyQuickCross(Configuration conf) {
		super(conf);
		width = conf.getInteger("gamesman.game.width", 4);
		height = conf.getInteger("gamesman.game.height", 4);
		boardSize = width * height;
		piecesToWin = conf.getInteger("gamesman.game.pieces", 4);
		Board = new char[width][height];
		whoseMove = "Player 1";
	}

	@Override
	/* Opposite of makeMove(), aka making a move but from bottom to top. */
	public boolean changeUnmakeMove() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void remakeMove() {
		// TODO Auto-generated method stub

	}

	@Override
	public int unmakeMove() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean changeMove() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public String displayState(HashState pos) {
		setToHash(pos.hash);
		return displayState();
	}

	@Override
	public String displayState() {
		return null;	
	}

	@Override
	public long getHash() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void longToRecord(long record, Record toStore) {
		// TODO Auto-generated method stub

	}

	@Override
	public int makeMove() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Collection<String> moveNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int numStartingPositions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Value primitiveValue() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long recordToLong(Record fromRecord) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFromString(String pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStartingPosition(int i) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setToHash(long hash) {
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
	public int maxChildren() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long numHashes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long recordStates() {
		// TODO Auto-generated method stub
		return 0;
	}


}
