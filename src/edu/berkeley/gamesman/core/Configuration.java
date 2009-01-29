package edu.berkeley.gamesman.core;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A Configuration object stores information related to a specific configuration of Game, Hasher, and Records.  
 * The information should be specific enough that a database will only match Configuration if the given Game and Hasher 
 * will derive useful information from it.
 * @author Steven Schlansker
 */
public class Configuration {
	private String config;
	final private Game<?> g;
	final private Hasher<?> h;
	final private EnumMap<RecordFields,Pair<Integer,Integer>> storedFields;
	/**
	 * Initialize a new Configuration.  Both parameters should be fully initialized.
	 * @param g The game used
	 * @param h The hasher used
	 * @param storedFields Which fields should be stored in the database
	 */
	public Configuration(Game<?> g, Hasher<?> h, EnumMap<RecordFields,Pair<Integer,Integer>> storedFields) {
		this.g = g;
		this.h = h;
		this.storedFields = storedFields;
		buildConfig();
		checkCompatibility();
	}
	
	private void checkCompatibility() {
		if(!DependencyResolver.isHasherAllowed(g.getClass(), h.getClass()))
			Util.fatalError("Game and hasher are not compatible!");
	}

	public Configuration(Game<?> g, Hasher<?> h, EnumSet<RecordFields> set){
		int i = 0;
		EnumMap<RecordFields,Pair<Integer, Integer>> map = new EnumMap<RecordFields, Pair<Integer,Integer>>(RecordFields.class);
		for(RecordFields rec : set){
			map.put(rec, new Pair(i++,rec.defaultBitSize()));
		}
		this.g = g;
		this.h = h;
		this.storedFields = map;
		buildConfig();
		checkCompatibility();
	}
	
	private void buildConfig(){
		config = Util.pstr(g.describe()) + Util.pstr(h.describe()) + Util.pstr(storedFields.toString());
	}
	
	/**
	 * Load a saved Configuration from a String.
	 * @param config The serialized Configuration
	 */
	public Configuration(String config) {
		this.config = config;
		this.g = null;
		this.h = null;
		this.storedFields = null;
	}

	/**
	 * Return a serialized version of the Configuration suitable for storing persistently
	 * @return a String with the Configuration information
	 */
	public String getConfigString(){
		return config;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Configuration)) return false;
		return ((Configuration)o).config.equals(config);
	}
	
	public EnumMap<RecordFields,Pair<Integer,Integer>> getStoredFields(){
		return storedFields;
	}

	public Hasher<?> getHasher() {
		return h;
	}
	

}
