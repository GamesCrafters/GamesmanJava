package edu.berkeley.gamesman;

import java.io.FileNotFoundException;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;

public class JSONInterfaceRunner {
	public static void main(String[] args) throws FileNotFoundException {
		Properties props = Configuration.readProperties(args[0]);
		DebugSetup.setup(props);
		JSONInterface iface = new JSONInterface(props);
		iface.run();
	}
}
