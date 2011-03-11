package edu.berkeley.gamesman.core;

/**
 * All game states must implement the State interface. This is so Solvers can
 * save time by reusing states rather than having to allocate new memory
 * 
 * @author dnspies
 * 
 */
public interface State {
	/**
	 * Sets this State so that this.equals(s) returns true
	 * 
	 * @param s
	 *            Another state
	 */
	public void set(State s);
}
