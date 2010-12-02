package edu.berkeley.gamesman.util;


/**
 * Enumeration of the available debug facilities
 * Debug facilities are selected and then used to filter
 * out unneeded/unwanted debug statements.
 * 
 * By default nothing is printed; each enabled facility adds
 * additional debug output. If you want to be able to call
 * Util.debug() of a certain DebugFacility in a given class,
 * you must first add that given class to the constructor of the
 * desired DebugFacility. 
 * 
 * @author Steven Schlansker
 * @author Jeremy Fleischman
 */
public enum DebugFacility {
	/**
	 * Enable all debug facilities
	 */
	ALL(null),
	/**
	 * Debug facility for core classes (that don't fall under other facilities)
	 */
	CORE(null, "edu.berkeley.gamesman.GamesmanMain", "edu.berkeley.gamesman.util.Util"),
	/**
	 * Debug facility for Databases
	 */
	DATABASE("edu.berkeley.gamesman.database","edu.berkeley.gamesman.database.util"),
	/**
	 * Debug facility for the Filer classes
	 */
	FILER("edu.berkeley.gamesman.database.filer"),
	/**
	 * Debug facility for games
	 */
	GAME("edu.berkeley.gamesman.game"),
	/**
	 * Debug facility for all Hashers
	 */
	HASHER("edu.berkeley.gamesman.hasher", "edu.berkeley.gamesman.core.TieredHasher"),
	/**
	 * Debug facility for Hadoop-related classes
	 */
	HADOOP("edu.berkeley.gamesman.hadoop"),
	/**
	 * Debug facility for all Solvers
	 */
	SOLVER("edu.berkeley.gamesman.solver"),
	/**
	 * Debug facility for all Masters
	 */
	MASTER("edu.berkeley.gamesman.master"),
	/**
	 * Debug facility for local-machine multithreading
	 */
	THREADING("edu.berkeley.gamesman.solver"),
	/**
	 * Debug facility for bitwise manipulation functions
	 */
	BITWISE(null, "edu.berkeley.gamesman.core.Record"),
	/**
	 * Debug facility for Record class
	 */
	RECORD(null, "edu.berkeley.gamesman.core.Record"),
	/**
	 * Debug facility for the JSON interface
	 */
	JSON(null, "edu.berkeley.gamesman.JSONInterface");
	
	private final String[] enabledClasses;
	private final String enabledPackage;
	private DebugFacility(String pack, String... classes) {
		enabledPackage = pack;
		enabledClasses = classes;
	}
	
	/**
	 * Enables assertions in the packages and classes that use this DebugFacility.
	 * This must be called before the class is loaded by cl, because it is impossible
	 * to enable/disable assertions once a class has been loaded.
	 * @param cl The classloader.
	 */
	public void setupClassloader(ClassLoader cl) {
		if(this == ALL) {
			cl.setDefaultAssertionStatus(true);
			return;
		}
		if(enabledPackage != null)
			cl.setPackageAssertionStatus(enabledPackage, true);
		for(String cls : enabledClasses)
			cl.setClassAssertionStatus(cls, true);
	}
}
