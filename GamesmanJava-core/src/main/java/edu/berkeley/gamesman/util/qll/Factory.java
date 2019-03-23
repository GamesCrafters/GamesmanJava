package edu.berkeley.gamesman.util.qll;

/**
 * Factory that produces instances and reset them for use again without having
 * to re-instantiate another instance.
 *
 * @param <T> Type of object
 */
public interface Factory<T> {

	/**
	 * Produce a new instance
	 *
	 * @return New instance
	 */
	T newObject();

	/**
	 * Reset the instance before reusing it
	 *
	 * If the instance has some internal counter, it's a good time to reset it
	 * so the next time when the instance is offered by a pool we can assume it
	 * as a freshly constructed object.
	 *
	 * @param t Instance to reset
	 */
	void reset(T t);

}
