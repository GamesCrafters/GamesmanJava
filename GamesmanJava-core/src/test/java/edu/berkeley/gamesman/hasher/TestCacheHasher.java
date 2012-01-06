package edu.berkeley.gamesman.hasher;

import junit.framework.Assert;

import org.junit.Test;

import edu.berkeley.gamesman.hasher.DBHasher;
import edu.berkeley.gamesman.hasher.cachehasher.CacheHasher;
import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.hasher.fixed.FixedState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class TestCacheHasher {
	@Test
	public void steppingAndHashing() {
		GenHasher.enableToughAsserts();
		CacheHasher<FixedState> dh = createHasher(false);
		FixedState state1 = dh.newState();
		Assert.assertEquals(1260L, dh.numHashes());
		CacheHasher<FixedState> dh2 = createHasher(false);
		FixedState state2 = dh2.newState();
		long lastHash = -1L;
		dh.unhash(0);
		dh2.unhash(0);
		do {
			arbitraryReset(dh);
			long hash = dh.getHash();
			Assert.assertEquals(lastHash + 1, hash);
			lastHash = hash;
			dh2.unhash(hash);
			dh.getState(state1);
			dh2.getState(state2);
			Assert.assertEquals(state1, state2);
			Assert.assertEquals(hash, dh2.getHash());
		} while (dh.next() != -1);
		Assert.assertEquals(1259L, lastHash);
	}

	private CacheHasher<FixedState> createHasher(boolean next) {
		DBHasher mainHasher, childHasher;
		if (!next) {
			mainHasher = new DBHasher(9, 3, new int[] { 4, 2, 3 });
			childHasher = new DBHasher(9, 3, new int[] { 3, 3, 3 });
		} else {
			mainHasher = new DBHasher(9, 3, new int[] { 3, 3, 3 });
			childHasher = new DBHasher(9, 3, new int[] { 2, 3, 4 });
		}
		@SuppressWarnings("unchecked")
		GenHasher<FixedState>[] hasherArray = new GenHasher[9];
		CacheMove[] moveArray = new CacheMove[9];
		for (int i = 0; i < 9; i++) {
			hasherArray[i] = childHasher;
			moveArray[i] = new CacheMove(i, 0, 1);
		}
		CacheHasher<FixedState> dh = new CacheHasher<FixedState>(mainHasher,
				hasherArray, moveArray, true);
		return dh;
	}

	/*
	 * Just to make sure that any way the board can be generated still gives the
	 * correct result (next, hash, or unhash)
	 */
	private void arbitraryReset(CacheHasher<FixedState> dh) {
		if (dh.getHash() % 37 == 5) {
			long hash = dh.getHash();
			dh.unhash(hash);
		} else if (dh.getHash() % 37 == 24) {
			FixedState state = dh.newState();
			dh.getState(state);
			dh.hash(state);
		}
	}

	@Test
	public void childrenFunctionsWithoutPredictions() {
		GenHasher.enableToughAsserts();
		long[] currentChildren = new long[9];
		int[] places = new int[9];
		long[] predictedCurrentChildren = new long[9];

		CacheHasher<FixedState> dh = createHasher(false);
		CacheHasher<FixedState> dh2 = createHasher(true);
		FixedState board = dh.newState();
		FixedState cb = dh2.newState();
		dh.unhash(0);
		do {
			arbitraryReset(dh);
			dh.getState(board);
			int[] seq = board.cloneSequence();
			for (int i = 0; i < 9; i++) {
				if (seq[i] == 0) {
					seq[i] = 1;
					dh2.set(cb, seq);
					seq[i] = 0;
					dh2.hash(cb);
					predictedCurrentChildren[i] = dh2.getHash();
				} else {
					predictedCurrentChildren[i] = -1;
				}
			}
			int numChildren = dh.getChildren(places, currentChildren);
			int lastPlace = 9;
			for (int i = numChildren - 1; i >= 0; i--) {
				for (lastPlace--; lastPlace > places[i]; lastPlace--) {
					currentChildren[lastPlace] = -1;
				}
				currentChildren[places[i]] = currentChildren[i];
				Assert.assertEquals(currentChildren[places[i]],
						dh.getChild(places[i]));
			}
			for (lastPlace--; lastPlace >= 0; lastPlace--) {
				currentChildren[lastPlace] = -1;
			}
			for (int i = 0; i < 9; i++) {
				Assert.assertEquals(predictedCurrentChildren[i],
						currentChildren[i]);
			}
		} while (dh.next() != -1);
	}
}
