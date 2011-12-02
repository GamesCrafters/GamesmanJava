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
	private static SplitLocalDatabase tttDbUncut;
	private static TierCutDatabase tttDbCut;
	private static DatabaseHandle tttDbUncutHandle;
	private static DatabaseHandle tttDbCutHandle;
	private static TierGame game;
	private TierState state;

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
		state = game.newState();
	}

	private void optimizedReadRecord(long recordIndex) throws IOException {
		long optimizedRead = tttDbCut.optimizedMissingTierSolve(tttDbCutHandle,
				recordIndex);
		long normalRead = tttDbUncut.readRecord(tttDbUncutHandle, recordIndex);
		Assert.assertEquals(normalRead, optimizedRead);
	}

	@Test
	public void optimizedReadCutTier1() throws IOException {
		optimizedReadRecord(game.stateToHash(state));
	}

	@Test
	public void optimizedReadCutTier2() throws IOException {
		state = game.validMoves(state).iterator().next().cdr;
		state = game.validMoves(state).iterator().next().cdr;
		optimizedReadRecord(game.stateToHash(state));
	}

	@Test
	public void optimizedReadUncutTier() throws IOException {
		state = game.validMoves(state).iterator().next().cdr;
		optimizedReadRecord(game.stateToHash(state));
	}

	@AfterClass
	public static void cleanupDatabases() throws IOException {
		tttDbCut.close();
		tttDbUncut.close();
	}

}
