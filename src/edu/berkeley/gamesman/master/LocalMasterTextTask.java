package edu.berkeley.gamesman.master;

import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.Util;

public class LocalMasterTextTask extends Task {
	private String name;

	public LocalMasterTextTask(String name) {
		this.name = name;
	}

	private long start;

	@Override
	public void begin() {
		start = System.currentTimeMillis();
		System.out.println("Starting task " + name);
	}

	@Override
	public void complete() {
		System.out.println("Completed task " + name + " in "
				+ Util.millisToETA(System.currentTimeMillis() - start) + ".");
	}

	@Override
	public void update() {
		long elapsedMillis = System.currentTimeMillis() - start;
		double fraction = (double) completed / total;
		System.out.println("Task: "
				+ name
				+ ", "
				+ String.format("%4.02f", fraction * 100)
				+ "% ETA "
				+ Util.millisToETA((long) (elapsedMillis / fraction)
						- elapsedMillis) + " remains");
	}
}