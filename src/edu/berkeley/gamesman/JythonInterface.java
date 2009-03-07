package edu.berkeley.gamesman;

import java.io.IOException;

import jline.ConsoleReader;
import jline.Terminal;

import org.python.core.PyObject;
import org.python.util.InteractiveConsole;
import org.python.util.JLineConsole;

public final class JythonInterface {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		InteractiveConsole rc = null;
		try {
			ConsoleReader cr = new ConsoleReader();
			System.out.print("Press enter to start gamesman-jython!\n");
			if(cr.readLine() != null) //eclipse doesn't work with jline
				rc = new JLineConsole();
		} catch(Error e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//if jline isn't working for some reason
		rc = new InteractiveConsole() {
			@Override
			public String raw_input(PyObject prompt) {
				return super.raw_input(prompt);
			}
		};
		//this will let us put .py files in the junk directory, and things will just work =)
		rc.exec(String.format("import sys; sys.path.append('%s/%s'); sys.path.append('%s/../%s');", System.getProperty("user.dir"), "junk", System.getProperty("user.dir"), "junk"));
		rc.interact();
	}

}
