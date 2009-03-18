package edu.berkeley.gamesman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumSet;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.shell.ConfigurationModule;
import edu.berkeley.gamesman.shell.DatabaseModule;
import edu.berkeley.gamesman.shell.SolverModule;
import edu.berkeley.gamesman.shell.UIModule;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Accepts commands and passes them off to UI modules. Handles the current module
 * and current configuration.
 * @author Wesley Hart
 */
public class GamesmanShell {
	private static UIModule curModule;
	private static Configuration curConf;

	/**
	 * Main entry point
	 * Gets input from the user to pass to the current module or to switch modules.
	 * @param args A list of commands that will be executed as if typed into the shell.
	 */
	public static void main(String args[]) {	
		// The first module defaults to the configuration module
		curModule = new ConfigurationModule(curConf);
		// Load the default configuration
		curModule.proccessCommand("load(default)");
		
		// If arguments were passed in, proccess them
		if (args.length > 0)
			proccessCommands(args);
		
		// Setup for user input
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String com = "";
		
		// Get user input until user types "exit" or "quit"
		while (true) {
			// Prompt
			System.out.print("gamesman/" + curConf.getProperty("conf.name", "NOCONF") 
					+ "/" + curModule.getModuleName() + "> ");
			System.out.flush();
			
			// Get input
			try {
				com = in.readLine();
				com = com.trim();
			} catch (IOException e) {
				Util.fatalError("I/O error while reading from console", e);
			}			
			
			
			if (!com.equals("exit") && !com.equals("quit"))
				// Split input by spaces and proccess as an array of Strings
				proccessCommands(com.split(" "));
			else
				break;
		}
		
		curModule.quit();		
	}
	
	/**
	 * If the command isn't "help" or "cm", then passes the command to the current module.
	 * @param coms An array of commands to be proccessed
	 */
	private static void proccessCommands(String[] coms) {
		// Loop through the string of commands
		for (int i = 0; i < coms.length; i++) {
			// Change module
			if (coms[i].equals("cm")) {
				i++;
				if (i < coms.length)
					changeModule(coms[i], true);
				else
					System.out.println("Incorrect usage of \"cm\": " +
							"did not specify a module to change to.");
			}
			// Get help
			else if ("help".startsWith(coms[i]))
				curModule.getHelp();
			// Pass command to current module
			else
				curModule.proccessCommand(coms[i]);
		}
	}
	
	/**
	 * Changes the current configuration and informs the user. Also changes the current module
	 * to the default module for the new configuration.
	 * @param conf The new current configuration
	 */
	public static void setConfiguration(Configuration conf) {
		// Update configuration
		curConf = conf;
		
		// Setup debug
		EnumSet<DebugFacility> debugOpts = EnumSet.noneOf(DebugFacility.class);
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		cl.setDefaultAssertionStatus(false);
		for(DebugFacility f: DebugFacility.values()){
			if(parseBoolean(curConf.getProperty("gamesman.debug."+f.toString(), "false"))) {
				debugOpts.add(f);
				f.setupClassloader(cl);
			}
		}
		if(!debugOpts.isEmpty()) {
			debugOpts.add(DebugFacility.CORE);
			DebugFacility.CORE.setupClassloader(cl);
		}
		Util.enableDebuging(debugOpts);
		
		// Change module
		changeModule(curConf.getProperty("conf.defaultuimodule"), false);
		
		System.out.println("Changed to configuration \"" + curConf.getProperty("conf.name") 
				+ "\" and " + curConf.getProperty("conf.defaultuimodule") + " module.");
	}
	
	/**
	 * Changes the current module unless the module passed in is already loaded.
	 * @param name The name of the new current module
	 * @param userCalled Whether or not the user instigated this change
	 */
	public static void changeModule(String name, boolean userCalled) {
		// List of all the possible modules (currently hardcoded - ideally could be automatically generated using reflection)
		ArrayList<Class<? extends UIModule>> moduleClasses = new ArrayList<Class<? extends UIModule>>();
		moduleClasses.add(ConfigurationModule.class);
		moduleClasses.add(DatabaseModule.class);
		moduleClasses.add(SolverModule.class);
		
		if (!curModule.getModuleName().startsWith(name.toLowerCase())) {
			// Loop through all the possible modules to find a match
			for (Class<? extends UIModule> moduleClass : moduleClasses) {
				// Compare the module class names with the given name
				if (moduleClass.getName().toLowerCase().startsWith("edu.berkeley.gamesman.shell." + name.toLowerCase())) {
					try {
						// Found a match so exit the current module
						curModule.quit();
						// Get an instance of the new module
						curModule = (UIModule)moduleClass.getDeclaredConstructors()[0].newInstance(curConf);
						
						// Make sure the current configuration supports the new module
						ArrayList<String> missingPropKeys = curModule.missingPropertyKeys();
						// If the current configuration is missing properties, inform the user and change to the configuration module
						if (!missingPropKeys.isEmpty()) {
							System.out.println("The current configuration does not support the " 
									+ curModule.getModuleName() + " module. It is missing these properties: ");
							for (String missingPropKey : missingPropKeys)
								System.out.println(missingPropKey);
							System.out.println("Changing to conf module.");
							GamesmanShell.changeModule("conf", false);
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
		else
			if (userCalled)
				System.out.println(curModule.getModuleName() + " module already running.");
	}
	
	//This is a copy of Util.parseBoolean().
	//It needs to be here to avoid loading the Util class before we're ready to
	private static boolean parseBoolean(String s) {
		return s != null && !s.equalsIgnoreCase("false") && !s.equalsIgnoreCase("0");
	}
}