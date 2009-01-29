package edu.berkeley.gamesman.core;


public interface Master {

	public void initialize(Class<? extends Game<?, ?>> game, Class<? extends Solver> solver, Class<? extends Hasher> hasher, Class<? extends Database> database);
	
	public void run();
	
}
