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

	/**
	 * Finds number of possible parents for the given position.
	 * @param pos  Current position whose parents must be found.
	 * @param parents  Array container of positions that represent possible parents for this
	 * 		position. Starts off as empty, and possibleParents() builds and stores the
	 * 		possible parents into this object.
	 */
	public int possibleParents(AlignmentState pos, AlignmentState[] parents) {
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
		 * 	invariant: number of current pieces is the same	
		 */
		
		//compare how many pieces are on the board and compare
		//to the number of pieces on parent board
		//if numpieces owned by current player less or equal than parents.numpieces then its slide
		//if numpieces owned by current player > parents.numpieces then its place
		

		int numParents = 0;
		char lastTurn = pos.lastMove;
		
		//get the parents of a "set" move
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if (pos.numPieces > parents[numParents].numPieces){
					if (pos.get(row,col) == lastTurn){
						parents[numParents].set(pos);
						parents[numParents].put(row, col, ' ');
						numParents++;
					}
				}
			}
		}
		
		//get the parents of a "slide" move
		//if(pos.numPieces == parents[numParents].numPieces) {
			//for every piece on the board, the piece could have been moved there from a space
			//adjacent to it, which has now become an empty space. So, possible parents are
			//all boards where the current piece is empty, and one empty space next to it is this
			//piece
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if (pos.get(row,col) == lastTurn){
						
					//for every adjacent cell
					for (int i = -1; i <= 1; i++) {
						for (int j = -1; j <= 1; j++) {
								
							//if adjacent cell is in bounds, and it's empty, then build parent
							if( ((row + i) >= 0) && ((col + j) >= 0)
									&& (pos.get(row + i,col + j) == ' ')) {
									
								parents[numParents].set(pos);
								parents[numParents].put(row, col, ' ');
								parents[numParents].put(row + i, col + j, lastTurn);
								numParents++;
							}
						}
					}
				}
			}
		}
		
		//get parents that exist before "getting shot" which can only happen after a "slide" move 
		//if(pos.numPieces < parents[numParents].numPieces) {
			/*
			for every empty space, if a gun exists on the current board and it's pointing to
			this space, it is possible that a piece existed on this space and was shot right
			before this turn. There are 2 scenarios:
			 1. The gun was formed on the opponent's turn after a slide move, and shot the piece
			 2. The gun was already formed, and the opponent could have placed anywhere else, or
			 		slid anywhere else.
			
			 Build a possible parent from the two scenarios:
			 Scenario 1:
			  - the current empty space would have had the player's piece on the previous turn, so
			  		set the player's piece into the empty cell
			  - the opponent's gun must have been formed last turn after his slide move, so
					for each gun, one of the three pieces must have been slid in, and they could
					have existed in any of their empty spaces adjacent to the three pieces of the
					gun. So make possible parents for an opponent piece having existed in any of the
					empty cells around those pieces.
					
			  Scenario 2:
			   - find every possible parent for the current board configuration except that:
			    	 - insert the player's piece in this empty cell
			    	 - the opponent could not have set or slid into an empty cell that forms the gun
			    	 	so do all possible set and slide moves except the ones that form the gun
			 */
		for(int row = 0; row < gameHeight; row++) {
			for(int col = 0; col < gameWidth; col++) {
				if(pos.get(row,col) == ' ') {
					int[][] allGuns = new int[4][6];
					int[] gunN = pos.getGun(row, col, 'n', lastTurn);
					int[] gunS = pos.getGun(row, col, 's', lastTurn);
					int[] gunE = pos.getGun(row, col, 'e', lastTurn);
					int[] gunW = pos.getGun(row, col, 'w', lastTurn);
					int index = 0;
					if (gunN[0] != 0 && gunN[1] != 0) {
						allGuns[index] = gunN;
						index++;
					}
					if (gunS[0] != 0 && gunS[1] != 0){
						allGuns[index] = gunS;
						index++;
					}
					if (gunE[0] != 0 && gunE[1] != 0){
						allGuns[index] = gunE;
						index++;
					}
					if (gunW[0] != 0 && gunW[1] != 0){
						allGuns[index] = gunW;
						index++;
					}						
					boolean[] guns = pos.checkEnemyGun(row, col, lastTurn);
					//if the empty cell has at least one gun pointing to it, then consider it
					if(guns[0] || guns[1] || guns[2] || guns[3]) {
						//Scenario 1: guns were formed from slide move
						/* for ( EVERY_GUN ) {
						 * 		for( EVERY_PIECE_THAT_FORMS_THIS_GUN ) {
						 * 			for( EVERY_EMPTY_CELL_ADJACENT_TO_THIS_GUN) {
						 * 				parents[numParents].set(pos);
						 * 				parents[numParents].put(row, col, lastTurn);
						 * 				parents[numParents].movePiece(PIECE_X, PIECE_Y, EMPTY_ADJ_CELL_X, EMPTY_ADJ_CELL_Y, parents[numParents])
						 *				numParents++;
						 *			}
						 * 		}
						 * }
						 */	
						for(int i = 0; i < allGuns.length; i++) {
							for(int p = 0; p < 6; p = p + 2) {
								//for every empty cell adjacent
								for(int r = -1; r < 2; r++) {
									for(int c = -1; c < 2; c++) {
										if((r != 0) && (c != 0) && (pos.get(r,c) == ' ')) {
											parents[numParents].set(pos);
											parents[numParents].put(row, col, lastTurn);
											parents[numParents].movePiece(allGuns[i][p], allGuns[i][p+1], r, c, parents[numParents]);
											numParents++;
										}
									}
								}
							}
						}
						//Scenario 2: gun was already formed, then include all possible moves
						//except those that form the gun
						for(int g = 0; g < allGuns.length; g++){
							int x1 = allGuns[g][0]; //coords of base of current gun
							int y1 = allGuns[g][1];
							int x2 = allGuns[g][2];
							int y2 = allGuns[g][3];
							int x3 = allGuns[g][4];
							int y3 = allGuns[g][5];
							//reuse the set code, but never remove a piece in the gun
							for (int r = 0; r < gameHeight; r++) {
								for (int c = 0; c < gameWidth; c++) {
							//		if (pos.numPieces > parents[numParents].numPieces){
									if (pos.get(r,c) == lastTurn
											&&((x1 != r && y1 != c)
													|| (x2 != r && y2 != c)
													|| (x3 != r && y3 != c))) {
										parents[numParents].set(pos);
										parents[numParents].put(r, c, ' ');
										numParents++;
									}
								}
							}
							//slide, but don't slide a piece from the gun
							//if(pos.numPieces == parents[numParents].numPieces) {
							for (int r = 0; r < gameHeight; r++) {
								for (int c = 0; c < gameWidth; c++) {
									if (pos.get(r,c) == lastTurn
											&&((x1 != r && y1 != c)
													|| (x2 != r && y2 != c)
													|| (x3 != r && y3 != c))) {
										for (int i = -1; i <= 1; i++) {
											for (int j = -1; j <= 1; j++) {
												if( ((r + i) >= 0) && ((c + j) >= 0)
														&& (pos.get(r + i,c + j) == ' ')) {
													parents[numParents].set(pos);
													parents[numParents].put(r, c, ' ');
													parents[numParents].put(r + i, c + j, lastTurn);
													numParents++;
												}
											}
										}
									}
								}
							}
						}
					}
				}	
			}
		}
		return numParents;
	}

	@Override
	public int maxParents() {
		// TODO Auto-generated method stub
		return gameSize;
	}
}
