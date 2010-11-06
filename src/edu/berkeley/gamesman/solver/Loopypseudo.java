package edu.berkeley.gamesman.solver;

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
 *		for (long i = 0; i < readDb.numRecords(); i++) {
 *			//need to fill in 
 *			//if 
 *		}
 *
 *	}
 */

/*private void solve(LoopyMutaGame game, Record value, int depth,
			DatabaseHandle readDh, DatabaseHandle writeDh) {
 *		value.set({retrieve from database})
 *		case IMPOSSIBLE:
 *			value.value = primitiveValue()
 *			if primitive:
 *				value.remoteness = 0
 *				{Store value in database}
 *				value.previousPosition()
 *				Run through parents:
 *					fix(...)
  *			else:
  *				value.value = DRAW
 *				{Store value in database}
 *				bestValue = -infinity
 *				(pool)
*				Run through children:
 *					solve(...)
 *					if value.value == UNDECIDED:
 *						bestValue.set({retrieve current position from database (not child position)})
 *					else:
 *						if(value>bestValue)
 *							bestValue.set(value)
 *							{store bestValue in database}
 *				value.set(bestValue)
 *				value.previousPosition()
 *				Run through parents:
 *					fix(...)
 *			value.set(UNDECIDED)
 *			break;
 *		default:
 *			value.previousPosition()
 *			break;
 *}
 *
 *private void fix(LoopyMutaGame game, Record value,
 *			DatabaseHandle readDh, DatabaseHandle writeDh) {
 *
 * (database value) = {retrieve from database}
 * (pool)
 *
 * 	case IMPOSSIBLE:
 * 		Do nothing
 * 	default:
 * 		if (database value)<DRAW:
 * 			Error
 *  	else if value>(database value)
 *			{Store value in database}
 *		else if (database value)==DRAW:
 *			value.set(-infinity)
 *			Run through children:
 *				childValue = {Retrieve from database}
 *				if childValue == DRAW:
 *					break
 *				else if childValue>DRAW:
 *					Error
 *				else if childValue>value:
 *					value.set(childValue)
 *			else (if I don't break in the above loop):
 *				value = value.previousPosition()
 *				{Store value in database}
 *		if database was changed:
 *  		value = value.previousPosition()
 *  		Run through parents:
 *  			fix(...)
 */
	
	
	
	
	
	
}
