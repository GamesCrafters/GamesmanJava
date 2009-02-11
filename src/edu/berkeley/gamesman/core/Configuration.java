package edu.berkeley.gamesman.core;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Properties;

import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A Configuration object stores information related to a specific configuration of Game, Hasher, and Records.  
 * The information should be specific enough that a database will only match Configuration if the given Game and Hasher 
 * will derive useful information from it.
 * @author Steven Schlansker
 */
public class Configuration implements Serializable {
	private static final long serialVersionUID = -5331459097835638972L;
	private String config;
	private Game<?> g;
	private Hasher<?> h;
	private EnumMap<RecordFields,Pair<Integer,Integer>> storedFields;
	
	private Properties props;
	/**
	 * Initialize a new Configuration.  Both parameters should be fully initialized.
	 * @param props The properties used to configure options
	 * @param g The game used
	 * @param h The hasher used
	 * @param storedFields Which fields should be stored in the database
	 */
	public Configuration(Properties props,Game<?> g, Hasher<?> h, EnumMap<RecordFields,Pair<Integer,Integer>> storedFields) {
		this.props = props;
		this.g = g;
		this.h = h;
		this.storedFields = storedFields;
		buildConfig();
		checkCompatibility();
	}
	
	/**
	 * @return the Game this configuration plays
	 */
	public Game<?> getGame(){
		return g;
	}
	
	public void setGame(Game<?> g){
		this.g = g;
	}
	
	public void setHasher(Hasher<?> h){
		this.h = h;
	}
	
	public void setStoredFields(EnumMap<RecordFields,Pair<Integer,Integer>> sf){
		storedFields = sf;
	}
	
	public void setStoredFields(EnumSet<RecordFields> set){
		int i = 0;
		EnumMap<RecordFields,Pair<Integer, Integer>> map = new EnumMap<RecordFields, Pair<Integer,Integer>>(RecordFields.class);
		for(RecordFields rec : set){
			map.put(rec, new Pair<Integer, Integer>(i++,rec.defaultBitSize()));
		}
		storedFields = map;
	}
	
	private void checkCompatibility() {
		if(!DependencyResolver.isHasherAllowed(g.getClass(), h.getClass()))
			Util.fatalError("Game and hasher are not compatible!");
	}

	/**
	 * Create a new Configuration
	 * @param props The properties used to configure options
	 * @param g The game we're playing
	 * @param h The hasher to use
	 * @param set Which records to save
	 */
	public Configuration(Properties props,Game<?> g, Hasher<?> h, EnumSet<RecordFields> set){
		this.props = props;
		int i = 0;
		EnumMap<RecordFields,Pair<Integer, Integer>> map = new EnumMap<RecordFields, Pair<Integer,Integer>>(RecordFields.class);
		for(RecordFields rec : set){
			map.put(rec, new Pair<Integer, Integer>(i++,rec.defaultBitSize()));
		}
		this.g = g;
		this.h = h;
		this.storedFields = map;
		buildConfig();
		checkCompatibility();
	}
	
	public Configuration(Properties props2) {
		props = props2;
	}

	private void buildConfig(){
		config = Util.pstr(g.describe()) + Util.pstr(h.describe()) + Util.pstr(storedFields.toString());
	}
	
	/**
	 * Load a saved Configuration from a String.
	 * @param config The serialized Configuration
	 * @return the Configuration
	 */
	
	public static Configuration configurationFromString(String config){
		return configurationFromBytes(config.getBytes());
	}
	
	/**
	 * Unserialize a configuration from a bytestream
	 * @param barr Bytes to deserialize
	 * @return a Configuration
	 */
	public static Configuration configurationFromBytes(byte[] barr){
		return deserialize(barr);
	}
	//public Configuration(String config) {
	//	this.config = config;
	//	this.g = null;
	//	this.h = null;
	//	this.storedFields = null;
	//}
	

	/**
	 * Return a serialized version of the Configuration suitable for storing persistently
	 * @return a String with the Configuration information
	 */
	public String getConfigString(){
		return new String(serialize());
		//return config;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Configuration)) return false;
		return ((Configuration)o).config.equals(config);
	}
	
	/**
	 * @return the records available from a database using this Configuration
	 */
	public EnumMap<RecordFields,Pair<Integer,Integer>> getStoredFields(){
		return storedFields;
	}

	/**
	 * @return the Hasher this Configuration is using
	 */
	public Hasher<?> getHasher() {
		return h;
	}

	/**
	 * @param bytes A bytestream
	 * @return the Configuration represented by that bytestream
	 */
	public static Configuration deserialize(byte[] bytes) {
		return Util.deserialize(bytes);
	}

	/**
	 * @return a bytestream representing this Configuration
	 */
	public byte[] serialize() {
		return Util.serialize(this);
	}
	
	public String toString(){
		return config;
		//return new String(serialize());
	}
	
	/**
	 * Get a property by its name
	 * @param key the name of the configuration property
	 * @return its value
	 */
	public String getProperty(String key){
		String s = props.getProperty(key);
		if(s == null)
			Util.fatalError("Property "+key+" is unset and has no default!");
		return s;
	}
	
	/**
	 * Get a property by its name.  If the property is not set,
	 * return dfl
	 * @param key the name of the configuration property
	 * @param dfl default value
	 * @return its value
	 */
	public String getProperty(String key,String dfl){
		return props.getProperty(key,dfl);
	}
	
	/**
	 * Set a property by its name
	 * @param key the name of the configuration property to set
	 * @param value the new value
	 * @return the old value
	 */
	public Object setProperty(String key, String value){
		return props.setProperty(key, value);
	}
	

}
