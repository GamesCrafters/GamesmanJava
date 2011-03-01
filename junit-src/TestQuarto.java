import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import edu.berkeley.gamesman.hasher.QuartoMinorHasher;

public class TestQuarto {
	private static final long MEM = 512L;

	@Test
	public void testStepper() {
		QuartoMinorHasher qmh = new QuartoMinorHasher();
		QuartoMinorHasher qmh2 = new QuartoMinorHasher();
		qmh.setTier(7);
		qmh2.setTier(7);
		Assert.assertEquals(151410L, qmh.numHashesForTier(7));
		for (long hash = 0L; hash < 151410L; hash++) {
			arbitraryReset(qmh, 7);
			Assert.assertEquals(hash, qmh.getHash());
			qmh2.unhash(hash);
			int[] board = qmh.getBoard();
			Assert.assertTrue(Arrays.equals(board, qmh2.getBoard()));
			qmh2.reset();
			Assert.assertEquals(hash, qmh2.hash(board));
			if (hash < 151409L)
				qmh.nextHashInTier();
		}
	}

	/*
	 * Just to make sure that any way the board can be generated still gives the
	 * correct result (next, hash, or unhash)
	 */
	private void arbitraryReset(QuartoMinorHasher qmh, int tier) {
		if (qmh.getHash() % 37 == 5) {
			long hash = qmh.getHash();
			if (qmh.getHash() % 3 == 0)
				qmh.setTier(tier);
			qmh.unhash(hash);
		} else if (qmh.getHash() % 37 == 24) {
			int[] board = qmh.getBoard();
			if (qmh.getHash() % 3 == 0)
				qmh.setTier(tier);
			qmh.hash(board);
		}
	}

	@Test
	public void testChildren() {
		QuartoMinorHasher qmh = new QuartoMinorHasher();
		QuartoMinorHasher qmh2 = new QuartoMinorHasher();
		int tier = 5;
		qmh.setTier(tier);
		qmh2.setTier(tier + 1);
		long[] children = new long[(tier + 1) * (16 - tier)];
		long numHashes = qmh.numHashesForTier(tier);
		for (long hash = 0L; hash < numHashes; hash++) {
			arbitraryReset(qmh, tier);
			int[] board = qmh.getBoard();
			int numChildren = qmh.getChildren(children);
			Assert.assertEquals(numChildren, children.length);
			int[] board2 = new int[board.length + 1];
			int childNum = 0;
			for (int piece = 0; piece < 16; piece++) {
				if (qmh.used(piece))
					continue;
				for (int place = 0; place <= board.length; place++) {
					long child = children[childNum++];
					for (int i = 0; i < board2.length; i++) {
						if (i < place) {
							board2[i] = board[i];
						} else if (i > place) {
							board2[i] = board[i - 1];
						} else
							board2[i] = piece;
					}
					qmh2.dropState(board2);
					qmh2.unhash(child);
					Assert.assertTrue(Arrays.equals(board2, qmh2.getBoard()));
				}
			}
			if (hash < numHashes - 1L)
				qmh.nextHashInTier();
		}
	}

	@Test
	public void testCacher() {
		QuartoMinorHasher qmh = new QuartoMinorHasher();
		int tier = 5;
		qmh.setTier(tier);
		long[] children = new long[(tier + 1) * (16 - tier)];
		long[][][] cache = new long[tier + 1][16][2];
		long[][] fullCache = new long[tier + 1][];
		long numHashes = qmh.numHashesForTier(tier);
		for (long hash = 0L; hash < numHashes; hash++) {
			arbitraryReset(qmh, tier);
			for (int place = 0; place <= tier; place++) {
				long[] oldCache = fullCache[place];
				long[] newCache = qmh.getCache(place, MEM);
				fullCache[place] = newCache;
				if (newCache == null) {
					for (int piece = 0; piece < 16; piece++) {
						if (qmh.used(piece))
							continue;
						newCache = qmh
								.getCache(place, piece, MEM / (16 - tier));
						long[] oldInnerCache = cache[place][piece];
						if (!Arrays.equals(oldInnerCache, newCache)) {
							if (oldInnerCache == null) {
								oldInnerCache = oldCache;
							}
							Assert.assertTrue(newCache[0] + newCache[1] <= oldInnerCache[0]
									|| newCache[0] >= oldInnerCache[0]
											+ oldInnerCache[1]);
							cache[place][piece] = newCache;
						}
					}
				} else {
					if (!Arrays.equals(oldCache, newCache)) {
						if (oldCache != null) {
							Assert.assertTrue(newCache[0] + newCache[1] <= oldCache[0]
									|| newCache[0] >= oldCache[0] + oldCache[1]);
						}
					}
				}
			}
			int[] board = qmh.getBoard();
			int numChildren = qmh.getChildren(children);
			Assert.assertEquals(numChildren, children.length);
			int[] board2 = new int[board.length + 1];
			int childNum = 0;
			for (int piece = 0; piece < 16; piece++) {
				if (qmh.used(piece))
					continue;
				for (int place = 0; place <= board.length; place++) {
					long child = children[childNum++];
					for (int i = 0; i < board2.length; i++) {
						if (i < place) {
							board2[i] = board[i];
						} else if (i > place) {
							board2[i] = board[i - 1];
						} else
							board2[i] = piece;
					}
					long[] childCache;
					if (fullCache[place] == null)
						childCache = cache[place][piece];
					else
						childCache = fullCache[place];
					long ind = child - childCache[0];
					Assert.assertTrue(ind >= 0 && ind < childCache[1]);
				}
			}
			if (hash < numHashes - 1L)
				qmh.nextHashInTier();
		}
	}
}
