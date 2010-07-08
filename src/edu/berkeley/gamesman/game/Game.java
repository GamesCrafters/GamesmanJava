package edu.berkeley.gamesman.game;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.Pair;

/**
 * Public interface that all Games must implement to be solvable
 * 
 * @author Steven Schlansker
 * @param <S>
 *            The object used to represent a Game State
 * 
 */
public abstract class Game<S extends State> {

	protected final Configuration conf;

	private Record[] valsBest = new Record[1];

	private Record[] valsBestScore = new Record[1];

	private Record[] valsBestRemoteness = new Record[1];

	/**
	 * @param conf
	 *            The configuratino object
	 */
	public Game(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * Generates all the valid starting positions
	 * 
	 * @return a Collection of all valid starting positions
	 */
	public abstract Collection<S> startingPositions();

	/**
	 * Given a board state, generates all valid board states one move away from
	 * the given state. It is <b>very strongly recommended</b> that you also
	 * override validMoves(S pos, S[] children) as this will result in a
	 * significant speedup for pretty much any solver
	 * 
	 * @param pos
	 *            The board state to start from
	 * @return A <move,state> pair for all valid board states one move forward
	 * @see Game#validMoves(State,State[])
	 */
	public abstract Collection<Pair<String, S>> validMoves(S pos);

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
	public abstract int validMoves(S pos, S[] children);

	/**
	 * @return The maximum number of child states for any position
	 */
	public abstract int maxChildren();

	/**
	 * Applies move to pos
	 * 
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
	 * @return The number of players, 2 for a game, 1 for a puzzle
	 */
	public int getPlayerCount() {
		return 2;
	}

	/**
	 * Given a board state return its primitive "value". Usually this value
	 * includes WIN, LOSE, and perhaps TIE Return UNDECIDED if this is not a
	 * primitive state
	 * 
	 * @param pos
	 *            The primitive State
	 * @return the primitive value of the state
	 */
	public abstract Value primitiveValue(S pos);

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
	 * Hash a given state into a hashed value
	 * 
	 * @param pos
	 *            The State given
	 * @return The hash that represents that State
	 */
	public abstract long stateToHash(S pos);

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
	 * @return a String that uniquely describes the setup of this Game
	 *         (including any variant information, game size, etc)
	 */
	public abstract String describe();

	/**
	 * @param recordArray
	 *            An array of records
	 * @param offset
	 *            The offset to start reading from
	 * @param len
	 *            The number of records to read through
	 * @return The record with the best possible outcome
	 */
	public final Record combine(Record[] recordArray, int offset, int len) {
		int size = len;
		int lastSize;
		Record[] arrVals = recordArray;
		Value bestVal = Value.WIN;
		if (conf.valueStates > 0) {
			lastSize = size;
			size = 0;
			bestVal = Value.LOSE;
			for (int i = 0; i < lastSize; i++) {
				Value value = arrVals[i + offset].value;
				if (value.isPreferableTo(bestVal)) {
					size = 1;
					valsBest[0] = arrVals[i + offset];
					bestVal = value;
				} else if (value.equals(bestVal)) {
					if (valsBest.length <= size) {
						Record[] temp = valsBest;
						valsBest = new Record[size + 1];
						for (int c = 0; c < temp.length; c++)
							valsBest[c] = temp[c];
					}
					valsBest[size++] = arrVals[i + offset];
				}
			}
			arrVals = valsBest;
			offset = 0;
		}
		if (conf.scoreStates > 0) {
			lastSize = size;
			size = 0;
			int bestScore = Integer.MIN_VALUE;
			for (int i = 0; i < lastSize; i++) {
				int score = arrVals[i + offset].score;
				if (score > bestScore) {
					size = 1;
					valsBestScore[0] = arrVals[i + offset];
					bestScore = score;
				} else if (score == bestScore) {
					if (valsBestScore.length <= size) {
						Record[] temp = valsBestScore;
						valsBestScore = new Record[size + 1];
						for (int c = 0; c < temp.length; c++)
							valsBestScore[c] = temp[c];
					}
					valsBestScore[size++] = arrVals[i + offset];
				}
			}
			arrVals = valsBestScore;
			offset = 0;
		}
		if (conf.remotenessStates > 0) {
			lastSize = size;
			size = 0;
			if (bestVal == Value.LOSE) {
				int bestRemoteness = 0;
				for (int i = 0; i < lastSize; i++) {
					int remoteness = arrVals[i + offset].remoteness;
					if (remoteness > bestRemoteness) {
						size = 1;
						valsBestRemoteness[0] = arrVals[i + offset];
						bestRemoteness = remoteness;
					} else if (remoteness == bestRemoteness) {
						if (valsBestRemoteness.length <= size) {
							Record[] temp = valsBestRemoteness;
							valsBestRemoteness = new Record[size + 1];
							for (int c = 0; c < temp.length; c++)
								valsBestRemoteness[c] = temp[c];
						}
						valsBestRemoteness[size++] = arrVals[i + offset];
					}
				}
			} else {
				int bestRemoteness = Integer.MAX_VALUE;
				for (int i = 0; i < lastSize; i++) {
					int remoteness = arrVals[i].remoteness;
					if (remoteness < bestRemoteness) {
						size = 1;
						valsBestRemoteness[0] = arrVals[i];
						bestRemoteness = remoteness;
					} else if (remoteness == bestRemoteness) {
						if (valsBestRemoteness.length <= size) {
							Record[] temp = valsBestRemoteness;
							valsBestRemoteness = new Record[size + 1];
							for (int c = 0; c < temp.length; c++)
								valsBestRemoteness[c] = temp[c];
						}
						valsBestRemoteness[size++] = arrVals[i];
					}
				}
			}
			arrVals = valsBestRemoteness;
		}
		return arrVals[0];
	}

	/**
	 * @return The total number of hashes
	 */
	public abstract long numHashes();

	/**
	 * @return The total number of possible states a record could be
	 */
	public long recordStates() {
		return (conf.valueStates > 0 ? conf.valueStates : 1)
				* (conf.remotenessStates > 0 ? conf.remotenessStates : 1)
				* (conf.scoreStates > 0 ? conf.scoreStates : 1);
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

	/**
	 * @return A new empty state
	 */
	public abstract S newState();

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

	/**
	 * @param recs
	 *            A list of records
	 * @return The best of the records
	 * @see Game#combine(Record[], int, int)
	 */
	public final Record combine(List<Record> recs) {
		Record[] recArray = new Record[recs.size()];
		recArray = recs.toArray(recArray);
		return combine(recArray, 0, recArray.length);
	}

	public int defaultValueStates() {
		return 6;
	}

	public int defaultRemotenessStates() {
		return 64;
	}

	public int defaultScoreStates() {
		return 0;
	}

	public void recordFromLong(S recordState, long record, Record toStore) {
		int fieldStates = 1;
		if (conf.valueStates > 0) {
			fieldStates = conf.valueStates;
			toStore.value = Value.values[(int) (record % fieldStates)];
		}
		if (conf.remotenessStates > 0) {
			record /= fieldStates;
			fieldStates = conf.remotenessStates;
			toStore.remoteness = (int) (record % fieldStates);
		}
		if (conf.scoreStates > 0) {
			record /= fieldStates;
			toStore.score = (int) record;
		}
	}

	public long getRecord(S recordState, Record fromRecord) {
		int fieldStates = 1;
		long totalState = 0;
		if (conf.scoreStates > 0) {
			totalState += fromRecord.score;
			fieldStates = conf.scoreStates;
		}
		if (conf.remotenessStates > 0) {
			totalState *= fieldStates;
			totalState += fromRecord.remoteness;
			fieldStates = conf.remotenessStates;
		}
		if (conf.valueStates > 0) {
			totalState *= fieldStates;
			totalState += fromRecord.value.value;
		}
		return totalState;
	}
}
