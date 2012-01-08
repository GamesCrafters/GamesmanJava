package edu.berkeley.gamesman;

import java.io.IOException;
import java.util.Properties;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;

import edu.berkeley.gamesman.core.Configuration;

public class HServerMain {
	public static void main(String[] args) throws IOException {
		GenericOptionsParser parser = new GenericOptionsParser(args);
		org.apache.hadoop.conf.Configuration conf = parser.getConfiguration();
		Properties props = Configuration.readProperties(parser
				.getRemainingArgs()[0]);
		DebugSetup.setup(props);
		String dbPath = props.getProperty("json.databasedirectory");
		HOpener ho = new HOpener(conf, new Path(dbPath));
		JSONInterface.addOpener("ttt", ho);
		JSONInterface iface = new JSONInterface(props);
		iface.run();
	}
}
