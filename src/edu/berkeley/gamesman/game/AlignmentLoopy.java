package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.AlignmentState;

/**
 * @author Kevin, Nancy
 * 
 */

public class AlignmentLoopy extends Alignment implements LoopyGame<AlignmentState> {

	
	public AlignmentLoopy(Configuration conf) {
		super(conf);
		
	}

	@Override
	public long recordStates() {
		return super.recordStates() + 2;
	}

	@Override
	public void longToRecord(AlignmentState recordState, long record, Record toStore) {
		if (record == super.recordStates()) {
			toStore.value = Value.IMPOSSIBLE;
		} else if (record == super.recordStates() + 1) {
			toStore.value = Value.DRAW;
		} else {
			super.longToRecord(recordState, record, toStore);
		}
	}

	@Override
	public long recordToLong(AlignmentState recordState, Record fromRecord) {
		if (fromRecord.value == Value.IMPOSSIBLE)
			return super.recordStates();
		else if (fromRecord.value == Value.DRAW)
			return super.recordStates() + 1;
		else
			return super.recordToLong(recordState, fromRecord);
	}

	public int possibleParents(AlignmentState pos, AlignmentState[] parents) {
		// TODO Auto-generated method stub
		//return 0;
		//compare how many pieces are on the board and compare
		//to the number of pieces on parent board
		//if numpieces owned by current player less or equal than parents.numpieces then its slide
		//if numpieces owned by current player > parents.numpieces then its place
		int numParents = 0;
		char lastTurn = pos.lastMove;
		
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if (pos.numPieces <= parents[numParents].numPieces){
					if (pos.get(row,col) == lastTurn){
						parents[numParents+1].set(pos);
						pos.set
					}
				}
			}
		}
		return numParents;
		
		/*
		 * check for placing pieces
		 * 	for each of the current player's pieces
		 * 		set next parent = current position 
		 * 		set current piece = ' '
		 * 
		 * check for sliding pieces
		 *  for each blank 
		 * 		-check to see if opponent piece could have been there
		 * 		if moving a piece from that parent would form a gun
		 * 		in the current position that would kill that opponent 
		 * 		piece
		 * 	invariant = number of current pieces is the same	
		 */
		
	}

	@Override
	public int maxParents() {
		// TODO Auto-generated method stub
		return gameSize;
	}
}
