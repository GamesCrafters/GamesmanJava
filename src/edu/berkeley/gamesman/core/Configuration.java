package edu.berkeley.gamesman.core;

public class Configuration implements Comparable<Configuration> {
	private String config;
	public Configuration(Game<?, ?> g, Hasher<?> h) {
		config = g.describe();
		config = config.length()+config+";"+h.describe();
	}
	
	public Configuration(String config) {
		this.config = config;
	}

	public String getConfigString(){
		return config;
	}

	public int compareTo(Configuration o) {
		return config.compareTo(o.config);
	}
	

}
