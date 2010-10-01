package edu.berkeley.gamesman.solver;

import java.util.Collection;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.LoopyMutaGame;
import edu.berkeley.gamesman.game.LoopyRecord;
import edu.berkeley.gamesman.game.TopDownMutaGame;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.Factory;

public class LoopySolver<S extends State> extends Solver {
	Pool<LoopyRecord> recordPool;

	public LoopySolver(Configuration conf) {
		super(conf);
	}

	@Override
	public WorkUnit prepareSolve(final Configuration conf) {
		final LoopyMutaGame<?> game = (LoopyMutaGame<?>) conf.getGame();
		recordPool = new Pool<LoopyRecord>(new Factory<LoopyRecord>() {

			public LoopyRecord newObject() {
				return game.getRecord();
			}

			public void reset(LoopyRecord t) {
				t.value = Value.UNDECIDED;
			}

		});
		long hashSpace = game.numHashes();
		Record defaultRecord = game.getRecord();
		defaultRecord.value = Value.IMPOSSIBLE;
		writeDb.fill(conf.getGame().recordToLong(null, defaultRecord), 0,
				hashSpace);

		return new WorkUnit() {

			public void conquer() {
				solve(conf);
			}

			public List<WorkUnit> divide(int num) {
				throw new UnsupportedOperationException();
			}

		};
	}

	public void solve(Configuration conf) {
		LoopyMutaGame<S> game = (LoopyMutaGame<S>) conf.<S> getCheckedGame();
		Collection<S> startingPositions = game.startingPositions();
		for (S state : startingPositions) {
			game.setToState(state);
			solve(game, state, game.getRecord(), 0, readDb.getHandle(),
					writeDb.getHandle());
		}
		/*
		 * Run through database:
		 * 	If (database value)<DRAW and remainingChildren>0:
		 * 		(database value)=DRAW
		 */
	}

	private void solve(TopDownMutaGame<S> game, S curState, Record value,
			int depth, DatabaseHandle readDh, DatabaseHandle writeDh) {
/*
 * value = {retrieve from database}
 *		case IMPOSSIBLE:
 *			Run primitiveValue
 *			if primitive:
 *				Store value in database
 *				Run through parents:
 *					fix(..., false)
 *			else:
 *				value.remainingChildren = len(children)
 *				value.value = DRAW
 *				Store value in database
 *				bestValue = -infinity
 *				Run through children:
 *					solve(...)
 *					if value==UNDECIDED:
 *						bestValue = {retrieve from database}
 *					else:
 *						if(value>bestValue)
 *							bestValue = value
 *							{store value in database}
 *						(database value).remainingChildren--
 *				value = bestValue.previousPosition()
 *				Run through parents:
 *					fix(..., false)
 *			value = UNDECIDED
 *		default:
 *			value = value.previousPosition()
 *
*/
	}
	
	private void fix(TopDownMutaGame<S> game, S curState, Record value, int depth, DatabaseHandle readDh, DatabaseHandle writeDh, boolean update){
/*
 * (database value) = {retrieve from database}
 * 	case IMPOSSIBLE:
 * 		Do nothing
 * 	default:
 *  If update:
 *  	value.children = (database value).remainingChildren
 *  else:
 *  	value.children = (database value).remainingChildren - 1
 *  if (database value) is DRAW or value>(database value)
 *  	(database value) = value
 *  	Run through parents:
 *  		fix(..., true)
 *  else
 *  	(database value).children = value.children
 */
	}
}
