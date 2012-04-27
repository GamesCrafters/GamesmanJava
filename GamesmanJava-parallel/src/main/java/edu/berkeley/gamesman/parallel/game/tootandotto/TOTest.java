package edu.berkeley.gamesman.parallel.game.tootandotto;

import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Test;

import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.parallel.game.tootandotto.TootAndOtto;

public class TOTest {
	
	@Test
	public void TurnSwapTest() {
		Configuration conf = new Configuration();
		TootAndOtto to = new TootAndOtto();
		to.setConf(conf);
		CountingState state = to.newState();
		Assert.assertEquals(to.numPieces(state), 0);
		Assert.assertEquals(to.getTurn(state), 1);
		Assert.assertEquals(to.get(state, 0, 3), 0);
		to.playMove(state, 3);
		Assert.assertEquals(to.get(state, 0, 3), 1);
		Assert.assertEquals(to.numPieces(state), 1);
		Assert.assertEquals(to.getTurn(state), 2);
		Assert.assertEquals(to.get(state, 1, 3), 0);
		to.playMove(state, 3);
		Assert.assertEquals(to.get(state, 1, 3), 2);
		Assert.assertEquals(to.numPieces(state), 2);
		Assert.assertEquals(to.getTurn(state), 1);
	}

}
