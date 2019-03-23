package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.Pair;

/**
 * The game One Two Ten. Implemented as a demonstration on 9/11/11
 * 
 * @author dnspies
 */
public class OneTwoTen extends Game<OneTwoTenState> {

	/**
	 * Default constructor
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public OneTwoTen(Configuration conf) {
		super(conf);
	}

	@Override
	public Collection<OneTwoTenState> startingPositions() {
		OneTwoTenState s = new OneTwoTenState();
		return Collections.singleton(s);
	}

	@Override
	public Collection<Pair<String, OneTwoTenState>> validMoves(
			OneTwoTenState pos) {
		if (pos.numXs == 0)
			return Collections.emptySet();
		else {
			ArrayList<Pair<String, OneTwoTenState>> moves =
					new ArrayList<Pair<String, OneTwoTenState>>(2);
			OneTwoTenState newState = newState();
			newState.set(pos);
			newState.makeMove(1);
			moves.add(new Pair<String, OneTwoTenState>("1", newState));
			if (pos.numXs >= 2) {
				newState = newState();
				newState.set(pos);
				newState.makeMove(2);
				moves.add(new Pair<String, OneTwoTenState>("2", newState));
			}
			return moves;
		}
	}

	@Override
	public int maxChildren() {
		return 2;
	}

	@Override
	public Value primitiveValue(OneTwoTenState pos) {
		if (pos.numXs == 0)
			return Value.LOSE;
		else
			return Value.UNDECIDED;
	}

	@Override
	public long stateToHash(OneTwoTenState pos) {
		return pos.numXs + (pos.turn ? 11 : 0);
	}

	@Override
	public String stateToString(OneTwoTenState pos) {
		return (pos.turn == OneTwoTenState.LEFT ? "L" : "R") + pos.numXs;
	}

	@Override
	public String displayState(OneTwoTenState pos) {
		StringBuilder sb = new StringBuilder(10);
		sb.append(pos.turn == OneTwoTenState.LEFT ? "L" : "R").append("\t");
		for (int i = 0; i < pos.numXs; i++)
			sb.append('X');
		return sb.toString();
	}

	@Override
	public OneTwoTenState stringToState(String pos) {
		OneTwoTenState s = newState();
		char firstChar = pos.charAt(0);
		if (firstChar == 'L')
			s.turn = OneTwoTenState.LEFT;
		else if (firstChar == 'R')
			s.turn = OneTwoTenState.RIGHT;
		else
			throw new Error("Bad state: " + pos);
		s.numXs = Integer.parseInt(pos.substring(1));
		if (s.numXs > 10 || s.numXs < 0)
			throw new Error("Bad state: " + pos);
		return s;
	}

	@Override
	public String describe() {
		return "One Two Ten";
	}

	@Override
	public long numHashes() {
		return 22;
	}

	@Override
	public long recordStates() {
		return 12;
	}

	@Override
	public void hashToState(long hash, OneTwoTenState s) {
		if (hash < 11) {
			s.turn = false;
			if (hash < 0)
				throw new Error("Hash out of bounds");
			s.numXs = (int) hash;
		} else {
			s.turn = true;
			if (hash >= 22)
				throw new Error("Hash out of bounds");
			s.numXs = (int) (hash - 11);
		}
	}

	@Override
	public OneTwoTenState newState() {
		return new OneTwoTenState();
	}

	@Override
	public void longToRecord(OneTwoTenState recordState, long record,
			Record toStore) {
		if (record < 0 || record > 11) {
			throw new Error("Record hash out of bounds " + record);
		}
		if (record == 11)
			toStore.value = Value.UNDECIDED;
		else {
			toStore.value = (record % 2 == 1) ? Value.WIN : Value.LOSE;
			toStore.remoteness = (int) record;
		}
	}

	@Override
	public long recordToLong(OneTwoTenState recordState, Record fromRecord) {
		if (fromRecord.value == Value.UNDECIDED)
			return 11;
		else if (fromRecord.remoteness < 0 || fromRecord.remoteness > 10)
			throw new Error("Remoteness out of bounds " + fromRecord);
		else if (fromRecord.value != ((fromRecord.remoteness % 2 == 1) ? Value.WIN
				: Value.LOSE))
			throw new Error("Value does not match remoteness " + fromRecord);
		return fromRecord.remoteness;
	}

}

/**
 * A game state for One Two Ten
 * 
 * @author dnspies
 */
class OneTwoTenState implements State<OneTwoTenState> {
	/**
	 * Left player constant
	 */
	static final boolean LEFT = false;
	/**
	 * Right player constant
	 */
	static final boolean RIGHT = true;
	/**
	 * The player whose turn it is
	 */
	boolean turn;
	/**
	 * The number of Xs currently on the board
	 */
	int numXs;

	/**
	 * Initializes to starting position
	 */
	public OneTwoTenState() {
		turn = LEFT;
		numXs = 10;
	}

	@Override
	public void set(OneTwoTenState other) {
		turn = other.turn;
		numXs = other.numXs;
	}

	/**
	 * Makes a move
	 * 
	 * @param n
	 *            The amount to remove
	 */
	public void makeMove(int n) {
		if (numXs > 0 && (n == 1 || (numXs >= 2 && n == 2))) {
			numXs -= n;
			turn = !turn;
		} else
			throw new Error("Can only remove one or two, not " + n);
	}
}