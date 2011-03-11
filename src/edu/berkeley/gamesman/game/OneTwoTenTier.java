package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Pair;

public class OneTwoTenTier extends TierGame {
	int value;

	public OneTwoTenTier(Configuration conf) {
		super(conf);
	}

	@Override
	public void setState(TierState pos) {
		value = pos.tier;
	}

	@Override
	public Value primitiveValue() {
		return value == 10 ? Value.LOSE : Value.UNDECIDED;
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		ArrayList<Pair<String, TierState>> moves = new ArrayList<Pair<String, TierState>>(
				2);
		moves.add(new Pair<String, TierState>("1", newState(value + 1)));
		if (value < 9)
			moves.add(new Pair<String, TierState>("2", newState(value + 2)));
		return moves;
	}

	private TierState newState(int value) {
		TierState ts = newState();
		ts.tier = value;
		ts.hash = 0L;
		return ts;
	}

	@Override
	public int getTier() {
		return value;
	}

	@Override
	public String stateToString() {
		return String.valueOf(value);
	}

	@Override
	public void setFromString(String pos) {
		value = Integer.parseInt(pos);
	}

	@Override
	public void getState(TierState state) {
		state.tier = value;
		state.hash = 0L;
	}

	@Override
	public long numHashesForTier(int tier) {
		return 1L;
	}

	@Override
	public String displayState() {
		return String.valueOf(value);
	}

	@Override
	public void setStartingPosition(int n) {
		value = 0;
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public boolean hasNextHashInTier() {
		return false;
	}

	@Override
	public void nextHashInTier() {
		throw new RuntimeException("One hash per tier");
	}

	@Override
	public int numberOfTiers() {
		return 11;
	}

	@Override
	public int maxChildren() {
		return 2;
	}

	@Override
	public int validMoves(TierState[] moves) {
		moves[0].tier = value + 1;
		moves[0].hash = 0L;
		if (value < 9) {
			moves[1].tier = value + 2;
			moves[1].hash = 0L;
			return 2;
		} else
			return 1;
	}

	@Override
	public String describe() {
		return "1,2,...10";
	}

	@Override
	public long recordStates() {
		return 11;
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		toStore.remoteness = (int) record;
		toStore.value = record % 2 == 0 ? Value.LOSE : Value.WIN;
	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		return fromRecord.remoteness;
	}

}
