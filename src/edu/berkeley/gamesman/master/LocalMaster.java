package edu.berkeley.gamesman.master;

import edu.berkeley.gamesman.Util;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.hasher.Hasher;
import edu.berkeley.gamesman.solver.Solver;

public final class LocalMaster implements Master {

	public void initialize(Class<? extends Game<?, ?>> gamec, Class<? extends Solver> solverc, Class<? extends Hasher> hasherc, Class<? extends Database<?>> databasec) {
		Game game = null;
		Solver solver = null;
		Hasher hasher = null;
		Database database = null;
		try{
			game = gamec.newInstance();
			solver = solverc.newInstance();
			hasher = hasherc.newInstance();
			database = databasec.newInstance();
		}catch(IllegalAccessException e){
			Util.fatalError("Fatal error while initializing: "+e);
		}catch (InstantiationException e) {
			Util.fatalError("Fatal error while initializing: "+e);
		}
		
		database.initialize(null);
		
		solver.setDatabase(database);
		
		hasher.setGame(game);
		game.setHasher(hasher);
		
		solver.solve(game);
		
	}
	
	public void run() {
		System.out.println("Launched!");
	}

}
