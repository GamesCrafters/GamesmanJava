package edu.berkeley.gamesman.util;

/**
 * A Task is a method of relaying progress information back to the user. It is
 * created by the abstract TaskFactory and can represent progress bars, textual
 * output, or even output to a website. The lifecycle should involve creating a
 * task through beginTask, setting the Total number once, and then updating the
 * Progress number every few seconds (depending on the granularity you
 * want/need). When you have completed the task, call complete() and the task
 * may then be garbage collected.
 * 
 * @see TaskFactory
 * @author Steven Schlansker
 */
public abstract class Task {

	private static TaskFactory factory;

	public long completed, total;

	protected Task() {
	}

	/**
	 * Create a new Task with the current TaskFactory. The task is ready to be
	 * interacted with immediately.
	 * 
	 * @param name
	 *            The name of the task (for user display)
	 * @return a new Task with that name
	 */
	public static Task beginTask(String name) {
		if (factory != null)
			return factory.createTask(name);
		return new Task() {
			protected void begin() {
			}

			public void complete() {
			}

			public void update() {
			}
		};
	}

	/**
	 * Sets the TaskFactory used to create new Tasks. Should be called by
	 * whatever controller is managing interaction with the users(s).
	 * 
	 * @param f
	 *            the Factory to use from here on out.
	 */
	public static void setTaskFactory(TaskFactory f) {
		factory = f;
	}

	/**
	 * Set the progress completed for the given Task. This is an absolute value
	 * which should be 0 <= l <= Total
	 * 
	 * @param p
	 *            The progress you have reached
	 */
	public synchronized void setProgress(long p) {
		completed = p;
		update();
	}

	/**
	 * Set the total progress possible for the given Task.
	 * 
	 * @param t
	 *            The total number of work units that this task has
	 */
	public void setTotal(long t) {
		total = t;
		begin();
	}

	protected abstract void begin();

	protected abstract void update();

	/**
	 * Inform the UI that the task has been completed successfully.
	 */
	public abstract void complete();

}
