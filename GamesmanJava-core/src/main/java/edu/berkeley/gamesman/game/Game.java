package edu.berkeley.gamesman.game;

import java.lang.reflect.Array;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * Public interface that all Games must implement to be solvable
 *
 * @author David Spies
 * @param <S>
 *            The object used to represent a Game State
 *
 */
public abstract class Game<S extends State<S>> {

	/**
	 * @param conf
	 *            The configuration object
	 */
	public Game(Configuration conf) {
		this.conf = conf;
	}

	// Configuration

	/**
	 * The configuration object associated with this game
	 */
	protected final Configuration conf;

	/**
	 * @return Whether it makes sense to include value for this game
	 */
	public boolean hasValue() {
		return true;
	}

	/**
	 * @return Whether it makes sense to include remoteness for this game
	 */
	public boolean hasRemoteness() {
		return true;
	}

	/**
	 * @return Whether it makes sense to include score for this game
	 */
	public boolean hasScore() {
		return false;
	}

	// Game Logic

	/**
	 * Generates all the valid starting positions
	 *
	 * @return a Collection of all valid starting positions
	 */
	public abstract Collection<S> startingPositions();

	/**
	 * Synchronizes the call to startingPositions in case necessary
	 *
	 * @return A collection of all possible starting positions
	 */
	public final synchronized Collection<S> synchronizedStartingPositions() {
		return startingPositions();
	}

	/**
	 * Given a board state, generates all valid board states one move away from
	 * the given state. The String indicates in some sense what move is made to
	 * reach that position. Also override the other validMoves (to be used by
	 * the solver)
	 *
	 * @param pos
	 *            The board state to start from
	 * @return A <move,state> pair for all valid board states one move forward
	 * @see Game#validMoves(State,State[])
	 */
	public abstract Collection<Pair<String, S>> validMoves(S pos);

	/**
	 * A synchronized implementation of validMoves for JSONInterface (When
	 * solving, the game is cloned to ensure there are no synchronization
	 * problems)
	 *
	 * @param pos
	 *            The current position
	 * @return A collection of each move along with its identifying string
	 */
	public synchronized final Collection<Pair<String, S>> synchronizedValidMoves(
			S pos) {
		return validMoves(pos);
	}

	/**
	 * Valid moves without instantiation. Pass in a State array which the
	 * children will be stored in.
	 *
	 * @param pos
	 *            The board state to start from
	 * @param children
	 *            The array to store all valid board states one move forward
	 * @return The number of children for this position
	 */
	public int validMoves(S pos, S[] children) {
		int i = 0;
		for (Pair<String, S> move : validMoves(pos))
			children[i++].set(move.cdr);
		return i;
	}

	/**
	 * @return The maximum number of child states for any position
	 */
	public abstract int maxChildren();

	/**
	 * Applies move to pos
	 *
	 * @deprecated Use validMoves instead
	 * @param pos
	 *            The State on which to apply move
	 * @param move
	 *            A String for the move to apply to pos
	 * @return The resulting State, or null if it isn't found in validMoves()
	 */
	public S doMove(S pos, String move) {
		for (Pair<String, S> next : validMoves(pos))
			if (next.car.equals(move))
				return next.cdr;
		return null;
	}

	/**
	 * Given a primitive board state, return how good it is. Return 0 if any
	 * winning/losing position is equal. Otherwise, return the value of this
	 * endgame such that the best possible ending state is 0 and worse states
	 * are higher.
	 *
	 * @param pos
	 *            The primitive State
	 * @return the score of this position
	 */
	public int primitiveScore(S pos) {
		return 0;
	}

	/**
	 * A synchronized implementation of primitiveScore for JSONInterface (When
	 * solving, the game is cloned to ensure there are no synchronization
	 * problems)
	 *
	 * @param pos
	 *            The position
	 * @return The position's score
	 */
	public synchronized final int synchronizedPrimitiveScore(S pos) {
		return primitiveScore(pos);
	}

	/**
	 * @return The number of players, 2 for a game, 1 for a puzzle
	 */
	public int getPlayerCount() {
		return 2;
	}

	/**
	 * Given a board state return its primitive "value". Usually this value
	 * includes WIN, LOSE, and perhaps TIE. Return UNDECIDED if this is not a
	 * primitive state. This method should be as efficient as possible, since it
	 * will be used repeatedly by the solver. For instance, in Chess this
	 * version of primitiveValue should probably be
	 * "Has the king been captured?" rather than "Is it Checkmate?"
	 *
	 * @param pos
	 *            The primitive State
	 * @return the primitive value of the state
	 */
	public abstract Value primitiveValue(S pos);

	/**
	 * This is primitiveValue according to the letter of the rules. In other
	 * words, this should return LOSE for a checkmate position (Note that
	 * remoteness 2 from capture the king is not sufficient for a checkmate. You
	 * also must verify that the king is in check or else it's a stalemate).
	 *
	 * @param pos
	 *            The primitive State
	 * @return the primitive value of the state
	 */
	public Value strictPrimitiveValue(S pos) {
		return primitiveValue(pos);
	}

	/**
	 * @param pos
	 *            The current position
	 * @return The primitive value of the position. Generally LOSE,TIE, or
	 *         UNDECIDED (for positions which aren't primitive)
	 */
	public synchronized final Value synchronizedStrictPrimitiveValue(S pos) {
		return strictPrimitiveValue(pos);
	}

	// States

	/**
	 * @return A new empty state
	 */
	public abstract S newState();

	/**
	 * Convenience method which clones a state using its set method
	 *
	 * @param orig
	 *            The state to clone
	 * @return The new state
	 */
	@SuppressWarnings("unchecked")
	public final S newState(S orig) {
		S s = newState();
		s.set(orig);
		return s;
	}

	/**
	 * @param len
	 *            The number of states
	 * @return A new array with len states
	 */
	@SuppressWarnings("unchecked")
	public final S[] newStateArray(int len) {
		S oneState = newState();
		S[] arr = (S[]) Array.newInstance(oneState.getClass(), len);
		if (len > 0)
			arr[0] = oneState;
		for (int i = 1; i < len; i++)
			arr[i] = newState();
		return arr;
	}

	// Long State Converters

    /**
     * @return The total number of hashes
     */
    public abstract long numHashes();

	/**
	 * Hash a given state into a hashed value
	 *
	 * @param pos
	 *            The State given
	 * @return The hash that represents that State
	 */
	public abstract long stateToHash(S pos);

	/**
	 * A synchronized implementation of stateToHash for JSONInterface (When
	 * solving, the game is cloned to ensure there are no synchronization
	 * problems)
	 *
	 * @param pos
	 *            The position
	 * @return The position's hash
	 */
	public synchronized final long synchronizedStateToHash(S pos) {
		return stateToHash(pos);
	}

	/**
	 * Unhash a given hashed value and return the corresponding Board
	 *
	 * @param hash
	 *            The hash given
	 * @return the State represented
	 */
	public final S hashToState(long hash) {
		S res = newState();
		hashToState(hash, res);
		return res;
	}

	/**
	 * For mutable states. Avoids needing to instantiate new states.
	 *
	 * @param hash
	 *            The hash to use
	 * @param s
	 *            The state to store the result in
	 */
	public abstract void hashToState(long hash, S s);

	// String State Converters

	/**
	 * Produce a machine-parsable String representing the state. This function
	 * must be the exact opposite of stringToState
	 *
	 * @param pos
	 *            the State given
	 * @return a String
	 * @see Game#stringToState(String)
	 */
	public abstract String stateToString(S pos);

	/**
	 * A synchronized implementation of stateToString for JSONInterface (When
	 * solving, the game is cloned to ensure there are no synchronization
	 * problems)
	 *
	 * @param pos
	 *            The position
	 * @return A string representing the position
	 */
	public synchronized final String synchronizedStateToString(S pos) {
		return stateToString(pos);
	}

	/**
	 * Given a String construct a State. This <i>must</i> be compatible with
	 * stateToString as it is used to send states over the network.
	 *
	 * @param pos
	 *            The String given
	 * @return a State
	 * @see Game#stateToString(State)
	 */
	public abstract S stringToState(String pos);

	/**
	 * A synchronized implementation of stringToState for JSONInterface (When
	 * solving, the game is cloned to ensure there are no synchronization
	 * problems)
	 *
	 * @param pos
	 *            The position as a string
	 * @return The resulting state
	 */
	public synchronized final S synchronizedStringToState(String pos) {
		return stringToState(pos);
	}

	// Records

	/**
	 * @return The total number of possible states a record could be
	 */
	public abstract long recordStates();

	/**
	 * @return A new Record object (if you wish to subclass Record for your
	 *         game, you should over-ride this)
	 */
	public Record newRecord() {
		return new Record(conf);
	}

	/**
	 * A new array of record objects
	 *
	 * @param len
	 *            The length of the array
	 * @return A length of instantiated records with length len
	 */
	public final Record[] newRecordArray(int len) {
		Record[] arr = new Record[len];
		for (int i = 0; i < len; i++) {
			arr[i] = newRecord();
		}
		return arr;
	}

	/**
	 * Finds the "best" record in a set and returns it.
	 *
	 * @param records
	 *            An array of record objects
	 * @param firstRecord
	 *            The first element of the array to consider
	 * @param numRecords
	 *            The total number of records to consider
	 * @return The record with the best possible outcome
	 */
	public Record combine(Record[] records, int firstRecord, int numRecords) {
		Record best = records[firstRecord];
		int lastRecord = firstRecord + numRecords;
		for (int i = firstRecord + 1; i < lastRecord; i++) {
			if (records[i].compareTo(best) > 0)
				best = records[i];
		}
		return best;
	}

	/**
	 * Equivalent to combine(records, 0, records.length)
	 *
	 * @param records
	 *            An array of records
	 * @return The record with the best possible outcome
	 * @see #combine(Record[], int, int)
	 */
	public Record combine(Record[] records) {
		return combine(records, 0, records.length);
	}

	// Long Record Converters

	/**
	 * @param recordState
	 *            The state corresponding to this record
	 * @param fromRecord
	 *            The record to extract the long from
	 * @return A long representing the record (to be stored in a database)
	 */
	public abstract long recordToLong(S recordState, Record fromRecord);

	/**
	 * @param recordState
	 *            The state corresponding to this record
	 * @param record
	 *            A long representing the record
	 * @param toStore
	 *            The record to store the result in (as opposed to returning a
	 *            newly instantiated record)
	 */
	public abstract void longToRecord(S recordState, long record, Record toStore);

	/**
	 * A synchronized implementation of stateToString for JSONInterface (When
	 * solving, the game is cloned to ensure there are no synchronization
	 * problems)
	 *
	 * @param recordState
	 *            The state corresponding to this record
	 * @param record
	 *            A long representing the record (extracted from a database)
	 * @param toStore
	 *            The record to store the result in (as opposed to returning a
	 *            newly instantiated record)
	 */
	public final synchronized void synchronizedLongToRecord(S recordState,
			long record, Record toStore) {
		longToRecord(recordState, record, toStore);
	}

	// Pretty Print

	/**
	 * "Pretty-print" a State for display to the user
	 *
	 * @param pos
	 *            The state to display
	 * @return a pretty-printed string
	 */
	public abstract String displayState(S pos);

	/**
	 * "Pretty-print" a State for display by Graphviz/Dotty. See
	 * http://www.graphviz.org/Documentation.php for documentation. By default,
	 * replaces newlines with <br />
	 * . Do not use a
	 * <table>
	 * here!
	 *
	 * @param pos
	 *            The GameState to format.
	 * @return The html-like formatting of the string.
	 */
	public String displayHTML(S pos) {
		return displayState(pos).replaceAll("\n", "<br align=\"left\"/>");
	}

	/**
	 * @return a String that uniquely describes the setup of this Game
	 *         (including any variant information, game size, etc)
	 */
	public abstract String describe();

	// Pool

	public final Record getPoolRecord() {
		return recordPool.get();
	}

	public final void release(Record r) {
		recordPool.release(r);
	}

	public final S getPoolState() {
		return statePool.get();
	}

	public final void release(S state) {
		statePool.release(state);
	}

	public final S[] getPoolChildStateArray() {
		return childStateArrayPool.get();
	}

	public final void release(S[] childStateArray) {
		childStateArrayPool.release(childStateArray);
	}

	public final Record[] getPoolRecordArray() {
		return recordArrayPool.get();
	}

	public final void release(Record[] recordArray) {
		recordArrayPool.release(recordArray);
	}

	private final Pool<S> statePool = new Pool<S>(new Factory<S>() {

		@Override
		public S newObject() {
			return newState();
		}

		@Override
		public void reset(S t) {
		}

	});

	private final Pool<S[]> childStateArrayPool = new Pool<S[]>(
			new Factory<S[]>() {

				@Override
				public S[] newObject() {
					return newStateArray(maxChildren());
				}

				@Override
				public void reset(S[] t) {
				}
			});

	private final Pool<Record> recordPool = new Pool<Record>(
			new Factory<Record>() {

				@Override
				public Record newObject() {
					return newRecord();
				}

				@Override
				public void reset(Record t) {
				}

			});

	private final Pool<Record[]> recordArrayPool = new Pool<Record[]>(
			new Factory<Record[]>() {

				@Override
				public Record[] newObject() {
					return newRecordArray(maxChildren());
				}

				@Override
				public void reset(Record[] t) {
				}
			});
}
