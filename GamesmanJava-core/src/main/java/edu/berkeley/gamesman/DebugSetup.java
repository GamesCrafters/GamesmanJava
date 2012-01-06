package edu.berkeley.gamesman;

import java.util.EnumSet;
import java.util.Properties;

import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * A special class for setting up debugging options without loading any other
 * gamesman packages first. This is important since debugging options may enable
 * asserts for certain packages.
 * 
 * @author dnspies
 */
public class DebugSetup {
	/**
	 * @param props
	 *            A Properties object to obtain debugging information from.
	 *            Ideally this same object will later be used to create the
	 *            Configuration.
	 */
	public static void setup(Properties props) {
		EnumSet<DebugFacility> debugOpts = EnumSet.noneOf(DebugFacility.class);
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		for (DebugFacility f : DebugFacility.values()) {
			if (parseBoolean(props.getProperty(
					"gamesman.debug." + f.toString(), "false"))) {
				debugOpts.add(f);
				f.setupClassloader(cl);
			}
		}
		if (!debugOpts.isEmpty()) {
			debugOpts.add(DebugFacility.CORE);
			DebugFacility.CORE.setupClassloader(cl);
			Util.enableDebuging(debugOpts);
		}
	}

	// This is a copy of Util.parseBoolean().
	// It needs to be here to avoid loading the Util class before we're ready to
	private static boolean parseBoolean(String s) {
		return s != null && !s.equalsIgnoreCase("false")
				&& !s.equalsIgnoreCase("0");
	}
}
