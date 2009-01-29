package edu.berkeley.gamesman.util;

import java.math.BigInteger;

/**
 * A simple callback interface to display progress
 * @author Steven Schlansker
 */
public interface ProgressMeter {
	/**
	 * Inform the designated progress meter that you have completed some work.
	 * @param completed The number of work units completed
	 * @param total The total number of work units expected
	 */
	public void progress(BigInteger completed, BigInteger total);
}
