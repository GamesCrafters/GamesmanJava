import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;

import edu.berkeley.gamesman.hasher.DartboardHasher;

public class TestDartboardHasher {
	@Test
	public void steppingAndHashing() {
		DartboardHasher dh = new DartboardHasher(9, ' ', 'O', 'X');
		dh.setNums(4, 2, 3);
		Assert.assertEquals(1260L, dh.numHashes());
		DartboardHasher dh2 = new DartboardHasher(9, ' ', 'O', 'X');
		dh2.setNums(4, 2, 3);
		long lastHash = -1L;
		do {
			arbitraryReset(dh);
			long hash = dh.getHash();
			Assert.assertEquals(lastHash + 1, hash);
			lastHash = hash;
			dh2.unhash(hash);
			Assert.assertEquals(dh.toString(), dh2.toString());
			Assert.assertEquals(hash, dh2.getHash());
		} while (dh.next());
		Assert.assertEquals(1259L, lastHash);
	}

	/*
	 * Just to make sure that any way the board can be generated still gives the
	 * correct result (next, hash, or unhash)
	 */
	private void arbitraryReset(DartboardHasher dh) {
		if (dh.getHash() % 37 == 5) {
			long hash = dh.getHash();
			if (dh.getHash() % 3 == 0)
				dh.setNums(4, 2, 3);
			dh.unhash(hash);
		} else if (dh.getHash() % 37 == 24) {
			char[] board = dh.toString().toCharArray();
			if (dh.getHash() % 3 == 0)
				dh.setNums(4, 2, 3);
			dh.hash(board);
		}
	}

	@Test
	public void childrenFunctions() {
		long[] lastChildren = new long[9];
		long[] currentChildren = new long[9];
		long[] nextChildren = new long[9];
		int[] places = new int[9];
		long[] predictedLastChildren = new long[9];
		long[] predictedCurrentChildren = new long[9];
		long[] predictedNextChildren = new long[9];

		Arrays.fill(lastChildren, -1L);
		Arrays.fill(predictedLastChildren, -1L);

		DartboardHasher dh = new DartboardHasher(9, ' ', 'O', 'X');
		dh.setNums(4, 2, 3);
		dh.setReplacements(' ', 'O');
		DartboardHasher dh2 = new DartboardHasher(9, ' ', 'O', 'X');
		dh2.setNums(3, 3, 3);
		do {
			arbitraryReset(dh);
			char[] board = dh.toString().toCharArray();
			for (int i = 0; i < 9; i++) {
				if (board[i] == ' ') {
					board[i] = 'O';
					dh2.hash(board);
					predictedCurrentChildren[i] = dh2.getHash();
					predictedLastChildren[i] = predictedCurrentChildren[i];
					board[i] = ' ';
				} else {
					predictedCurrentChildren[i] = -1;
				}
			}
			dh.nextChildren(' ', 'O', nextChildren);
			for (int i = 0; i < 9; i++) {
				/*
				 * Either the last nextChildren[i] was actually the last
				 * child[i] or else nextChildren[i] is not changing.
				 */
				Assert.assertTrue(predictedNextChildren[i] == currentChildren[i]
						|| nextChildren[i] == predictedNextChildren[i]);
				predictedNextChildren[i] = nextChildren[i];
			}

			int numChildren = dh.getChildren(' ', 'O', places, currentChildren);
			int lastPlace = 9;
			for (int i = numChildren - 1; i >= 0; i--) {
				for (lastPlace--; lastPlace > places[i]; lastPlace--) {
					currentChildren[lastPlace] = -1;
				}
				currentChildren[places[i]] = currentChildren[i];
				Assert.assertEquals(nextChildren[places[i]],
						currentChildren[places[i]]);
				// Coupled with the above test, this is indiscretely nontrivial
			}
			for (lastPlace--; lastPlace >= 0; lastPlace--) {
				currentChildren[lastPlace] = -1;
			}
			dh.previousChildren(' ', 'O', lastChildren);
			for (int i = 0; i < 9; i++) {
				Assert.assertEquals(predictedCurrentChildren[i],
						currentChildren[i]);
				Assert.assertEquals(predictedLastChildren[i], lastChildren[i]);
			}
		} while (dh.next());
		for (int i = 0; i < 9; i++) {
			Assert.assertTrue(nextChildren[i] == currentChildren[i]
					|| nextChildren[i] == -1);
		}
	}

}
