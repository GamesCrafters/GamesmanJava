package edu.berkeley.gamesman.game.type;

import org.junit.Assert;
import org.junit.Test;

public class GameRecordTest {
	@Test
	public void testPreviousMatches() {
		GameRecord gr1 = new GameRecord(GameValue.WIN, 5);
		GameRecord gr2 = new GameRecord();
		gr2.previousPosition(gr1);
		gr1.previousPosition();
		Assert.assertEquals(gr1, gr2);
	}
}
