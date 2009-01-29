package edu.berkeley.gamesman.master;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.hasher.Hasher;
import edu.berkeley.gamesman.solver.Solver;

public interface Master {

	public void initialize(Class<? extends Game<?, ?>> game, Class<? extends Solver> solver, Class<? extends Hasher> hasher, Class<? extends Database> database);
	
	public void run();
	
}
