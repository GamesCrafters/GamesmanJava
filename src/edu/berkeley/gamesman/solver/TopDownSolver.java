package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * The top down solver is a simple solver that simply builds a
 * game tree in memory and solves it with a graph search.
 * @author Steven Schlansker
 */
public class TopDownSolver extends Solver {

	Configuration conf;
	//Game<Object> g;
	
	@Override
	public WorkUnit prepareSolve(Configuration config, Game<Object> game) {
		conf = config;
		TopDownSolverWorkUnit<Object> wu = new TopDownSolverWorkUnit<Object>(game,db);
		return wu;
	}
	
	class TopDownSolverWorkUnit<T> implements WorkUnit {
		
		final private Game<T> game;
		final private Database database;

		public TopDownSolverWorkUnit(Game<T> g, Database db) {
			game = g;
			this.database = db;
		}

		public void conquer() {
			HashMap<T, BigInteger> cache = new HashMap<T, BigInteger>();
			
			List<T> workList = new ArrayList<T>();
			ArrayList<Record> recs = new ArrayList<Record>();
			for (T s : game.startingPositions()) workList.add(s);
			next: while(!workList.isEmpty()){
				recs.clear();
				T state = workList.remove(workList.size()-1);
				Collection<Pair<String,T>> children = game.validMoves(state);
				Util.debug(DebugFacility.Solver,"Looking at state "+state);
				Util.debug(DebugFacility.Solver,"Worklist is now "+workList);
				for(Pair<String,T> child : children){
					BigInteger loc = cache.get(child.cdr);
					Record r;
					if(loc == null){
						loc = game.stateToHash(child.cdr);
						cache.put(child.cdr,loc);
						workList.add(state);
						workList.add(child.cdr);
						continue next;
					}
					r = database.getRecord(loc);
					if(r.get(RecordFields.Value) == PrimitiveValue.Undecided.value()){
						workList.add(state);
						workList.add(child.cdr);
						continue next;
					}
					recs.add(r);
				}
				BigInteger loc = game.stateToHash(state);
				Record next;
				if(children.isEmpty()){
					PrimitiveValue prim = game.primitiveValue(state);
					Util.debug(DebugFacility.Solver,"Getting primitive value for state "+state+": "+prim);
					next = new Record(conf,prim);
				}else{
					next = Record.combine(conf, recs);
				}
				database.putRecord(loc, next);
				Util.debug(DebugFacility.Solver,"Solved state "+game.displayState(state)+" to "+next);
			}
		}

		public List<WorkUnit> divide(int num) {
			List<WorkUnit> a = new ArrayList<WorkUnit>();
			a.add(this);
			return a;
		}
		
	}

}
