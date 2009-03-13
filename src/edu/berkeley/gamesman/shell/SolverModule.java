package edu.berkeley.gamesman.shell;

import java.util.ArrayList;

import edu.berkeley.gamesman.GamesmanMain;
import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.util.Util;

public class SolverModule extends UIModule {
	public SolverModule(Configuration c) {
		super(c, "solver");
		requiredPropKeys = new ArrayList<String>();
		requiredPropKeys.add("gamesman.game");
		requiredPropKeys.add("gamesman.hasher");
		requiredPropKeys.add("gamesman.solver");
		requiredPropKeys.add("gamesman.database");
		
	}
	
	protected void u_solve(ArrayList<String> args) {
		GamesmanMain main = new GamesmanMain();
		Game<?> g = Util.typedInstantiateArg("edu.berkeley.gamesman.game."+conf.getProperty("gamesman.game"), conf);
		Hasher<?> h = Util.typedInstantiateArg("edu.berkeley.gamesman.hasher."+conf.getProperty("gamesman.hasher"), conf);
		conf.initialize(g, h);
		main.run(conf);
	}

}
