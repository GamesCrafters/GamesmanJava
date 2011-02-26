import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import edu.berkeley.gamesman.hasher.QuartoMinorHasher;

public class TestQuarto {
	@Test
	public void testStepper() {
		QuartoMinorHasher qmh = new QuartoMinorHasher();
		QuartoMinorHasher qmh2 = new QuartoMinorHasher();
		qmh.setTier(7);
		qmh2.setTier(7);
		Assert.assertEquals(151410L, qmh.numHashesForTier(7));
		for (long hash = 0L; hash < 151410L; hash++) {
			arbitraryReset(qmh);
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
	private void arbitraryReset(QuartoMinorHasher qmh) {
		if (qmh.getHash() % 37 == 5) {
			long hash = qmh.getHash();
			if (qmh.getHash() % 3 == 0)
				qmh.setTier(7);
			qmh.unhash(hash);
		} else if (qmh.getHash() % 37 == 24) {
			int[] board = qmh.getBoard();
			if (qmh.getHash() % 3 == 0)
				qmh.setTier(7);
			qmh.hash(board);
		}
	}
}
