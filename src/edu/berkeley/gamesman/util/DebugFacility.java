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
	All,
	/**
	 * Debug facility for core classes (that don't fall under other facilities)
	 */
	Core,
	/**
	 * Debug facility for Databases
	 */
	Database,
	/**
	 * Debug facility for local-machine multithreading
	 */
	Threading,
	/**
	 * Debug facility for all Hashers
	 */
	Hasher,
	/**
	 * Debug facility for Hadoop-related classes
	 */
	Hadoop,
	/**
	 * Debug facility for all Solvers
	 */
	Solver,
	/**
	 * Debug facility for all Masters
	 */
	Master,
	/**
	 * Debug facility for bitwise manipulation functions
	 */
	Bitwise,
	/**
	 * Debug facility for Record class
	 */
	Record,
	/**
	 * Debug facility for the Filer classes
	 */
	Filer,
	/**
	 * Debug facility for the JSON interface
	 */
	JSON
}
