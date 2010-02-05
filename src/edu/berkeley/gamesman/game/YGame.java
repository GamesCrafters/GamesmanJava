package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.*;
import edu.berkeley.gamesman.util.Pair;

public class YGame extends TieredIterGame {

	public YGame(Configuration conf) {
		super(conf);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String displayState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ItergameState getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTier() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasNextHashInTier() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int maxChildren() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void nextHashInTier() {
		// TODO Auto-generated method stub

	}

	@Override
	public long numHashesForTier() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long numHashesForTier(int tier) {
		//TODO Write method
		return 0L;
	}
	
	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public int numberOfTiers() {
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
	public void setStartingPosition(int n) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setState(ItergameState pos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTier(int tier) {
		// TODO Auto-generated method stub

	}

	@Override
	public String stateToString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, ItergameState>> validMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int validMoves(ItergameState[] moves) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String describe() {
		return "Y";
	}

}
