package edu.berkeley.gamesman.shell;

import java.util.ArrayList;
import java.util.Properties;

import edu.berkeley.gamesman.GamesmanMain;
import edu.berkeley.gamesman.core.Configuration;

public class SolverModule extends UIModule {
	public SolverModule(Configuration c) {
		super(c, "solver");
		requiredPropKeys = new ArrayList<String>();
		requiredPropKeys.add("gamesman.game");
		requiredPropKeys.add("gamesman.hasher");
		requiredPropKeys.add("gamesman.solver");
		requiredPropKeys.add("gamesman.database");
		helpLines = new Properties();
		helpLines.setProperty("solve", "solve the current game.");
	}
	
	public void quit() {
		super.quit();
	}
	
	protected void u_solve(ArrayList<String> args) {
		proccessCommand("i");
		GamesmanMain main = new GamesmanMain();
		main.run(conf.props);
	}
	
	protected void u_initializeConfiguration(ArrayList<String> args) throws ClassNotFoundException {
		conf.initialize(conf.getProperty("gamesman.game"), conf.getProperty("gamesman.hasher"));
	}

}
