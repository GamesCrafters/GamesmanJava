/**
 * 
 */
package edu.berkeley.gamesman;

import com.sun.tools.javac.util.FatalError;

/**
 * Various utility functions accessible from any class
 * 
 * @author Steven Schlansker
 *
 */
public final class Util {
	
	private Util() {}

	protected static class AssertionFailedError extends Error {
		private static final long serialVersionUID = 2545784238123111405L;
	}
	
	protected static class FatalError extends Error {
		private static final long serialVersionUID = -5642903706572262719L;
	}
	
	/**
	 * Throws a fatal Error if a required condition is not satisfied
	 * @param b The boolean (expression) that must be true
	 */
	public static void assertTrue(boolean b){
		if(!b){
			System.err.println("Assertion failed: backtrace forthcoming");
			throw new AssertionFailedError();
		}
	}
	
	public static void fatalError(String s){
		System.err.println("FATAL: "+s);
		System.err.println("Stack trace follows:");
		throw new FatalError();
	}
	
	public static void debug(String s){
		System.err.println("DEBUG: "+s);
	}
	
}
