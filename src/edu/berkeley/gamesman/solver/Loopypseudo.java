package edu.berkeley.gamesman.solver;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.LoopyMutaGame;
import edu.berkeley.gamesman.game.LoopyRecord;

public class Loopypseudo {
/*
 * 	public void solve(Configuration conf) {
 *		LoopyMutaGame game = (LoopyMutaGame) conf.getGame();
 *		for (int startNum = 0; startNum < game.numStartingPositions(); startNum++) {
 *			game.setStartingPosition(startNum);
 *			solve(game, game.getRecord(), 0, readDb.getHandle(),
 *					writeDb.getHandle());
 *		}
 *		
 *		 // Run through database: //how to read database value?
 *		 //	If (database value)<DRAW and remainingChildren>0:
 *		 // 		(database value)=DRAW
 *		 
 *		
 *		for (long i = 0; i < readDb.numRecords(); i++) {
 *			//need to fill in 
 *			//if 
 *		}
 *
 *	}
 */

/*private void solve(LoopyMutaGame game, LoopyRecord value, int depth,
			DatabaseHandle readDh, DatabaseHandle writeDh) {
 *value = {retrieve from database}
 *		case IMPOSSIBLE:
 *			value.value = primitiveValue()
 *			if primitive:
 *				value.remoteness = value.remainingChildren = 0
 *				{Store value in database}
 *				value = value.previousPosition()
 *				Run through parents:
 *					fix(..., false)
  *			else:
 *				value.remainingChildren = len(children)
 *				value.value = DRAW
 *				{Store value in database}
 *				bestValue = -infinity
*			Run through children:
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
 *}
 *
 *private void fix(LoopyMutaGame game, LoopyRecord value, int depth,
 *			DatabaseHandle readDh, DatabaseHandle writeDh, boolean update) {
 *
 * (database value) = {retrieve from database}
 * Question: what's the difference between database value  and LoopyRecord value 
 *
 * 	case IMPOSSIBLE:
 * 		Do nothing
 * 	default:
 *  If update:
 *  	value.remainingChildren = (database value).remainingChildren
 *  else:
 *  	value.remainingChildren = (database value).remainingChildren - 1
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
	
	
	
	
	
	
}
