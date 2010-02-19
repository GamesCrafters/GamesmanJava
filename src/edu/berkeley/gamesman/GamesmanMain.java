package edu.berkeley.gamesman;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Master;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author Steven Schlansker
 */
public final class GamesmanMain extends GamesmanApplication {
	private Configuration conf;
	private Game<State> gm;

	/**
	 * No arg constructor
	 */
	public GamesmanMain() {}

	@Override
	public int run(Properties props) {
		try {
			this.conf = new Configuration(props);
		} catch (ClassNotFoundException e) {
			Util.fatalError("Configuration contains unknown game or hasher ", e);
		}
		gm = Util.checkedCast(conf.getGame());
		Thread.currentThread().setName("Gamesman");
		String masterName = conf.getProperty("gamesman.master","LocalMaster");

		Object omaster = null;
		try {
			omaster = Class.forName(
					"edu.berkeley.gamesman.master." + masterName).newInstance();
		} catch (ClassNotFoundException cnfe) {
			Util.fatalError("Could not load master controller '"
					+ masterName + "': " + cnfe);
		} catch (IllegalAccessException iae) {
			Util.fatalError("Not allowed to access requested master '"
					+ masterName + "': " + iae);
		} catch (InstantiationException ie) {
			Util.fatalError("Master failed to instantiate: " + ie);
		}

		if (!(omaster instanceof Master)) {
			Util.fatalError("Master does not implement master.Master interface");
		}

		Master m = (Master) omaster;

		assert Util.debug(DebugFacility.CORE, "Preloading classes...");

		String gameName, solverName, hasherName;
		String[] databaseName;

		gameName = conf.getProperty("gamesman.game");
		if(gameName == null)
			Util.fatalError("You must specify a game with the property gamesman.game");
		solverName = conf.getProperty("gamesman.solver");
		if(solverName == null)
			Util.fatalError("You must specify a solver with the property gamesman.solver");
		hasherName = conf.getProperty("gamesman.hasher");
		if(hasherName == null)
			Util.fatalError("You must specify a hasher with the property gamesman.hasher");
		databaseName = conf.getProperty("gamesman.database","CachedDatabase").split(":");

		Class<? extends Solver> s;
		Class<? extends Database> d;
		boolean cached;
//		Class<? extends Game<Object>> g;
//		Class<? extends Hasher<Object>> h;

		try {
//			g = Util.typedForName("edu.berkeley.gamesman.game." + gameName);
			s = Util.typedForName("edu.berkeley.gamesman.solver." + solverName, Solver.class);
//			h = Util.typedForName("edu.berkeley.gamesman.hasher." + hasherName);
			if (databaseName.length > 1
					&& databaseName[0].trim().equals("cached")) {
				d = Util.typedForName("edu.berkeley.gamesman.database."
						+ databaseName[1].trim(), Database.class);
				cached=true;
			}else{
				d = Util.typedForName("edu.berkeley.gamesman.database."
						+ databaseName[0].trim(), Database.class);
				cached=false;
			}
		} catch (Exception e) {
			Util.fatalError("Fatal error in preloading", e);
			return 1;
		}
		
		String cmd = conf.getProperty("gamesman.command",null);
		if (cmd != null) {
			try {
				this.getClass().getMethod("execute" + cmd, (Class<?>[]) null).invoke(this);
			} catch (NoSuchMethodException nsme) {
				System.out.println("Don't know how to execute command " + nsme);
			} catch (IllegalAccessException iae) {
				System.out.println("Permission denied while executing command "
						+ iae);
			} catch (InvocationTargetException ite) {
				System.out.println("Exception while executing command: " + ite);
				ite.getTargetException().printStackTrace();
			}
		} else {
			assert Util.debug(DebugFacility.CORE, "Defaulting to solve...");
			try {
				m.initialize(conf, s, d, cached);
			} catch (Exception e){
				Util.fatalError("Exception while instantiating and initializing",e);
			}
			m.run();
		}

		assert Util.debug(DebugFacility.CORE,"Finished run, tearing down...");
		return 0;
	}

	/**
	 * Diagnostic call to unhash an arbitrary value to a game board
	 */
	public void executeunhash() {
		State state = gm.hashToState(Long.parseLong(conf.getProperty("gamesman.hash")));
		System.out.println(gm.displayState(state));
	}

	/**
	 * Diagnostic call to view all child moves of a given hashed game state
	 */
	public void executegenmoves() {
		State state = gm.hashToState(Long.parseLong(conf.getProperty("gamesman.hash")));
		for (Pair<String,State> nextstate : gm.validMoves(state)) {
			System.out.println(gm.stateToHash(nextstate.cdr));
			System.out.println(gm.displayState(nextstate.cdr));
		}
	}

	/**
	 * Hash a single board with the given hasher and print it.
	 */
	public void executehash() {
		String str = conf.getProperty("board");
		if (str == null)
			Util.fatalError("Please specify a board to hash");
		System.out.println(gm.stateToHash(gm.stringToState(str.toUpperCase())));
	}

	/**
	 * Evaluate a single board and return its primitive value.
	 */
	public void executeevaluate() {
		String board = conf.getProperty("gamesman.board");
		if (board == null)
			Util.fatalError("Please specify a hash to evaluate");
		long val = Long.parseLong(board);
		System.out.println(gm.primitiveValue(gm.hashToState(val)));
	}

	//public void executetestRPC(){
	//	new RPCTest();
	//}

}
