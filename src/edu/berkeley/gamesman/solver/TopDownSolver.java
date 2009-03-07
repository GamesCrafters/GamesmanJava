package edu.berkeley.gamesman.solver;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
			//HashMap<T, BigInteger> cache = new HashMap<T, BigInteger>();
			HashSet<BigInteger> seen = new HashSet<BigInteger>();
			
			List<T> workList = new ArrayList<T>();
			ArrayList<Record> recs = new ArrayList<Record>();
			for (T s : game.startingPositions()) {
				workList.add(s);
				seen.add(game.stateToHash(s));
			}
			while(!workList.isEmpty()){
				recs.clear();
				T state = workList.remove(workList.size()-1);
				Collection<Pair<String,T>> children = game.validMoves(state);
				//TODO - shouldn't these lines use the displayState method in game? --Jeremy
				Util.debug(DebugFacility.Solver,"Looking at state "+game.stateToString(state));
				Util.debug(DebugFacility.Solver,"Worklist is now "+Util.mapStateToString(game,workList));
				
				long insertBefore = -1;
				
				for(Pair<String,T> child : children){
					BigInteger loc = game.stateToHash(child.cdr);
					Record r;
					if(!seen.contains(loc)){
						seen.add(loc);
						
						insertBefore = seen.size();
						Util.debug(DebugFacility.Solver,"Not seen child state "+game.stateToString(child.cdr)+" before, coming back later...");
						
						workList.add(child.cdr);
						continue;
					}
					try{ //TODO: make databases do something nicer than throw EOFExceptions
					r = database.getRecord(loc);
					if(r.get(RecordFields.Value) == PrimitiveValue.Undecided.value()){
						insertBefore = seen.size();
						workList.add(child.cdr);
						continue;
					}
					recs.add(r);
					}catch(Util.FatalError e){ // Hope this is an EOFException!
						continue;
					}
				}
				
				if(insertBefore != -1){
					Util.debug(DebugFacility.Solver,"One of the children hasn't been solved yet, revisiting "+game.stateToString(state)+" later");
					workList.add(0, state);
					continue;
				}
				
				BigInteger loc = game.stateToHash(state);
				Record next;
				PrimitiveValue prim = game.primitiveValue(state);
				if((!prim.equals(PrimitiveValue.Undecided)) || children.isEmpty()){
					Util.debug(DebugFacility.Solver,"Getting primitive value for state "+game.stateToString(state)+": "+prim);
					next = new Record(conf,prim);
				}else{
					next = Record.combine(conf, recs);
				}
				database.putRecord(loc, next);
				Util.debug(DebugFacility.Solver,"Solved state \n"+game.displayState(state)+" to "+next);
			}
		}

		public List<WorkUnit> divide(int num) {
			List<WorkUnit> a = new ArrayList<WorkUnit>();
			a.add(this);
			return a;
		}
		
	}

}
