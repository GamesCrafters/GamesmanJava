package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.TierHasher;
import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies
 */
public abstract class TierGame extends Game<TierState> {
	protected TierHasher myHasher;

	public TierGame(Configuration conf) {
		super(conf);
		myHasher = new TierHasher(this);
	}

	@Override
	public void hashToState(long hash, TierState state) {
		int tier = hashToTier(hash);
		myHasher.gameStateForTierAndOffset(tier,
				hash - hashOffsetForTier(tier), state);
	}

	/**
	 * @param hash
	 *            A hash for a record in this game
	 * @return The tier this hash is contained in
	 */
	public int hashToTier(long hash) {
		int tiers = myHasher.numberOfTiers();
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

	@Override
	public long numHashes() {
		return myHasher.numHashes();
	}

	@Override
	public synchronized PrimitiveValue primitiveValue(TierState pos) {
		setState(pos);
		return primitiveValue();
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
	public abstract PrimitiveValue primitiveValue();

	@Override
	public synchronized Collection<Pair<String, TierState>> validMoves(
			TierState pos) {
		setState(pos);
		return validMoves();
	}

	@Override
	public synchronized int validMoves(TierState pos,
			TierState[] children) {
		setState(pos);
		return validMoves(children);
	}

	/**
	 * @return The states of all the possible moves from this position.
	 */
	public abstract Collection<Pair<String, TierState>> validMoves();

	/**
	 * @return Whether there is another position.
	 */
	public boolean hasNext() {
		return hasNextHashInTier() || getTier() < numberOfTiers();
	}

	/**
	 * @return The tier of this position
	 */
	public abstract int getTier();

	/**
	 * Cycle to the next position.
	 */
	public void next() {
		if (hasNextHashInTier())
			nextHashInTier();
		else
			setTier(getTier() + 1);
	}

	@Override
	public synchronized String stateToString(TierState pos) {
		setState(pos);
		return stateToString();
	}

	/**
	 * @return A string representation of this position.
	 */
	public abstract String stateToString();

	@Override
	public synchronized TierState stringToState(String pos) {
		setFromString(pos);
		return getState();
	}

	/**
	 * @param pos
	 *            A string representing this position
	 */
	public abstract void setFromString(String pos);

	/**
	 * Returns a state object for this position
	 * 
	 * @return The state of this position
	 */
	public abstract TierState getState();

	@Override
	public synchronized String displayState(TierState pos) {
		setState(pos);
		return displayState();
	}

	/**
	 * @param tier
	 *            The tier in question
	 * @return The number of hashes in the tier
	 */
	public synchronized long numHashesForTier(int tier) {
		setTier(tier);
		return numHashesForTier();
	}

	/**
	 * Sets this game to the given tier.
	 * 
	 * @param tier
	 *            The tier to set to.
	 */
	public abstract void setTier(int tier);

	/**
	 * Pretty-print's the current position
	 * 
	 * @return a string of the position
	 */
	public abstract String displayState();

	/**
	 * @return The number of hashes in this tier.
	 */
	public abstract long numHashesForTier();

	@Override
	public synchronized Collection<TierState> startingPositions() {
		ArrayList<TierState> positions = new ArrayList<TierState>();
		for (int i = 0; i < numStartingPositions(); i++) {
			setStartingPosition(i);
			positions.add(getState());
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

	@Override
	public long stateToHash(TierState pos) {
		return myHasher.hashOffsetForTier(pos.tier) + pos.hash;
	}

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
	public TierState newState() {
		return new TierState();
	}
}
