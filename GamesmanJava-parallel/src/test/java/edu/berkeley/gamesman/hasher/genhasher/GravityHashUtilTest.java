package edu.berkeley.gamesman.hasher.genhasher;

import org.junit.Assert;
import org.junit.Test;

import edu.berkeley.gamesman.parallel.game.connect4.C4Hasher;
import edu.berkeley.gamesman.parallel.game.connect4.C4State;

public class GravityHashUtilTest {
	@Test
	public void testC4Inv() {
		C4Hasher hasher = new C4Hasher(3, 3);
		C4State state = hasher.newState();
		int[] board = new int[] { 0, 0, 0, 1, 1, 0, 2, 2, 1, 5 };
		hasher.set(state, board);
		GravityHashUtil<C4State> calc = new GravityHashUtil<C4State>(3, 3);
		Assert.assertEquals((((5 << 9) | 3) << 8) | 2,
				calc.getInv(hasher, state));
		board = new int[] { 1, 0, 0, 0, 1, 0, 2, 2, 1, 5 };
		hasher.set(state, board);
		Assert.assertEquals((((((5 << 1) | 1) << 8) | 3) << 8) | 2,
				calc.getInv(hasher, state));
	}

}
