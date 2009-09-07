package edu.berkeley.gamesman.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Public interface that all Games must implement to be solvable
 * 
 * @author Steven Schlansker
 * @param <State>
 *            The object used to represent a Game State
 * 
 */
public abstract class Game<State> {

	protected final Configuration conf;

	private final ArrayList<Record> allVals = new ArrayList<Record>();

	private final ArrayList<Record> valsBest = new ArrayList<Record>();

	private final ArrayList<Record> valsBestScore = new ArrayList<Record>();

	private final ArrayList<Record> valsBestRemoteness = new ArrayList<Record>();

	@SuppressWarnings("unused")
	private Game() {
		Util.fatalError("Do not call this constructor!");
		conf = null;
	}

	/**
	 * Initialize game width/height NB: when this constructor is called, the
	 * Configuration is not required to have initialized the Hasher yet!
	 * 
	 * @param conf
	 *            configuration
	 */
	public Game(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * Generates all the valid starting positions
	 * 
	 * @return a Collection of all valid starting positions
	 */
	public abstract Collection<State> startingPositions();

	/**
	 * Given a board state, generates all valid board states one move away from
	 * the given state
	 * 
	 * @param pos
	 *            The board state to start from
	 * @return A <move,state> pair for all valid board states one move forward
	 */
	public abstract Collection<Pair<String, State>> validMoves(State pos);

	/**
	 * Applies move to pos
	 * 
	 * @param pos
	 *            The State on which to apply move
	 * @param move
	 *            A String for the move to apply to pos
	 * @return The resulting State, or null if it isn't found in validMoves()
	 */
	public State doMove(State pos, String move) {
		// TODO - better solution for games!
		// we don't want to keep the user from turning a rubik's cube if solved
		// (primitive)
		// if(!primitiveValue(pos).equals(PrimitiveValue.UNDECIDED))
		// return null;
		for (Pair<String, State> next : validMoves(pos))
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
	public int primitiveScore(State pos) {
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
	 * @return the Record representing the state
	 * @see edu.berkeley.gamesman.core.Record
	 */
	public abstract PrimitiveValue primitiveValue(State pos);

	/**
	 * @param conf
	 *            the Configuration that this game is played with
	 */
	// public abstract void initialize(Configuration conf);

	/**
	 * Unhash a given hashed value and return the corresponding Board
	 * 
	 * @param hash
	 *            The hash given
	 * @return the State represented
	 */
	public abstract State hashToState(long hash);

	/**
	 * Hash a given state into a hashed value
	 * 
	 * @param pos
	 *            The State given
	 * @return The hash that represents that State
	 */
	public abstract long stateToHash(State pos);

	/**
	 * @return the last valid hash possible in the current configuration
	 */
	public abstract long lastHash();

	/**
	 * Produce a machine-parsable String representing the state. This function
	 * must be the exact opposite of stringToState
	 * 
	 * @param pos
	 *            the State given
	 * @return a String
	 * @see Game#stringToState(String)
	 */
	public abstract String stateToString(State pos);

	/**
	 * "Pretty-print" a State for display to the user
	 * 
	 * @param pos
	 *            The state to display
	 * @return a pretty-printed string
	 */
	public abstract String displayState(State pos);

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
	public String displayHTML(State pos) {
		return displayState(pos).replaceAll("\n", "<br align=\"left\"/>");
	}

	/**
	 * Given a String construct a State. This <i>must</i> be compatible with
	 * stateToString as it is used to send states over the network.
	 * 
	 * @param pos
	 *            The String given
	 * @return a State
	 * @see Game#stateToString(Object)
	 */
	public abstract State stringToState(String pos);

	/**
	 * @return a String that uniquely describes the setup of this Game
	 *         (including any variant information, game size, etc)
	 */
	public abstract String describe();

	/**
	 * Called to notify the Game that the Configuration now has a specified and
	 * initialized Hasher. Make sure to call your superclass's method!
	 */
	public void prepare() {
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param vals
	 *            A collection of records
	 * @return The "best" record in the collection (ordered by primitive value,
	 *         then score, then remoteness)
	 */
	public Record combine(Configuration conf, List<Record> vals) {
		allVals.clear();
		for (Record r : vals)
			allVals.add(r);
		valsBest.clear();
		PrimitiveValue bestPrim = PrimitiveValue.LOSE;
		for (Record val : allVals) {
			PrimitiveValue pv = val.get();
			if (pv.isPreferableTo(bestPrim)) {
				valsBest.clear();
				valsBest.add(val);
				bestPrim = pv;
			} else if (pv.equals(bestPrim))
				valsBest.add(val);
		}
		vals = valsBest;
		if (conf.containsField(RecordFields.SCORE)) {
			valsBestScore.clear();
			long bestScore = Long.MIN_VALUE;
			for (Record val : vals) {
				long score = val.get(RecordFields.SCORE);
				if (score > bestScore) {
					valsBestScore.clear();
					valsBestScore.add(val);
					bestScore = score;
				} else if (score == bestScore)
					valsBestScore.add(val);
			}
			vals = valsBestScore;
		}
		if (conf.containsField(RecordFields.REMOTENESS)) {
			if (bestPrim.equals(PrimitiveValue.LOSE)) {
				valsBestRemoteness.clear();
				long bestRemoteness = 0;
				for (Record val : vals) {
					long remoteness = val.get(RecordFields.REMOTENESS);
					if (remoteness > bestRemoteness) {
						valsBestRemoteness.clear();
						valsBestRemoteness.add(val);
						bestRemoteness = remoteness;
					} else if (remoteness == bestRemoteness)
						valsBestRemoteness.add(val);
				}
				vals = valsBestRemoteness;
			} else {
				valsBestRemoteness.clear();
				long bestRemoteness = Long.MAX_VALUE;
				for (Record val : vals) {
					long remoteness = val.get(RecordFields.REMOTENESS);
					if (remoteness < bestRemoteness) {
						valsBestRemoteness.clear();
						valsBestRemoteness.add(val);
						bestRemoteness = remoteness;
					} else if (remoteness == bestRemoteness)
						valsBestRemoteness.add(val);
				}
				vals = valsBestRemoteness;
			}
		}
		return vals.get(0);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordArray
	 *            An array of records
	 * @param offset
	 *            The offset to start reading from
	 * @param len
	 *            The number of records to read through
	 * @return The record with the best possible outcome
	 */
	public Record combine(Configuration conf, Record[] recordArray, int offset,
			int len) {
		allVals.clear();
		for (int i = 0; i < len; i++)
			allVals.add(recordArray[offset++]);
		valsBest.clear();
		PrimitiveValue bestPrim = PrimitiveValue.LOSE;
		for (int i = 0; i < allVals.size(); i++) {
			Record val = allVals.get(i);
			PrimitiveValue pv = val.get();
			if (pv.isPreferableTo(bestPrim)) {
				valsBest.clear();
				valsBest.add(val);
				bestPrim = pv;
			} else if (pv.equals(bestPrim))
				valsBest.add(val);
		}
		ArrayList<Record> vals = valsBest;
		if (conf.containsField(RecordFields.SCORE)) {
			valsBestScore.clear();
			long bestScore = Long.MIN_VALUE;
			for (int i = 0; i < vals.size(); i++) {
				Record val = vals.get(i);
				long score = val.get(RecordFields.SCORE);
				if (score > bestScore) {
					valsBestScore.clear();
					valsBestScore.add(val);
					bestScore = score;
				} else if (score == bestScore)
					valsBestScore.add(val);
			}
			vals = valsBestScore;
		}
		if (conf.containsField(RecordFields.REMOTENESS)) {
			if (bestPrim.equals(PrimitiveValue.LOSE)) {
				valsBestRemoteness.clear();
				long bestRemoteness = 0;
				for (int i = 0; i < vals.size(); i++) {
					Record val = vals.get(i);
					long remoteness = val.get(RecordFields.REMOTENESS);
					if (remoteness > bestRemoteness) {
						valsBestRemoteness.clear();
						valsBestRemoteness.add(val);
						bestRemoteness = remoteness;
					} else if (remoteness == bestRemoteness)
						valsBestRemoteness.add(val);
				}
				vals = valsBestRemoteness;
			} else {
				valsBestRemoteness.clear();
				long bestRemoteness = Long.MAX_VALUE;
				for (int i = 0; i < vals.size(); i++) {
					Record val = vals.get(i);
					long remoteness = val.get(RecordFields.REMOTENESS);
					if (remoteness < bestRemoteness) {
						valsBestRemoteness.clear();
						valsBestRemoteness.add(val);
						bestRemoteness = remoteness;
					} else if (remoteness == bestRemoteness)
						valsBestRemoteness.add(val);
				}
				vals = valsBestRemoteness;
			}
		}
		return vals.get(0);
	}
}
