package edu.berkeley.gamesman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import edu.berkeley.gamesman.shell.*;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.core.Configuration;

/**
 * The main class that accepts commands and passes them off to UI modules. It also handles
 * reading in the configuration file.
 * @author Wesley Hart
 */
public class GamesmanShell {
	private static UIModule curModule;
	private static Configuration curConf;

	/**
	 * Main entry point
	 * @param args Program arguments
	 */
	public static void main(String args[]) {	
		curModule = new ConfigurationModule(curConf);
		curModule.proccessCommand("load(default)");
		
		if (args.length > 0)
			proccessCommands(args);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String com = "";
		while (!com.equals("exit")) { // SAYS INVALID COMMAND IF PUT EXIT
			System.out.print("gamesman/" + curConf.getProperty("conf.name", "NOCONF") 
					+ "/" + curModule.getModuleName() + "> ");
			System.out.flush();
			try {
				com = in.readLine();
			} catch (IOException e) {
				Util.fatalError("I/O error while reading from console", e);
			}
			com = com.trim();
			proccessCommands(com.split(" "));
		}
	}
	
	private static void proccessCommands(String[] coms) {
		for (int i = 0; i < coms.length; i++) {
			if (coms[i].equals("cm")) {
				i++;
				changeModule(coms[i]);
			}
			else if ("help".startsWith(coms[i]))
				curModule.getHelp();
			else
				curModule.proccessCommand(coms[i]);
		}
	}
	
	public static void setConfiguration(Configuration conf) {
		curConf = conf;
		System.out.println("Configuration changed to \"" + curConf.getProperty("conf.name")
				+ "\".");
		System.out.println("Switching to \"" + curConf.getProperty("conf.name")
				+ "\" configuration's default module, " 
				+ curConf.getProperty("conf.defaultuimodule") + ".");
		changeModule(curConf.getProperty("conf.defaultuimodule"));		
	}
	
	public static void changeModule(String name) {
		ArrayList<Class<? extends UIModule>> moduleClasses = new ArrayList<Class<? extends UIModule>>();
		
		// hardcoded - insert all module classes here
		moduleClasses.add(ConfigurationModule.class);
		moduleClasses.add(DatabaseModule.class);
		moduleClasses.add(SolverModule.class);
		
		for (Class<? extends UIModule> moduleClass : moduleClasses) {
			if (moduleClass.getName().toLowerCase().startsWith("edu.berkeley.gamesman.shell." + name.toLowerCase())) {
				try {
					curModule = (UIModule)moduleClass.getDeclaredConstructors()[0].newInstance(curConf);
					ArrayList<String> missingPropKeys = curModule.missingPropertyKeys();
					if (!missingPropKeys.isEmpty()) {
						System.out.println("The current configuration does not support the " 
								+ curModule.getModuleName() + " module. It is missing these properties: ");
						for (String missingPropKey : missingPropKeys)
							System.out.println(missingPropKey);
						System.out.println("Changing to conf module.");
						GamesmanShell.changeModule("conf");
					}
					return;
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
		System.out.println("No module beginning with " + name + " exists.");
	}
}