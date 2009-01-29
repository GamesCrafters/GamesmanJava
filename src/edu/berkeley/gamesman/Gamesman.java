/**
 * 
 */
package edu.berkeley.gamesman;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.hasher.Hasher;
import edu.berkeley.gamesman.master.Master;
import edu.berkeley.gamesman.solver.Solver;

/**
 * @author Gamescrafters Project
 * 
 */
public final class Gamesman {

	private Game<?,?> gm;
	private Hasher ha;
	private Solver so;
	private Database<?> db;
	private boolean testrun;

	private Gamesman(Game<?, ?> g, Solver s, Hasher h, Database<?> d, boolean er) {
		gm = g;
		ha = h;
		so = s;
		db = d;
		testrun = er;
	}

	/**
	 * @param args
	 *            Command line arguments
	 */
	public static void main(String[] args) {
		OptionProcessor.initializeOptions(args);
		OptionProcessor.acceptOption("h", "help", false,
				"Display this help string and exit");
		OptionProcessor.nextGroup();
		OptionProcessor.acceptOption("x", "with-graphics", false,
				"Enables use of graphical displays");
		OptionProcessor.nextGroup();
		OptionProcessor.acceptOption("G", "game", true,
				"Specifies which game to play", "NullGame");
		OptionProcessor.acceptOption("S", "solver", true,
				"Specifies which solver to use", "TierSolver");
		OptionProcessor.acceptOption("H", "hasher", true,
				"Specifies which hasher to use", "GenericHasher");
		OptionProcessor.acceptOption("D", "database", true,
				"Specifies which database backend to use", "FileDatabase");
		OptionProcessor.acceptOption("M", "master", true,
				"Specifies which master controller to use", "LocalMaster");
		OptionProcessor
				.acceptOption("C", "command", true, "Command to execute");
		OptionProcessor.nextGroup();

		String masterName = OptionProcessor.checkOption("master");

		Object omaster = null;
		try {
			omaster = Class.forName(
					"edu.berkeley.gamesman.master." + masterName).newInstance();
		} catch (ClassNotFoundException cnfe) {
			System.err.println("Could not load master controller '"
					+ masterName + "': " + cnfe);
			System.exit(1);
		} catch (IllegalAccessException iae) {
			System.err.println("Not allowed to access requested master '"
					+ masterName + "': " + iae);
			System.exit(1);
		} catch (InstantiationException ie) {
			System.err.println("Master failed to instantiate: " + ie);
			System.exit(1);
		}

		if (!(omaster instanceof Master)) {
			System.err
					.println("Master does not implement master.Master interface");
			System.exit(1);
		}

		Master m = (Master) omaster;

		Util.debug("Preloading classes...");

		String gameName, solverName, hasherName, databaseName;

		gameName = OptionProcessor.checkOption("game");
		solverName = OptionProcessor.checkOption("solver");
		hasherName = OptionProcessor.checkOption("hasher");
		databaseName = OptionProcessor.checkOption("database");

		Class<? extends Game<?, ?>> g;
		Class<? extends Solver> s;
		Class<? extends Hasher> h;
		Class<? extends Database<?>> d;

		try {
			g = (Class<? extends Game<?, ?>>) Class
					.forName("edu.berkeley.gamesman.game." + gameName);
			s = (Class<? extends Solver>) Class
					.forName("edu.berkeley.gamesman.solver." + solverName);
			h = (Class<? extends Hasher>) Class
					.forName("edu.berkeley.gamesman.hasher." + hasherName);
			d = (Class<? extends Database<?>>) Class
					.forName("edu.berkeley.gamesman.database." + databaseName);
		} catch (Exception e) {
			System.err.println("Fatal error in preloading: " + e);
			return;
		}
		
		boolean dohelp = (OptionProcessor.checkOption("h") != null);

		String cmd = OptionProcessor.checkOption("command");
		if (cmd != null) {
			try {
				boolean tr = (OptionProcessor.checkOption("help") != null);
				Gamesman executor = new Gamesman(g.newInstance(), s
						.newInstance(), h.newInstance(), d.newInstance(),tr);
				executor.getClass().getMethod("execute" + cmd,
						(Class<?>[]) null).invoke(executor);
			} catch (NoSuchMethodException nsme) {
				System.out.println("Don't know how to execute command " + nsme);
			} catch (IllegalAccessException iae) {
				System.out.println("Permission denied while executing command "
						+ iae);
			} catch (InstantiationException ie) {
				System.out.println("Could not instantiate: " + ie);
			} catch (InvocationTargetException ite) {
				System.out.println("Exception while executing command: " + ite);
				ite.getTargetException().printStackTrace();
			}
		} else if(!dohelp){
			Util.debug("Defaulting to solve...");
			m.initialize(g, s, h, d);
			m.run();
		}


		if (dohelp) {
			System.out.println("Gamesman help stub, please fill this out!"); // TODO: help text
			OptionProcessor.help();
			return;
		}
		
		Util.debug("Finished run, tearing down...");

	}
	
	/**
	 * Diagnostic call to unhash an arbitrary value to a game board
	 */
	public void executeunhash(){
		OptionProcessor.acceptOption("v", "hash", true, "The hash value to be manipulated");
		if(testrun) return;
		Object state = gm.hashToState(new BigInteger(OptionProcessor.checkOption("hash")));
		System.out.println(((Game<Object,Object>)gm).stateToString(state));
	}

}
