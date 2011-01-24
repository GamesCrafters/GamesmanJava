package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.Pair;

public class OneTwoTen extends Game<OTTState> {

	public OneTwoTen(Configuration conf) {
		super(conf);
	}

	@Override
	public Collection<OTTState> startingPositions() {
		return Collections.singleton(newState(0));
	}

	@Override
	public Collection<Pair<String, OTTState>> validMoves(OTTState pos) {
		ArrayList<Pair<String, OTTState>> states = new ArrayList<Pair<String, OTTState>>(
				2);
		states.add(new Pair<String, OTTState>("1", newState(pos.value + 1)));
		if (pos.value < 9)
			states.add(new Pair<String, OTTState>("2", newState(pos.value + 2)));
		return states;
	}

	@Override
	public int validMoves(OTTState pos, OTTState[] children) {
		children[0].value = pos.value + 1;
		if (pos.value < 9) {
			children[1].value = pos.value + 2;
			return 2;
		} else
			return 1;
	}

	@Override
	public int maxChildren() {
		return 2;
	}

	@Override
	public Value primitiveValue(OTTState pos) {
		return pos.value == 10 ? Value.LOSE : Value.UNDECIDED;
	}

	@Override
	public long stateToHash(OTTState pos) {
		return pos.value;
	}

	@Override
	public String stateToString(OTTState pos) {
		return String.valueOf(pos.value);
	}

	@Override
	public String displayState(OTTState pos) {
		return String.valueOf(pos.value);
	}

	@Override
	public OTTState stringToState(String pos) {
		return newState(Integer.parseInt(pos));
	}

	@Override
	public String describe() {
		return "1,2,...10";
	}

	@Override
	public long numHashes() {
		return 11;
	}

	@Override
	public long recordStates() {
		return 12;
	}

	@Override
	public void hashToState(long hash, OTTState s) {
		s.value = (int) hash;
	}

	@Override
	public OTTState newState() {
		return new OTTState();
	}

	private OTTState newState(int value) {
		return new OTTState(value);
	}

	@Override
	public void longToRecord(OTTState recordState, long record, Record toStore) {
		if (record == 11)
			toStore.value = Value.UNDECIDED;
		else {
			toStore.remoteness = (int) record;
			toStore.value = record % 2 == 0 ? Value.LOSE : Value.WIN;
		}
	}

	@Override
	public long recordToLong(OTTState recordState, Record fromRecord) {
		if (fromRecord.value == Value.UNDECIDED)
			return 11;
		else
			return fromRecord.remoteness;
	}

}

class OTTState implements State {
	int value;

	public OTTState() {
		this(0);
	}

	public OTTState(int value) {
		this.value = value;
	}

	@Override
	public void set(State s) {
		value = ((OTTState) s).value;
	}
}
