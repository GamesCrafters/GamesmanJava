package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.ProgressMeter;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Game;

public abstract class Solver {

	Database db;
	
	public void setDatabase(Database db){
		this.db = db;
	}
	
	public abstract void solve(Game<?, ?> game,ProgressMeter p);
	
}
