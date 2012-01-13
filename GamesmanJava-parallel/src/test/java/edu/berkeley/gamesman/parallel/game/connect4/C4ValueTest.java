package edu.berkeley.gamesman.parallel.game.connect4;

import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Test;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.parallel.game.connect4.C4State;
import edu.berkeley.gamesman.parallel.game.connect4.Connect4;

public class C4ValueTest {
	@Test
	public void testTurnSwaps() {
		Configuration conf = new Configuration();
		Connect4 c4 = new Connect4();
		c4.setConf(conf);
		C4State state = c4.newState();
		Assert.assertEquals(c4.numPieces(state), 0);
		Assert.assertEquals(c4.getTurn(state), 1);
		Assert.assertEquals(c4.get(state, 0, 3), 0);
		c4.playMove(state, 3);
		Assert.assertEquals(c4.get(state, 0, 3), 1);
		Assert.assertEquals(c4.numPieces(state), 1);
		Assert.assertEquals(c4.getTurn(state), 2);
		Assert.assertEquals(c4.get(state, 1, 3), 0);
		c4.playMove(state, 3);
		Assert.assertEquals(c4.get(state, 1, 3), 2);
		Assert.assertEquals(c4.numPieces(state), 2);
		Assert.assertEquals(c4.getTurn(state), 1);
	}

	@Test
	public void testPV() {
		Configuration conf = new Configuration();
		Connect4 c4 = new Connect4();
		c4.setConf(conf);
		C4State state = c4.newState();

		// Test vertical win
		c4.playMove(state, 3);
		c4.playMove(state, 4);
		c4.playMove(state, 3);
		c4.playMove(state, 4);
		c4.playMove(state, 3);
		Assert.assertNull(c4.getValue(state));
		c4.playMove(state, 4);
		Assert.assertNull(c4.getValue(state));
		c4.playMove(state, 3);
		Assert.assertEquals(c4.getValue(state), GameValue.LOSE);

		// Test horizontal win
		state = c4.newState();
		c4.playMove(state, 0);
		c4.playMove(state, 0);
		c4.playMove(state, 1);
		c4.playMove(state, 1);
		c4.playMove(state, 2);
		Assert.assertNull(c4.getValue(state));
		c4.playMove(state, 2);
		Assert.assertNull(c4.getValue(state));
		c4.playMove(state, 3);
		Assert.assertEquals(c4.getValue(state), GameValue.LOSE);

		// Test full board tie
		state = c4.newState();
		for (int col = 0; col < 2; col++) {
			for (int row = 0; row < 4; row++) {
				c4.playMove(state, col);
			}
		}
		for (int row = 0; row < 3; row++) {
			c4.playMove(state, 3);
		}
		for (int row = 0; row < 4; row++) {
			c4.playMove(state, 2);
		}
		c4.playMove(state, 3);
		for (int row = 0; row < 4; row++) {
			c4.playMove(state, 4);
		}
		Assert.assertEquals(c4.getValue(state), GameValue.TIE);

		// Test diagonal win
		state = c4.newState();
		for (int col = 0; col < 2; col++) {
			for (int row = 0; row < 4; row++) {
				c4.playMove(state, col);
			}
		}
		for (int row = 0; row < 3; row++) {
			c4.playMove(state, 3);
		}
		for (int row = 0; row < 4; row++) {
			c4.playMove(state, 2);
		}
		Assert.assertNull(c4.getValue(state));
		c4.playMove(state, 4);
		Assert.assertEquals(c4.getValue(state), GameValue.LOSE);
	}
}
