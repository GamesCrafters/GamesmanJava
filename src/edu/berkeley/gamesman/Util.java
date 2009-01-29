/**
 * 
 */
package edu.berkeley.gamesman;

/**
 * Various utility functions accessible from any class
 * 
 * @author Steven Schlansker
 *
 */
public final class Util {

	static class AssertionFailedError extends Error {
		private static final long serialVersionUID = 2545784238123111405L;
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
	
}
