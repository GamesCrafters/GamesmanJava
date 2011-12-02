package edu.berkeley.gamesman.database;

import java.io.IOException;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;

public class TierCutDatabaseTest {
	public static SplitLocalDatabase tttDbUncut;
	public static TierCutDatabase tttDbCut;
	public static DatabaseHandle tttDbUncutHandle;
	public static DatabaseHandle tttDbCutHandle;
	public static TierGame game;

	@BeforeClass
	public static void initDatabases() throws IOException,
			ClassNotFoundException {
		tttDbUncut = (SplitLocalDatabase) Database
				.openDatabase("../GamesmanJava/test_data/database/tiercut_db_ttt/ttt.db");
		tttDbCut = (TierCutDatabase) Database
				.openDatabase("../GamesmanJava/test_data/database/tiercut_db_ttt/ttt_cut.db");
		tttDbUncutHandle = tttDbUncut.getHandle(true);
		tttDbCutHandle = tttDbCut.getHandle(true);
		game = (TierGame) tttDbCut.conf.getGame();
	}

	@Before
	public void initState() {
		game.setStartingPosition(0);
	}

	@Test
	public void optimzedReadStartPos() throws IOException {
		TierState pos = game.newState();
		game.getState(pos);
		long recordIndex = game.stateToHash(pos);
		long optimizedRead = tttDbCut.optimizedMissingTierSolve(tttDbCutHandle,
				recordIndex);
		long normalRead = tttDbUncut.readRecord(tttDbUncutHandle, recordIndex);
		Assert.assertEquals(normalRead, optimizedRead);
	}

	@AfterClass
	public static void cleanupDatabases() throws IOException {
		tttDbCut.close();
		tttDbUncut.close();
	}

}
