package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.cache.TierCache;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.TierHasher;
import edu.berkeley.gamesman.util.Pair;

/**
 * Implementation of a game which can be solved by the tier solver. The game
 * hash-space is divided into some number of tiers. A given position in any tier
 * must be guaranteed that the children are in the next tier (or some greater
 * tier)
 * 
 * @author DNSpies
 */
public abstract class TierGame extends Game<TierState> {
	private final TierHasher myHasher;
	private TierState tempState = null;

	/**
	 * Default Constructor
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public TierGame(Configuration conf) {
		super(conf);
		myHasher = new TierHasher(this);
	}

	@Override
	public final void hashToState(long hash, TierState state) {
		int tier = hashToTier(hash);
		myHasher.gameStateForTierAndOffset(tier,
				hash - hashOffsetForTier(tier), state);
	}

	/**
	 * Does a binary search of the tier offset table and returns the tier for a
	 * given position.
	 * 
	 * @param hash
	 *            A hash for a record in this game
	 * @return The tier this hash is contained in
	 */
	public final int hashToTier(long hash) {
		int tiers = numberOfTiers();
		int guess = tiers / 2, low = 0, high = tiers;
		while (high > low + 1) {
			long offset = myHasher.hashOffsetForTier(guess);
			if (offset <= hash)
				if (myHasher.hashOffsetForTier(guess + 1) > hash) {
					return guess;
				} else
					low = guess;
			else
				high = guess;
			guess = (low + high) / 2;
		}
		return low;
	}

	/**
	 * @param tier
	 *            the Tier we're interested in
	 * @return the first hash value for that tier
	 */
	public final long hashOffsetForTier(int tier) {
		return myHasher.hashOffsetForTier(tier);
	}

	/**
	 * @return The first hash value for the current tier
	 */
	public final long hashOffsetForTier() {
		return myHasher.hashOffsetForTier(getTier());
	}

	@Override
	public final long numHashes() {
		return myHasher.numHashes();
	}

	@Override
	public final synchronized Value primitiveValue(TierState pos) {
		setState(pos);
		return primitiveValue();
	}

	@Override
	public final synchronized Value strictPrimitiveValue(TierState pos) {
		setState(pos);
		return strictPrimitiveValue();
	}

	/**
	 * Assumes the given state.
	 * 
	 * @param pos
	 *            The position to assume
	 */
	public abstract void setState(TierState pos);

	/**
	 * @return The "primitive value" of the current position.
	 */
	public abstract Value primitiveValue();

	@Override
	public final synchronized Collection<Pair<String, TierState>> validMoves(
			TierState pos) {
		setState(pos);
		return validMoves();
	}

	@Override
	public final synchronized int validMoves(TierState pos, TierState[] children) {
		setState(pos);
		return validMoves(children);
	}

	/**
	 * @return The states of all the possible moves from this position.
	 */
	public abstract Collection<Pair<String, TierState>> validMoves();

	/**
	 * @return The tier of this position
	 */
	public abstract int getTier();

	@Override
	public final synchronized String stateToString(TierState pos) {
		setState(pos);
		return stateToString();
	}

	/**
	 * @return A string representation of this position.
	 */
	public abstract String stateToString();

	@Override
	public final synchronized TierState stringToState(String pos) {
		setFromString(pos);
		TierState ts = newState();
		getState(ts);
		return ts;
	}

	/**
	 * Sets this position to match the passed string
	 * 
	 * @param pos
	 *            A string representing this position
	 */
	public abstract void setFromString(String pos);

	/**
	 * Sets the passed state to this position
	 * 
	 * @param state
	 *            A state to fill with the current position
	 */
	public abstract void getState(TierState state);

	@Override
	public final synchronized String displayState(TierState pos) {
		setState(pos);
		return displayState();
	}

	/**
	 * @param tier
	 *            The tier in question
	 * @return The number of hashes in the tier
	 */
	public abstract long numHashesForTier(int tier);

	/**
	 * Pretty-print's the current position
	 * 
	 * @return a string of the position
	 */
	public abstract String displayState();

	@Override
	public final synchronized Collection<TierState> startingPositions() {
		ArrayList<TierState> positions = new ArrayList<TierState>();
		for (int i = 0; i < numStartingPositions(); i++) {
			setStartingPosition(i);
			TierState ts = newState();
			getState(ts);
			positions.add(ts);
		}
		return positions;
	}

	/**
	 * Sets this board to a starting position.
	 * 
	 * @param n
	 *            The number position to set to
	 */
	public abstract void setStartingPosition(int n);

	/**
	 * @return The number of possible starting positions.
	 */
	public abstract int numStartingPositions();

	/**
	 * @return Is there another hash for this tier?
	 */
	public abstract boolean hasNextHashInTier();

	/**
	 * Cycles to the next hash in this tier
	 */
	public abstract void nextHashInTier();

	/**
	 * Is equivalent to stateToHash where state.tier = tier and state.hash =
	 * offset
	 * 
	 * @param tier
	 *            The current tier
	 * @param offset
	 *            The offset into that tier
	 * @return The total hash
	 */
	protected final long hashForTierAndOffset(int tier, long offset) {
		return myHasher.hashOffsetForTier(tier) + offset;
	}

	@Override
	public final long stateToHash(TierState pos) {
		return hashForTierAndOffset(pos.tier, pos.hash);
	}

	/**
	 * @return The number of tiers in the game
	 */
	public abstract int numberOfTiers();

	@Override
	public abstract int maxChildren();

	/**
	 * Stores all the valid moves for this position in moves
	 * 
	 * @param moves
	 *            An array to store to
	 * @return The number of moves stored
	 */
	public abstract int validMoves(TierState[] moves);

	@Override
	public final TierState newState() {
		return new TierState();
	}

	/**
	 * Creates a new state with the given tier and hash offset
	 * 
	 * @param tier
	 *            The tier
	 * @param hash
	 *            The offset into the tier
	 * @return The TierState
	 */
	public final TierState newState(int tier, long hash) {
		return new TierState(tier, hash);
	}

	/**
	 * If over-ridden, creates a cache for a particular game and database given
	 * a certain amount of memory
	 * 
	 * @param db
	 *            The database to cache
	 * @param availableMem
	 *            The amount of memory available to use
	 * @return The cache for the tier
	 * @throws UnsupportedOperationException
	 *             If not over-ridden
	 */
	public TierCache getCache(Database db, long availableMem)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @return A separate primitive value which may be used for playing rather
	 *         than solving (if the two are not equivalent)
	 */
	public Value strictPrimitiveValue() {
		return primitiveValue();
	}

	/**
	 * Hashes a record using the current position as the game state.
	 * 
	 * @param r
	 *            The record to hash
	 * @return The hashed value
	 */
	public final long recordToLong(Record r) {
		if (tempState == null)
			tempState = newState();
		getState(tempState);
		return recordToLong(tempState, r);
	}

	/**
	 * Unhashes a record using the current position as the game state
	 * 
	 * @param longVal
	 *            The hash of the record
	 * @param r
	 *            The record to store the result in
	 */
	public final void longToRecord(long longVal, Record r) {
		if (tempState == null)
			tempState = newState();
		getState(tempState);
		longToRecord(tempState, longVal, r);
	}

	/**
	 * An implementation of validMoves in which the game can tell the cache
	 * where to find each child using the cachePlaces array
	 * 
	 * @param children
	 *            The array to store the states of the children
	 * @param cachePlaces
	 *            The array to indicate the respective cache buckets of each
	 *            child
	 * @return The number of children of the current position
	 */
	public int validMoves(TierState[] children, int[] cachePlaces) {
		throw new UnsupportedOperationException();
	}

}
