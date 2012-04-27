package edu.berkeley.gamesman.parallel.game.tootandotto;

import org.apache.hadoop.conf.Configuration;
import org.junit.Test;

public class TOTest {
	
	@Test
	public void testTurnSwap() {
		Configuration conf = new Configuration();
		TootAndOtto to = new TootAndOtto();
		to.setConf(conf);
	}

}
