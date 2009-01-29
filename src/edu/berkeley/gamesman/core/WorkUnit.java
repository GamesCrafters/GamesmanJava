package edu.berkeley.gamesman.core;

import java.util.List;

public interface WorkUnit {

	public List<WorkUnit> divide(int num);
	public void conquer();
	
}
