package edu.berkeley.gamesman;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Util;

/**
 * An entry main for all of gamesman-java.
 * The main class to use when solving on a single machine
 * 
 * @author Jeremy Fleischman
 */
public class Gamesman {
	/**
	 * Gamesman applications
	 */
	private static final HashMap<String, String> APPLICATION_MAP = new HashMap<String, String>();

	static {
		APPLICATION_MAP.put("gamesmanmain", GamesmanMain.class.getName());
		APPLICATION_MAP.put("jythoninterface", JythonInterface.class.getName());
	}

	/**
	 * @param args The command line arguments. Should just be a job file, or a
	 *             target Gamesman application that optionally takes a job file.
	 */
	public static void main(String[] args) {
		String jobFile = null, entryPoint = null;

		// Default to GamesmanMain application
		if (args.length == 1) {
			String arg = args[0];
			if (APPLICATION_MAP.containsKey(arg.toLowerCase())) {
				// we have a Gamesman application
				entryPoint = arg.toLowerCase();
			} else {
				// if we just have a job file, use GamesmanMain
				jobFile = arg;
				entryPoint = "gamesmanmain";
			}
		} else if (args.length == 2) {
			entryPoint = args[0];
			jobFile = args[1];
		} else {
			System.out.println("Usage: Gamesman [entry point] [job file]"
							+ "\n\tAvailable entry points (some require a job file, some don't): "
							+ APPLICATION_MAP.keySet()
							+ "\n\tIf a job file is given, but no entry point is, entry point defaults to GamesmanMain.");
			return;
		}
		if (!APPLICATION_MAP.containsKey(entryPoint)) {
			System.out.println("Couldn't find " + entryPoint + " in "
					+ APPLICATION_MAP.keySet());
			return;
		}

		// Read properties from job file if specified
		Properties props = null;
		if (jobFile != null) {
			try {
				props = Configuration.readProperties(jobFile);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			DebugSetup.setup(props);
		}

		// Load & run the target Gamesman application
		Class<? extends GamesmanApplication> cls;
		GamesmanApplication ga;
		try {
			cls = Util.typedForName(APPLICATION_MAP.get(entryPoint),
					GamesmanApplication.class);
			ga = cls.getConstructor().newInstance();
		} catch (IllegalArgumentException e) {
			throw new Error(e);
		} catch (SecurityException e) {
			throw new Error(e);
		} catch (InstantiationException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw new Error(e.getCause());
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		}
		ga.run(props);
	}
}
