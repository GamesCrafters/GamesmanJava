package edu.berkeley.gamesman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Properties;

import org.python.core.PyObject;
import org.python.util.InteractiveConsole;
import org.python.util.JLineConsole;
import org.python.util.ReadlineConsole;

import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.JythonUtil;
import edu.berkeley.gamesman.util.Util;

/**
 * @author Jeremy Fleischman
 * @author Steven Schlansker
 *
 */
public final class JythonInterface extends GamesmanApplication {
	private enum Consoles {	DUMB,READLINE,JLINE	}
	
	/**
	 * No arg constructor
	 */
	public JythonInterface() {}
	
	@Override
	public int run(Properties props) {
		//Preferences prefs = Preferences.userNodeForPackage(JythonInterface.class);
		
		String consoleName =null; // prefs.get("console", null);
		Consoles console = null;
		if (consoleName != null) {
			try {
				console = Consoles.valueOf(consoleName);
			} catch(IllegalArgumentException e) {
				System.out.println(consoleName + " isn't one of " + Arrays.toString(Consoles.values()));
			}
		}
		while(console == null){
			System.out.println("Available consoles:");
			for(int i = 0; i < Consoles.values().length; i++)
				System.out.printf("\t%d. %s\n", i, Consoles.values()[i].toString());
			System.out.print("What console would you like to use? ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			int i=-1;
			try {
				i = Integer.parseInt(br.readLine());
			} catch(NumberFormatException e) {
				System.out.println("Please input a number");
				continue;
			} catch (IOException e) {
				Util.fatalError("Can't read from console", e);
			}
			if(i < 0 || i >= Consoles.values().length) {
				System.out.println(i + " is out of range");
				continue;
			}
			console = Consoles.values()[i];
		}
		JythonUtil.init();
		//prefs.put("console", console.name());
		InteractiveConsole rc = null;
		switch(console){
		case DUMB:
			rc = new InteractiveConsole() {
				@Override
				public String raw_input(PyObject prompt) {
					//eclipse sometimes interleaves the error messages 
					exec("import sys; sys.stderr.flush()");
					return super.raw_input(prompt);
				}
			};
			break;
		case JLINE:
			rc = new JLineConsole();
			break;
		case READLINE:
			rc = new ReadlineConsole();
			break;
		default:
			Util.fatalError("Need to pick a console");
		}

		EnumSet<DebugFacility> debugOpts = EnumSet.noneOf(DebugFacility.class);
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		cl.setDefaultAssertionStatus(false);
		debugOpts.add(DebugFacility.SOLVER);
		DebugFacility.SOLVER.setupClassloader(cl);
		debugOpts.add(DebugFacility.CORE);
		DebugFacility.CORE.setupClassloader(cl);
		Util.enableDebuging(debugOpts);
		
		//this will let us put .py files in the junk directory, and things will just work =)
	
		rc.exec("from Play import *");
		rc.interact();
		return 0;
	}


	/**
	 * Simple main function to run JythonInterface directly.
	 * @param args program args
	 * @throws Throwable 
	 */
	public static void main(String[] args) throws Throwable {
		String []newArgs = new String[args.length + 1];
		newArgs[0] = "JythonInterface";
		System.arraycopy(args, 0, newArgs, 1, args.length);
		Gamesman.main(newArgs);
	}
}
