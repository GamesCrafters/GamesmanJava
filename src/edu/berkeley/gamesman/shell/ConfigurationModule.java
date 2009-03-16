package edu.berkeley.gamesman.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import edu.berkeley.gamesman.GamesmanShell;
import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Util;

public class ConfigurationModule extends UIModule {
	private static File confFile;
	private static ArrayList<Configuration> confList;
	
	public ConfigurationModule(Configuration c) {
		super(c, "conf");
		
		requiredPropKeys = new ArrayList<String>();
		helpLines = new Properties();
		
		helpLines.setProperty("loadConfiguration", 
				"Load a configuration from the gamesman-java.conf file.");
		helpLines.setProperty("loadConfiguration.args", "(nameStart)");
		helpLines.setProperty("loadConfiguration.arg0",
				"nameStart\tA unique beginning for a configuration name.");
		
		helpLines.setProperty("newConfiguration", 
				"Create a new configuration to be added to the gamesman-java.conf file.");
		
		helpLines.setProperty("viewConfigurations",
				"List all the configurations available from the gamesman-java.conf file.");
		
		confList = new ArrayList<Configuration>();
		
		if (confFile == null)
			confFile = new File("gamesman-java.conf");
		if (confFile.exists())
			loadConfigurationsFromFile();
		else {
			System.out.println("No gamesman-java.conf file found.\nYou can " +
					"create this file using \"save\" in the conf module\nor it " +
					"will be created automatically when you exit.");
			createDefaultConfiguration();
		}
	}
	
	public void quit() {
		proccessCommand("s");
	}
	
	protected void u_loadConfiguration(ArrayList<String> args) {
		String confName = null;
		if (!args.isEmpty()) {
			confName = args.get(0);			
			ArrayList<Configuration> matchingConfs = new ArrayList<Configuration>();
			for (Configuration conf : confList)
				if (conf.getProperty("conf.name", "").toLowerCase().startsWith(confName.toLowerCase()))
					matchingConfs.add(conf);
			if (matchingConfs.size() > 1) {
				System.out.println("You did not specify a unique configuration name.");
				System.out.print("Possible matches are:\t");
				for (Configuration conf : matchingConfs)
					System.out.print(conf.getProperty("conf.name", "NONAME") + "\t");
				System.out.println();
			}
			else if (matchingConfs.size() == 1)
				GamesmanShell.setConfiguration(matchingConfs.get(0));
			else
				System.out.println("No configuration beginning with " + confName + " found.");
		}
		else
			System.out.println("You did not specify a configuration to load.");
	}
	
	protected void u_newConfiguration(ArrayList<String> args) {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Properties newConfProps = new Properties();
		try {
			String name = null;
			boolean uniqueName = false;
						
			while (!uniqueName) {
				System.out.print("Enter a name for the new configuration: ");
				name = in.readLine().trim();
				uniqueName = true;
				for (Configuration c : confList) {
					if (c.getProperty("conf.name").startsWith(name)) {
						System.out.println("A configuration beginning with \"" + name + "\" already exists.");
						uniqueName = false;
					}
					else if (name.startsWith(c.getProperty("conf.name"))) {
						System.out.println("The configuration \"" + c.getProperty("conf.name") + "\"'s name begins \"" + name + "\".");
						uniqueName = false;
					}
				}					
			}
			newConfProps.setProperty("conf.name", name);
			
			System.out.print("Enter a default module name: ");
			newConfProps.setProperty("conf.defaultuimodule", in.readLine().trim());
			
			String key = "", val = "";
			
			while (true) {
				System.out.print("Enter a property key (\"gamesman.\" is assumed) (enter \"end\" to stop): ");
				key = "gamesman." + in.readLine().trim();
				if (!key.equals("gamesman.end")) {					
					System.out.print("Enter a corresponding value for " + key + ": ");
					val = in.readLine().trim();
					newConfProps.setProperty(key, val);
				}
				else
					break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		confList.add(new Configuration(newConfProps, true));		
	}
	
	protected void u_editConfigurations(ArrayList<String> args) {
		
		// DUPLICATE CODE WITH LOADCONF
		String confName = null;
		Configuration matchingConf = null;
		if (!args.isEmpty()) {
			confName = args.get(0);			
			ArrayList<Configuration> matchingConfs = new ArrayList<Configuration>();
			for (Configuration conf : confList)
				if (conf.getProperty("conf.name", "").toLowerCase().startsWith(confName.toLowerCase()))
					matchingConfs.add(conf);
			if (matchingConfs.size() > 1) {
				System.out.println("You did not specify a unique configuration name.");
				System.out.print("Possible matches are:\t");
				for (Configuration conf : matchingConfs)
					System.out.print(conf.getProperty("conf.name", "NONAME") + "\t");
				System.out.println();
				return;
			}
			else if (matchingConfs.size() == 1) {
				confName = matchingConfs.get(0).getProperty("conf.name");
				matchingConf = matchingConfs.get(0);
			}
			else {
				System.out.println("No configuration beginning with " + confName + " found.");
				return;
			}
		}
		else {
			confName = conf.getProperty("conf.name");
			matchingConf = conf;
		}
		
		System.out.println("Editing \"" + confName + "\" configuration.");	
				
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String com = "";
		while (!com.toLowerCase().startsWith("end")) {
			System.out.print("Enter a command (addProperty, deleteProperty, " +
					"changeProperty, viewProperties, end): ");
			try {
				com = in.readLine().trim();
			
				if ("addproperty".startsWith(com.toLowerCase())) {
					System.out.print("Enter a property key to be added (\"gamesman.\" is assumed): ");
					String key = "gamesman." + in.readLine().trim();
					if (!key.equals("gamesman.end")) {					
						System.out.print("Enter a corresponding value for " + key + ": ");
						String val = in.readLine().trim();
						matchingConf.setProperty(key, val);
					}					
				}
				else if ("deleteproperty".startsWith(com.toLowerCase())) {
					System.out.print("Enter a property key to be deleted (\"gamesman.\" is assumed): ");
					String key = "gamesman." + in.readLine().trim();
					matchingConf.deleteProperty(key);				
				}
				else if ("changeproperty".startsWith(com.toLowerCase())) {
					System.out.print("Enter a property key whose value is to be changed (\"gamesman.\" is assumed): ");
					String key = "gamesman." + in.readLine().trim();
					if (!key.equals("gamesman.end")) {					
						System.out.print("Enter a new value for " + key + ": ");
						String val = in.readLine().trim();
						matchingConf.setProperty(key, val);
					}					
				}
				else if ("viewproperties".startsWith(com.toLowerCase())) {
					proccessCommand("v(" + confName + ")");				
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}	
	}
	
	protected void u_viewConfigurations(ArrayList<String> args) {
		for (Configuration c : confList) {
			if (args.isEmpty() || c.getProperty("conf.name").startsWith(args.get(0))) {
				System.out.println("---------");
				System.out.println("name = " + c.getProperty("conf.name"));
				System.out.println("default module = " + c.getProperty("conf.defaultuimodule"));
				for (Object oKey : c.getKeys()) {
					String key = (String)oKey;
					if (!key.equals("conf.name") && !key.equals("conf.defaultuimodule"))
						System.out.println(key + " = " + c.getProperty(key));
				}
				System.out.println("---------");
			}
		}
	}
	
	protected void u_saveConfigurations(ArrayList<String> args) {
		System.out.println("Saving all configurations to gamesman-java.conf file.");
		confFile.delete();
		confFile = new File("gamesman-java.conf");
		for (Configuration c : confList) {
			addConfigurationToFile(c);
		}
		System.out.println("Done saving.");
	}
	
	private void addConfigurationToFile(Configuration c) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(confFile, true));
			
			out.println("{");
			for (Object oPropKey : c.getKeys()) {
				String propKey = (String)oPropKey;
				String value = c.getProperty(propKey);
				out.println(propKey + " = " + value);
			}
			out.println("}");
				
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadConfigurationsFromFile() {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(confFile));
		} catch (FileNotFoundException e) {
			Util.fatalError("gamesman-java.conf file not found and unable to be created.");
		}
		
		String line;
		ArrayList<String> propStrings = null;
		Configuration conf = null;
		
		try {
			line = in.readLine();
			if (line != null) {
				do {
					if (line.trim().length() != 0) {
						if (line.charAt(0) == '{') {
							conf = new Configuration(new Properties(), true);
							propStrings = new ArrayList<String>();
						}
						else if (line.charAt(0) == '}') {
							conf.addProperties(propStrings);
							confList.add(conf);
						}
						else
							propStrings.add(line);
					}
				} while ((line = in.readLine()) != null);
			}
			in.close();
		} catch (IOException e) {
			Util.fatalError("I/O error while reading from .conf file");
		}
		
		if (confList.isEmpty()) {
			System.out.println("No configurations found in gamesman-java.conf.");
			createDefaultConfiguration();			
		}
	}
	
	private void createDefaultConfiguration() {
		System.out.println("Creating default configuration with no properties but name and default module.");
		Properties defConfProps = new Properties();
		defConfProps.setProperty("conf.name", "default");
		defConfProps.setProperty("conf.defaultuimodule", "conf");
		confList.add(new Configuration(defConfProps, true));
		proccessCommand("s");
	}
}
