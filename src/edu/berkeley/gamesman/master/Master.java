package edu.berkeley.gamesman.master;

public interface Master {

	public void initialize(Class game, Class solver, Class hasher, Class database);
	
	public void launch();
	
}
