package edu.berkeley.gamesman.parallel;

import java.io.IOException;

import org.junit.Test;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.propogater.writable.GenericTestRW;

public class TestFlipRecordRW {
	@Test
	public void testRW() throws IOException {
		FlipRecord rec = new FlipRecord();
		rec.set(GameValue.LOSE, 4);
		GenericTestRW.testEqualsCreate(rec);
		rec.set(GameValue.WIN, 3);
		GenericTestRW.testEqualsCreate(rec);
		rec.set(GameValue.TIE, 0);
		GenericTestRW.testEqualsCreate(rec);
		rec.set(GameValue.TIE, 3);
		GenericTestRW.testEqualsCreate(rec);
		rec.set(GameValue.DRAW);
		GenericTestRW.testEqualsCreate(rec);
	}
}
