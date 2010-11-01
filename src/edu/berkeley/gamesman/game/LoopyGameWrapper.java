package edu.berkeley.gamesman.game;

import java.util.Collection;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.qll.RecycleLinkedList;

import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.RLLFactory;

public class LoopyGameWrapper extends LoopyMutaGame {
//public final class LoopyGameWrapper<S extends State> extends LoopyMutaGame{
//	private final Game<S> myGame;
//	private final RecycleLinkedList<RecycleLinkedList<S>> moveLists;
//	private final RecycleLinkedList<S> stateList;
//	private final S[] possibleMoves;
//	private final S[] startingPositions;

	public LoopyGameWrapper(Configuration conf, LoopyGame g) {
		super(conf);
		if (!(g instanceof Game<?>)) {
			throw new Error("Can only wrap games");
		}
		// TODO Auto-generated constructor stub
	}

	@Override
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

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
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
		setToState(startingPositions[i]);

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
		//return null;
		return myGame.describe();
	}

	@Override
	public int maxChildren() {
		// TODO Auto-generated method stub
		//return 0;
		return myGame.maxChildren();
	}

	@Override
	public long numHashes() {
		// TODO Auto-generated method stub
		//return 0;
		return myGame.numHashes();
	}

	@Override
	public long recordStates() {
		// TODO Auto-generated method stub
		//return 0;
		return myGame.recordStates();
	}

}
