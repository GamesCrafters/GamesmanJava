package edu.berkeley.gamesman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.prefs.Preferences;

import org.python.core.PyObject;
import org.python.util.InteractiveConsole;
import org.python.util.JLineConsole;
import org.python.util.ReadlineConsole;

import edu.berkeley.gamesman.util.Util;

public final class JythonInterface {
	
	private enum Consoles {
		dumb,readline,jline
	}

	/**
	 * @param args
	 * @throws IOException something bad happened
	 */
	public static void main(String[] args) throws Exception {
		Preferences prefs = Preferences.userNodeForPackage(JythonInterface.class);
		
		String consoleName = prefs.get("console", null);
		Consoles console = null;
		
		while(console == null){
			if(consoleName == null){
				System.out.print("What console would you like to use? "+Arrays.toString(Consoles.values())+": ");
				System.out.flush();
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				consoleName = br.readLine();
			}
			try {
				console = Consoles.valueOf(consoleName);
			} catch(IllegalArgumentException e) {
				System.out.println(consoleName + " isn't one of " + Arrays.toString(Consoles.values()));
				consoleName = null;
			}
		}
		prefs.put("console", consoleName);
		
		InteractiveConsole rc = null;
		switch(console){
		case dumb:
			rc = new InteractiveConsole() {
				@Override
				public String raw_input(PyObject prompt) {
					//eclipse sometimes interleaves the error messages 
					exec("import sys; sys.stderr.flush()");
					return super.raw_input(prompt);
				}
			};
			break;
		case jline:
			rc = new JLineConsole();
			break;
		case readline:
			rc = new ReadlineConsole();
			break;
		default:
			Util.fatalError("Need to pick a console");
		}
		
		//this will let us put .py files in the junk directory, and things will just work =)
		rc.exec("import sys");
		addpath(rc, "jython_lib");
		addpath(rc, "junk");
		addpath(rc, "jobs");
	
		rc.exec("from Play import *");
		rc.interact();
	}
	
	private static void addpath(InteractiveConsole ic, String what){
		String ud = System.getProperty("user.dir");
		ic.exec(String.format("sys.path.append('%s/%s'); sys.path.append('%s/../%s');",
				ud, what, ud, what));
	}

}
