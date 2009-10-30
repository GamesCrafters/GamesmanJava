package edu.berkeley.gamesman.core;

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
 * 
 * @param <State>
 *            The type that you use to represent your States
 */
public abstract class TieredGame<State> extends Game<State> {
	protected TieredHasher<State> myHasher;

	/**
	 * Default constructor
	 * 
	 * @param conf
	 *            the configuration
	 */
	public TieredGame(Configuration conf) {
		super(conf);
	}

	@Override
	public void prepare() {
		myHasher = Util.checkedCast(conf.getHasher());
	}

	@Override
	public State hashToState(long hash) {
		if (myHasher == null)
			Util.fatalError("You must call prepare() before hashing!");
		int tiers = myHasher.numberOfTiers();
		int guess = tiers / 2, low = 0, high = tiers;
		while (high > low + 1) {
			long offset = myHasher.hashOffsetForTier(guess);
			if (offset <= hash)
				if (myHasher.hashOffsetForTier(guess + 1) > hash)
					return myHasher.gameStateForTierAndOffset(guess, hash
							- offset);
				else
					low = guess;
			else
				high = guess;
			guess = (low + high) / 2;
		}
		return myHasher.gameStateForTierAndOffset(low, hash
				- myHasher.hashOffsetForTier(low));
	}

	@Override
	public long stateToHash(State pos) {
		if (myHasher == null)
			Util.fatalError("You must call prepare() before hashing!");
		Pair<Integer, Long> p = myHasher.tierIndexForState(pos);
		return myHasher.hashOffsetForTier(p.car) + p.cdr;
	}

	/**
	 * @return the number of Tiers in this particular game
	 */
	public int numberOfTiers() {
		if (myHasher == null)
			Util.fatalError("You must call prepare() before hashing!");
		return myHasher.numberOfTiers();
	}

	/**
	 * @param tier
	 *            the Tier we're interested in
	 * @return the first hash value for that tier
	 */
	public final long hashOffsetForTier(int tier) {
		if (myHasher == null)
			Util.fatalError("You must call prepare() before hashing!");
		return myHasher.hashOffsetForTier(tier);
	}

	@Override
	public long numHashes() {
		return myHasher.numHashes();
	}
}
