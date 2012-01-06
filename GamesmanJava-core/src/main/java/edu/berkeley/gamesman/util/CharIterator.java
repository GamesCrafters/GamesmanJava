package edu.berkeley.gamesman.util;

/**
 * An iterator for the base type char. This is better than Iterator<Character>
 * because wrappers don't need need to be constantly instantiated.
 * 
 * @author dnspies
 */
public interface CharIterator {

	/**
	 * @return Whether there are more characters present
	 */
	public boolean hasNext();

	/**
	 * @return The next character in the sequence
	 */
	public char next();

}
