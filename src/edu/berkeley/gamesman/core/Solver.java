package edu.berkeley.gamesman.core;


public abstract class Solver {

	protected Database db;
	
	public void setDatabase(Database db){
		this.db = db;
	}
	
	public abstract WorkUnit prepareSolve(Game<?, ?> game);
	
}
