package edu.berkeley.gamesman.core;

import java.math.BigInteger;


/**
 * A Database is the abstract superclass of all data storage methods used in Gamesman.  
 * Each particular Database is responsible for the persistent storage of Records derived from solving 
 * games.
 * @author Steven Schlansker
 */
public abstract class Database {

	protected Configuration conf;
	
	/**
	 * Initialize a Database given a URI and a Configuration.  This method may either open an existing database or 
	 * create a new one.  If a new one is created, the Configuration should be stored.  If one is opened, the Configuration 
	 * should be checked to ensure it matches that already stored.  This method must be called exactly once before any other methods are called.
	 * The URI must be in the URI syntax, ex: file:///absolute/path/to/file.db or gdfp://server:port/dbname
	 * If config is null, it will use whatever is in the database.  It is recommended that you pass in the
	 * configuration that you are expecting to ensure you don't load a db for a different game.
	 * @param uri The URI that the Database is associated with
	 * @param config The Configuration that is relevant
	 */
	public final void initialize(String uri, Configuration config){
		conf = config;
		initialize(uri);
	}
	
	protected abstract void initialize(String uri);
	
	/**
	 * Return the Nth Record in the Database
	 * @param loc The record number
	 * @return The stored Record
	 */
	public abstract Record getRecord(BigInteger loc);
	/**
	 * Store a record in the Database
	 * @param loc The record number
	 * @param value The Record to store
	 */
	public abstract void getRecord(BigInteger loc, Record value);
	
	/**
	 * Ensure all buffers are flushed to disk.  The on-disk state should be consistent after this call returns.
	 */
	public abstract void flush();
	/**
	 * Close this Database, flush to disk, and release all associated resources.  This object should not be used again after making this call.
	 */
	public abstract void close();
	
	/**
	 * Retrieve the Configuration associated with this Database.
	 * @return the Configuration stored in the database
	 */
	public final Configuration getConfiguration(){
		return conf;
	}
	
}
