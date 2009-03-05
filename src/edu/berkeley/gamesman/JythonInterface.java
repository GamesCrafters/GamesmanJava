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
		pi.exec("from edu.berkeley.gamesman.util import Util");
		pi.exec("Util.warn(\"testing!\")");
		pi.exec("print \"hi!\"");

		InteractiveConsole rc = null;
		try {
			rc = new ReadlineConsole();
		} catch(NoClassDefFoundError e) { //if we don't have the ReadlineLibrary
			rc = new InteractiveConsole();
		}
		rc.interact();
	}

}
