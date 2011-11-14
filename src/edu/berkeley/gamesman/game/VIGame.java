package edu.berkeley.gamesman.game;

import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.cache.TierCache;
import edu.berkeley.gamesman.database.cache.VICache;
import edu.berkeley.gamesman.util.Pair;

public abstract class VIGame extends MutaGame {
	public VIGame(Configuration conf) {
		super(conf);
	}

	private long[] children = null;

	@Override
	public int validMoves(HashState pos, HashState[] children) {
		setToHash(pos.hash);
		if (this.children == null)
			this.children = new long[maxChildren()];
		int numChildren = validMoves(this.children);
		for (int i = 0; i < numChildren; i++) {
			children[i].hash = this.children[i];
		}
		return numChildren;
	}

	/**
	 * @param childHashes
	 * @return
	 */
	public abstract int validMoves(long[] childHashes);

	/**
	 * @param childHashes
	 * @param whichChildren
	 * @return
	 * @throws UnsupportedOperationException
	 */
	public int validMoves(long[] childHashes, int[] whichChildren)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Pair<String, HashState>> validMoves(HashState pos) {
		setToHash(pos.hash);
		return validMoves();
	}

	public abstract Collection<Pair<String, HashState>> validMoves();

	public abstract boolean next();

	public VICache getCache(Database db, long availableMem)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}
