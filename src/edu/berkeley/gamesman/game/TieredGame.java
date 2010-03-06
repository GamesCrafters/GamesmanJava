package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.hasher.TieredHasher;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Superclass of Tiered games. Each game state falls into a logical tier. As an
 * example, you can represent TicTacToe as a tiered game with the tier being the
 * number of pieces placed.
 * 
 * The important invariant is that any board's value must depend only on (a)
 * primitives or (b) boards in a later tier. This allows us to solve from the
 * last tier up to the top at tier 0 (the starting state) in a very efficient
 * manner
 * 
 * @author Steven Schlansker
 * @param <S>
 *            The type that you use to represent your States
 */
public abstract class TieredGame<S extends State> extends Game<S> {
	protected TieredHasher<S> myHasher;

	@Override
	public void initialize(Configuration conf) {
		super.initialize(conf);
		myHasher = Util.checkedCast(conf.getHasher());
	}

	@Override
	public void hashToState(long hash, S state) {
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

	@Override
	public long stateToHash(S pos) {
		Pair<Integer, Long> p = myHasher.tierIndexForState(pos);
		return myHasher.hashOffsetForTier(p.car) + p.cdr;
	}

	/**
	 * @return the number of Tiers in this particular game
	 */
	public int numberOfTiers() {
		return myHasher.numberOfTiers();
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
}
