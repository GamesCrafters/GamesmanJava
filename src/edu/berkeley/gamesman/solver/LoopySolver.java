package                                                                                                                                                                                                                                                                                      edu.berkeley.gamesman.solver;

import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.LoopyMutaGame;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.RecycleLinkedList;

/**
 * @author David, Brent, Nancy, Kevin, Peter, Sharmishtha, Raji
 *
 */
public class LoopySolver extends Solver {
	Pool<Record> recordPool;
	
	protected RecycleLinkedList<Record[]> recordList; //Nancy: added this for else clause in solve loopy game function

	public LoopySolver(Configuration conf) {
		super(conf);
	}

	@Override
	public WorkUnit prepareSolve(final Configuration conf) {
		final LoopyMutaGame game = (LoopyMutaGame) conf.getGame();
		recordPool = new Pool<Record>(new Factory<Record>() {

			public Record newObject() {
				return game.getRecord();
			}

			public void reset(Record t) {
				t.value = Value.UNDECIDED;
			}

		});
		long hashSpace = game.numHashes();
		Record defaultRecord = game.getRecord();
		defaultRecord.value = Value.IMPOSSIBLE; //why do we set it to impossible?
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
		LoopyMutaGame game = (LoopyMutaGame) conf.getGame();
		for (int startNum = 0; startNum < game.numStartingPositions(); startNum++) {
			game.setStartingPosition(startNum); 
			solve(game, game.getRecord(), 0, readDb.getHandle(),
					writeDb.getHandle());
		}
	}

	private void solve(LoopyMutaGame game, Record value, int depth,
			DatabaseHandle readDh, DatabaseHandle writeDh) {
	long hash = game.getHash();
	game.longToRecord(readDb.getRecord(readDh, hash), value);
	Record best; //Added for best value, not sure
	
/*
 *		case IMPOSSIBLE:
 *			value.value = primitiveValue()
 *			if primitive:
 *				value.remoteness = value.remainingChildren = 0
 *				{Store value in database}
 *				value = value.previousPosition()
 *				Run through parents:
 *					fix(..., false)
 */
	switch(value.value){
		case IMPOSSIBLE:
			value.value = game.primitiveValue();
			if (value.value != Value.UNDECIDED){
				value.remoteness = 0;
				writeDb.putRecord(writeDh, hash, game.recordToLong(value));
				value.previousPosition();
				boolean done = game.unmakeMove()>0;
				while(!done){
					fix(game,value,readDh,writeDh,false);
					done = game.changeUnmakeMove();
				}
				game.remakeMove();
			}
/*	
 *			else:
 *				value.remainingChildren = len(children)
 *				value.value = DRAW
 *				{Store value in database}
 *				bestValue = -infinity
 *
 */
			else{
				value.value = Value.DRAW;
				writeDb.putRecord(writeDh, hash, game.recordToLong(value));
				//best.value = Value.				
				
				
			}
	}
			//HOW to run through children? (ie makeMove/moveLists)
/*			Run through children:
 *					solve(...)
 *					if value.value == UNDECIDED:
 *						bestValue = {retrieve from database}
 *					else:
 *						if(value.remainingChildren==0):
 *							value.remainingChildren = (database value).remainingChildren - 1
 *						else
 *							value.remainingChildren = (database value).remainingChildren
 *						if(value>bestValue)
 *							bestValue = value
 *							{store value in database}
 *						else
 *							{store value.remainingChildren in database}
 *				value = bestValue
 *				Run through parents:
 *					fix(..., false)
 *			value = UNDECIDED
 *		default:
 *			value = value.previousPosition()
 *
*/
			Record[] recs = recordList.addFirst();
			boolean made = game.makeMove()>0;
			int i = 0;
			while (made) {
				solve(game, value, depth + 1, readDh, writeDh);
				recs[i].set(value);
				recs[i].previousPosition();
				++i;
				made = game.changeMove();
			}
			if (value.value == Value.UNDECIDED){
				best = value;
			}
			else {
//				if(value.remainingChildren == 0){
					//keep track of remaining children
					//loop through positions
					//see if draws, if none draws, remaining children 0
					//else children is greater
					
					//don't do this --value.remainingChildren = (database value).remainingChildren	
				}
//				else{
					//value.remainingChildren = (database value).remainingChildren
//				}
	//			if (value.value > best.value){
//					best.value = value.value;
					writeDb.putRecord(writeDh, hash, game.recordToLong(value));
//				}
	}	
	private void fix(LoopyMutaGame game, Record value,
			DatabaseHandle readDh, DatabaseHandle writeDh, boolean update) {
/*
 * (database value) = {retrieve from database}
 * Question: what's the difference between database value  and Record value 
 */
 //		Record value = new Record(conf);
//		game.longToRecord(readDb.getRecord(readDb.getHandle() game.getHash(), value);
		
		//how do we define database value?
/* 	case IMPOSSIBLE:
 * 		Do nothing
 * 	default:
 *  If update:
 *  	value.remainingChildren = (database value).remainingChildren
 *  else:
 *  	value.remainingChildren = (database value).remainingChildren - 1
 */
		switch (value.value){
			case IMPOSSIBLE:
				return;
			default:
			if (update){
			//	value.remainingChildren = 
					
			}
			else{
				// value.remainingChildren = 
			}
		}
		
		
/*  
 *  if (database value).value is DRAW or value>(database value)
 *  	{Store value in database}
 *  	value = value.previousPosition()
 *  	Run through parents:
 *  		fix(..., not (database value changed from <=DRAW to >DRAW or 
 *  				(database value<DRAW and database value.remainingChildren changed from 1 to 0)))
 *  	value = value.nextPosition()
 *  else
 *  	{Store value.remainingChildren in database}
 */
		if (true){
			value.previousPosition();
			//while(value.remainingChildren >= 0) {
				//fix(game, value, depth, readDh, writeDh, not());
			//}
			value.nextPosition();
		}
		else{
			//writeDb.putRecord(writeDh, hash, value.remainingChildren);
		}
	}
}