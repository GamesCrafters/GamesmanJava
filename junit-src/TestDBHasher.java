import junit.framework.Assert;

import org.junit.Test;

import edu.berkeley.gamesman.hasher.DBHasher;
import edu.berkeley.gamesman.hasher.fixed.FixedState;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class TestDBHasher {
	@Test
	public void steppingAndHashing() {
		GenHasher.enableToughAsserts();
		DBHasher dh = new DBHasher(9, 3, new int[] { 4, 2, 3 });
		Assert.assertEquals(1260L, dh.totalPositions());
		DBHasher dh2 = new DBHasher(9, 3, new int[] { 4, 2, 3 });
		long lastHash = -1L;
		FixedState state = dh.newState();
		FixedState state2 = dh2.newState();
		do {
			arbitraryReset(dh, state);
			long hash = dh.hash(state);
			Assert.assertEquals(lastHash + 1, hash);
			lastHash = hash;
			dh2.unhash(hash, state2);
			Assert.assertEquals(state, state2);
			Assert.assertEquals(hash, dh2.hash(state2));
		} while (dh.step(state) != -1);
		Assert.assertEquals(1259L, lastHash);
	}

	/*
	 * Just to make sure that any way the board can be generated still gives the
	 * correct result (next, hash, or unhash)
	 */
	private void arbitraryReset(DBHasher dh, FixedState state) {
		if (dh.hash(state) % 37 == 5) {
			long hash = dh.hash(state);
			dh.unhash(hash, state);
		}
	}
}
