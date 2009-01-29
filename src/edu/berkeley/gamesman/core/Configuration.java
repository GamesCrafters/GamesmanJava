package edu.berkeley.gamesman.core;

public class Configuration {
	private Game<?,?> ga;
	private Hasher<?> ha;
	public Configuration(Game<?, ?> g, Hasher<?> h) {
		ga = g;
		ha = h;
	}
	
	public Game<?,?> getGame(){
		return ga;
	}
	
	public Hasher<?> getHasher(){
		return ha;
	}
}
