package edu.berkeley.gamesman.core;

/**
 * 
 * A Filer is a collection of Databases stored on some persistent medium (usually disk).  
 * The Filer supports opening databases by configuration or by name
 * 
 * @author Steven Schlansker
 *
 * @param <DB> The Database type(s) that the filer interacts with
 */
public abstract class Filer<DB extends Database> {

	/**
	 * List the available databases in this filer.
	 * @return A list of the names of databases available
	 */
	public abstract String[] ls();
	/**
	 * Open a database by name
	 * @param name The name of the requested database
	 * @return a Database if it exists, null otherwise
	 */
	public abstract DB openDatabase(String name);
	/**
	 * Open a database by configuration
	 * @param conf The configuration that the database should match
	 * @return a Database if one satisfies the configuration, null otherwise
	 */
	public abstract DB openDatabase(Configuration conf);
	/**
	 * Close the filer and update all metadata.
	 */
	public abstract void close();
	
}
