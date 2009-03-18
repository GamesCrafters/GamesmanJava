package edu.berkeley.gamesman.shell;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collections;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Util;

/**
 * A UIModule is an interface object that given commands from a user performs
 * specific actions related to its type.
 * @author Wesley Hart
 */
public abstract class UIModule {
	protected String name;
	protected ArrayList<String> requiredPropKeys;
	protected Properties helpLines;
	protected Configuration conf;
	
	public UIModule(Configuration c, String n) {
		conf = c;
		name = n;
	}
	
	/**
	 * Outputs all of the commands available to the user for the current module
	 * and their descriptions.
	 */
	public void getHelp() {
		System.out.println("Commands for " + name + " module:");
		Method[] methods = this.getClass().getDeclaredMethods();
		ArrayList<String> methodNames = new ArrayList<String>();
		for (Method method : methods) {
			String name = method.getName();
			if (name.startsWith("u_")) {
				name = name.substring(2);
				methodNames.add(name);
			}
		}
		Collections.sort(methodNames);
		for (String name : methodNames) {
			String args = helpLines.getProperty(name + ".args");
			if (args == null)
				args = "";
			System.out.println(name +  args	+ "\t" + helpLines.getProperty(name));
			if (helpLines.getProperty(name + ".arg0") != null) {
				for (int i = 0; helpLines.getProperty(name + ".arg" + i) != null; i++)
					System.out.println("\t" + helpLines.getProperty(name + ".arg" + i));
			}
		}
		
	}
	
	/**
	 * Calls the corresponding function beginning with u_.
	 * @param command The command to be handled (the name of a u_ function in the module).
	 */
	public void proccessCommand(String command) {
		Method[] methods = this.getClass().getDeclaredMethods();
		for (Method method : methods) {
			String name = method.getName();
			if (name.startsWith("u_")) {
				name = name.substring(2);
				String[] nameAndArgs = command.split("\\(|,|\\)");
				if (name.toLowerCase().startsWith(nameAndArgs[0].toLowerCase())) {
					try {
						ArrayList<String> args = new ArrayList<String>();
						for (int i = 1; i < nameAndArgs.length; i++)
							if (nameAndArgs[i] != "")
								args.add(nameAndArgs[i]);								
						method.invoke(this, args);
					} catch (InvocationTargetException e) {
						Util.fatalError("", e);
					} catch (IllegalAccessException e) {
						Util.fatalError("", e);					
					}
					return;
				}
			}
		}
		System.out.println("Invalid command.");
	}
	
	/**
	 * Loops through the required properties for this module to run all of its
	 * functions and creates a list of the keys missing in the configuration.
	 * @param conf The configuration the module will be run on
	 * @return List of missing keys in the given configuration
	 */
	public ArrayList<String> missingPropertyKeys() {
		ArrayList<String> missingPropKeys = new ArrayList<String>();
		for (String key : requiredPropKeys) {
			if (conf.getProperty(key, null) == null)
				missingPropKeys.add(key);
		}
		return missingPropKeys;		
	}
	
	/**
	 * @return The name of the module to be output to the user.
	 */
	public String getModuleName() {
		return name;
	}
	
	public void quit() {
	}
}