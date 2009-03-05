package edu.berkeley.gamesman;

import org.python.util.InteractiveConsole;
import org.python.util.PythonInterpreter;
import org.python.util.ReadlineConsole;

public final class JythonInterface {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PythonInterpreter pi = new PythonInterpreter();
		//this will let us put .py files in the junk directory, and things will just work =)
		pi.exec(String.format("import sys; sys.path.append('%s/%s')", System.getProperty("user.dir"), "junk"));

		InteractiveConsole rc = null;
		try {
			rc = new ReadlineConsole();
		} catch(NoClassDefFoundError e) { //if we don't have the ReadlineLibrary
			rc = new InteractiveConsole();
		}
		rc.interact();
	}

}
