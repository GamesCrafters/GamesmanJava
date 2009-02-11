package edu.berkeley.gamesman.solver;

import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.WorkUnit;

/**
 * The top down solver is a simple solver that simply builds a
 * game tree in memory and solves it with a graph search.
 * @author Steven Schlansker
 */
public class TopDownSolver extends Solver {

	Configuration conf;
	Game<?> g;
	
	@Override
	public WorkUnit prepareSolve(Configuration config, Game<?> game) {
		conf = config;
		g = game;
		return new TopDownSolverWorkUnit();
	}
	
	class TopDownSolverWorkUnit implements WorkUnit {

		public void conquer() {
			// TODO Auto-generated method stub
			
		}

		public List<WorkUnit> divide(int num) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
