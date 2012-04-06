package edu.berkeley.gamesman.hasher.genhasher;

import org.junit.Assert;
import org.junit.Test;

import edu.berkeley.gamesman.parallel.game.connect4.C4Hasher;
import edu.berkeley.gamesman.parallel.game.connect4.C4State;

public class DBInvCalculatorTest {
	@Test
	public void testC4Inv() {
		C4Hasher hasher = new C4Hasher(3, 3);
		C4State state = hasher.newState();
		int[] board = new int[] { 0, 0, 0, 1, 1, 0, 2, 2, 1, 5 };
		hasher.set(state, board);
		DBInvCalculator calc = new DBInvCalculator(9);
		Assert.assertEquals((((5 << 8) | 3) << 8) | 2, calc.getInv(state));
		Assert.assertEquals((3 << 8) | 2, calc.getPieceInv(state));
	}
}
