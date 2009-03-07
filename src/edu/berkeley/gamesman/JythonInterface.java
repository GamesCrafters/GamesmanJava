package edu.berkeley.gamesman;

import org.python.core.PyObject;
import org.python.util.InteractiveConsole;
import org.python.util.JLineConsole;

public final class JythonInterface {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//PythonInterpreter pi = new PythonInterpreter();

		InteractiveConsole rc = new InteractiveConsole() {
		};
		try {
			rc = new JLineConsole();
		} catch(NoClassDefFoundError e) { //if we don't have the ReadlineLibrary
			rc = new InteractiveConsole() {
				@Override
				public String raw_input(PyObject prompt) {
					return super.raw_input(prompt);
				}
			};
		}
		//this will let us put .py files in the junk directory, and things will just work =)
		rc.exec(String.format("import sys; sys.path.append('%s/%s'); sys.path.append('%s/../%s');", System.getProperty("user.dir"), "junk", System.getProperty("user.dir"), "junk"));
		rc.interact();
	}

}
