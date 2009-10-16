package edu.berkeley.gamesman.util;

/**
 * An iterator for longs which does not waste time instantiating wrappers
 * 
 * @author dnspies
 */
public interface LongIterator {
	/**
	 * @return Are there any more longs available
	 */
	public boolean hasNext();

	/**
	 * @return The next long available
	 */
	public long next();
}
