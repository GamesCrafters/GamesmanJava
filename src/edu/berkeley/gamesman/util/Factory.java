package edu.berkeley.gamesman.util;

/**
 * This is a generic factory class to be used as an alternative Constructor<T>
 * in the reflection class
 * 
 * @author dnspies
 * 
 * @param <T>
 *            The type to be instantiated
 */
public interface Factory<T> {
	/**
	 * @return A new element of type T
	 */
	T newElement();
}
