package edu.berkeley.gamesman;

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

		ReadlineConsole rc = new ReadlineConsole();
		rc.interact();
	}

}
