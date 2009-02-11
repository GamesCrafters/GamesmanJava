package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * The top down solver is a simple solver that simply builds a
 * game tree in memory and solves it with a graph search.
 * @author Steven Schlansker
 */
public class TopDownSolver extends Solver {

	Configuration conf;
	Game<Object> g;
	
	@Override
	public WorkUnit prepareSolve(Configuration config, Game<?> game) {
		conf = config;
		g = Util.checkedCast(game);
		return new TopDownSolverWorkUnit();
	}
	
	class TopDownSolverWorkUnit implements WorkUnit {

		public void conquer() {
			HashMap<Object, BigInteger> seen = new HashMap<Object, BigInteger>();
			
			List<Object> workList = new ArrayList<Object>();
			ArrayList<Record> recs = new ArrayList<Record>();
			for (Object s : g.startingPositions()) workList.add(s);
			next: while(!workList.isEmpty()){
				recs.clear();
				Object state = workList.remove(workList.size()-1);
				Collection<Object> children = g.validMoves(state);
				for(Object child : children){
					BigInteger loc = seen.get(child);
					Record r;
					if(loc == null){
						loc = g.stateToHash(child);
						workList.add(state);
						workList.add(child);
						continue next;
					}else{
						r = db.getValue(loc);
						if(r.get(RecordFields.Value) == PrimitiveValue.Undecided.value()){
							workList.add(state);
							workList.add(child);
							continue next;
						}
					}
					if(r == null) r = db.getValue(loc);
					recs.add(r);
				}
				BigInteger loc = g.stateToHash(state);
				Record next;
				if(children.isEmpty())
					next = new Record(conf,g.primitiveValue(state));
				else
					next = Record.combine(conf, recs);
				db.setValue(loc, next);
				Util.debug(DebugFacility.Solver,"Solved state "+g.stateToString(state)+" to "+next);
			}
		}

		public List<WorkUnit> divide(int num) {
			List<WorkUnit> a = new ArrayList<WorkUnit>();
			a.add(this);
			return a;
		}
		
	}

}
