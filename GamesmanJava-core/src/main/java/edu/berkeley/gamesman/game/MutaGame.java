package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;

/**
 * This is the super class for all games which contain their own state
 *
 * <p>Note that the states are not passed into methods as in {@link Game}.
 *
 * @author dnspies
 */
public abstract class MutaGame extends Game<HashState> {
	/**
	 * The default constructor
	 *
	 * @param conf The configuration object
	 */
	public MutaGame(Configuration conf) {
		super(conf);
	}

	// Game Logic

	/**
	 * @return The number of starting positions for this game
	 */
	public abstract int numStartingPositions();

	@Override
	public Collection<HashState> startingPositions() {
		int numStartingPositions = numStartingPositions();
		ArrayList<HashState> startingPositions = new ArrayList<HashState>(
				numStartingPositions);
		for (int i = 0; i < numStartingPositions; i++) {
			setStartingPosition(i);
			HashState thisState = newState(getHash());
			startingPositions.add(thisState);
		}
		return startingPositions;
	}

	/**
	 * Sets the internal state to the ith starting position
	 *
	 * @param i The starting position to set to
	 */
	public abstract void setStartingPosition(int i);

	/**
	 * Returns the primitive value of the given position
	 *
	 * @return The primitive value of the current position
	 */
	public abstract Value primitiveValue();

	/**
	 * Returns the primitive value of the given position
	 *
	 * <p>Calling this method could affect the internal game state
	 *
	 * @param pos The primitive State
	 * @return The primitive value of the given position
	 */
	@Override
	public Value primitiveValue(HashState pos) {
		setFromHash(pos.hash);
		return primitiveValue();
	}

	/**
	 * Returns the actual primitive value according to the rules for the current
	 * state
	 *
	 * @return The value of the current state
	 */
	public Value strictPrimitiveValue() {
		return primitiveValue();
	}

	/**
	 * Returns the actual primitive value according to the rules for the given state
	 *
	 * <p>Calling this method could affect the internal game state
	 *
	 * @param pos The primitive State
	 * @return The value of the given state
	 */
	@Override
	public Value strictPrimitiveValue(HashState pos) {
		setFromHash(pos.hash);
		return strictPrimitiveValue();
	}

	// States

	@Override
	public final HashState newState() {
		return new HashState();
	}

	protected final HashState newState(long hash) {
		return new HashState(hash);
	}

	// Long State Converters

	@Override
	public void hashToState(long hash, HashState state) {
		state.hash = hash;
	}

	@Override
	public long stateToHash(HashState pos) {
		return pos.hash;
	}

	/**
	 * @return The hash of the current position
	 */
	public abstract long getHash();

	/**
	 * Sets the board position to the passed hash
	 *
	 * @param hash The hash to match
	 */
	public abstract void setFromHash(long hash);

	// String State Converters

	@Override
	public String stateToString(HashState pos) {
		setFromHash(pos.hash);
		return toString();
	}

	/**
	 * Convert the string to a state object
	 *
	 * <p>Calling this method could affect the internal game state
	 *
	 * @param pos The String given
	 * @return New state from the given string
	 */
	@Override
	public HashState stringToState(String pos) {
		setFromString(pos);
		return newState(getHash());
	}

	/**
	 * Sets the board to the position passed in string form
	 *
	 * @param pos The position to set to
	 */
	public abstract void setFromString(String pos);

	// Records

	/**
	 * Hashes a record using the current position as the game state
	 *
	 * <p>Calling this method could affect the internal game state
	 *
	 * @param recordState The state corresponding to this record
	 * @param fromRecord  The record to extract the long from
	 * @return The hashed value
	 */
	@Override
	public long recordToLong(HashState recordState, Record fromRecord) {
		setFromHash(recordState.hash);
		return recordToLong(fromRecord);
	}

	/**
	 * Hashes a record using the current position as the game state
	 *
	 * @param fromRecord The record to hash
	 * @return The hashed value
	 */
	public abstract long recordToLong(Record fromRecord);

	@Override
	public void longToRecord(HashState recordState, long record, Record toStore) {
		setFromHash(recordState.hash);
		longToRecord(record, toStore);
	}

	/**
	 * Unhashes a record using the current position as the game state
	 *
	 * @param record  The hash of the record
	 * @param toStore The record to store the result in
	 */
	public abstract void longToRecord(long record, Record toStore);

	// Pretty Print

	@Override
	public String displayState(HashState pos) {
		setFromHash(pos.hash);
		return displayState();
	}

	/**
	 * "Pretty-print" the current State for display to the user
	 *
	 * @return a pretty-printed string
	 */
	public abstract String displayState();
}
