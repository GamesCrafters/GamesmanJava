package edu.berkeley.gamesman.util;

/**
 * A TaskFactory is used to instantiate Tasks, which are the main way of reporting progress
 * back to the controlling process.
 * @author Steven Schlansker
 */
public interface TaskFactory {

	/**
	 * Begin a new Task
	 * @param name The name of the process
	 * @return a new Task
	 */
	public Task createTask(String name);
	
}
