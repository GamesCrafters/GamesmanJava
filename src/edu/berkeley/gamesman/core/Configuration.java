package edu.berkeley.gamesman.core;

/**
 * A Configuration object stores information related to a specific configuration of Game, Hasher, and Records.  
 * The information should be specific enough that a database will only match Configuration if the given Game and Hasher 
 * will derive useful information from it.
 * @author Steven Schlansker
 */
public class Configuration implements Comparable<Configuration> {
	private String config;
	/**
	 * Initialize a new Configuration.  Both parameters should be fully initialized.
	 * @param g The game used
	 * @param h The hasher used
	 */
	public Configuration(Game<?> g, Hasher<?> h) {
		config = g.describe();
		config = config.length()+config+";"+h.describe();
	}
	
	/**
	 * Load a saved Configuration from a String.
	 * @param config The serialized Configuration
	 */
	public Configuration(String config) {
		this.config = config;
	}

	/**
	 * Return a serialized version of the Configuration suitable for storing persistently
	 * @return a String with the Configuration information
	 */
	public String getConfigString(){
		return config;
	}

	public int compareTo(Configuration o) {
		return config.compareTo(o.config);
	}
	

}
