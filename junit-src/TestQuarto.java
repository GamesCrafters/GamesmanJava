import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import edu.berkeley.gamesman.hasher.QuartoMinorHasher;

public class TestQuarto {
	private static final long MEM = 256L;

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
		int tier = 6;
		qmh.setTier(tier);
		qmh2.setTier(tier + 1);
		long[] children = new long[72];
		long numHashes = qmh.numHashesForTier(tier);
		for (long hash = 0L; hash < numHashes; hash++) {
			arbitraryReset(qmh, tier);
			int[] board = qmh.getBoard();
			qmh.getChildren(children);
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
		long[][][] caches = new long[8][17][];
		QuartoMinorHasher qmh = new QuartoMinorHasher();
		for (long hash = 0L; hash < 151410L; hash++) {
			arbitraryReset(qmh, 7);
			for (int place = 0; place <= 7; place++) {
				long[] oldCache = caches[place][16];
				long[] cache = qmh.getCache(place, MEM);
				if (!Arrays.equals(oldCache, cache)) {
					Assert.assertTrue(oldCache == null
							|| oldCache[1] == qmh.getHash());
					caches[place][16] = cache;
				}
				if (cache == null) {
					for (int piece = 0; piece < 16; piece++) {
						if (!qmh.used(piece)) {
							caches[place][piece] = qmh.getCache(place, piece,
									MEM / 11);
						}
					}
				}
			}
		}
	}
}
