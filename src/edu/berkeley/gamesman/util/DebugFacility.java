package edu.berkeley.gamesman.util;

/**
 * Enumeration of the available debug facilities
 * Debug facilities are selected and then used to filter
 * out unneeded/unwanted debug statements.
 * 
 * By default nothing is printed; each enabled facility adds
 * additional debug output.
 * @author Steven Schlansker
 */
public enum DebugFacility {
	/**
	 * Enable all debug facilities
	 */
	ALL,
	/**
	 * Debug facility for core classes (that don't fall under other facilities)
	 */
	CORE,
	/**
	 * Debug facility for Databases
	 */
	DATABASE,
	/**
	 * Debug facility for local-machine multithreading
	 */
	THREADING,
	/**
	 * Debug facility for games
	 */
	GAME,
	/**
	 * Debug facility for all Hashers
	 */
	HASHER,
	/**
	 * Debug facility for Hadoop-related classes
	 */
	HADOOP,
	/**
	 * Debug facility for all Solvers
	 */
	SOLVER,
	/**
	 * Debug facility for all Masters
	 */
	MASTER,
	/**
	 * Debug facility for bitwise manipulation functions
	 */
	BITWISE,
	/**
	 * Debug facility for Record class
	 */
	RECORD,
	/**
	 * Debug facility for the Filer classes
	 */
	FILER,
	/**
	 * Debug facility for the JSON interface
	 */
	JSON
}
