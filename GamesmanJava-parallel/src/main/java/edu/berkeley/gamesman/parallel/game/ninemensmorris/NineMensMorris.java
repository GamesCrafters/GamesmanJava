package edu.berkeley.gamesman.parallel.game.ninemensmorris;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Reducer;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.Move;
//import edu.berkeley.gamesman.hasher.genhasher.Moves;
//import edu.berkeley.gamesman.parallel.ranges.MoveWritable;
//import edu.berkeley.gamesman.parallel.ranges.Range;
//import edu.berkeley.gamesman.parallel.ranges.RangeReducer;
import edu.berkeley.gamesman.parallel.FlipRecord;
import edu.berkeley.gamesman.parallel.game.connect4.C4Hasher;
import edu.berkeley.gamesman.parallel.game.connect4.C4State;
import edu.berkeley.gamesman.parallel.game.connect4.Connect4;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class NineMensMorris extends RangeTree<NMMState, NMMRecord> implements 
SolveReader<NMMState, NMMRecord>	{

	private NMMHasher myHasher;
	private final int levelsOfBoxes = 3; //represents how many boxes are in the board. in Typical NMM there are three concentric boxes
	private final int elementsInABox = 8; // each box has 8 elements in it.
	private int gameSize = levelsOfBoxes*elementsInABox;
	private Configuration conf;

	@Override
	/* We assume that the String board is in the following format:
	 * The first 24 characters in the string or either X or O, corresponding to pieces on the board
	 * the second to last character is an int that has the number of X to be placed
	 * the last character is an int that has the number of O to be placed
	 */
	public NMMState getPosition(String board) {

		assert board.length() == gameSize;
		int[] pos = new int[gameSize + 4];
		int xCount = 0;
		int oCount = 0;
		int i = 0;
		for (int boxLevel = 0; boxLevel < levelsOfBoxes; boxLevel++) {
			for (int elementInBox = 0; elementInBox < elementsInABox; elementInBox++) {
				char c = board.charAt(i++);
				pos[boxLevel * elementsInABox + elementInBox] = pieceFor(c);
				if (c == 'X')
					xCount++;
				if (c == 'O')
					oCount++;
			}
		}
		pos[gameSize+3] = xCount;
		pos[gameSize+2] = oCount;
		pos[gameSize+1] = Integer.valueOf(board.substring(gameSize+1, gameSize+1)).intValue();
		pos[gameSize] = Integer.valueOf(board.substring(gameSize, gameSize)).intValue();


		NMMState s = newState();
		getHasher().set(s, pos);
		return s;
	}

	// WRITE THIS METHOD ALSO
	private NMMState newState() {
		// TODO Auto-generated method stub
		return null;
	}

	private static int pieceFor(char c) {
		switch (c) {
		case ' ':
			return 0;
		case 'X':
			return 1;
		case 'O':
			return 2;
		default:
			throw new IllegalArgumentException();
		}
	}


	@Override
	public Collection<Pair<String, NMMState>> getChildren(NMMState position) {
		// TODO Auto-generated method stub
		return null;
	}


	/* We assume that the String board is in the following format:
	 * The first 24 characters in the string or either x or o, corresponding to pieces on the board
	 * the second to last character is an int that has the number of X pieces to be placed
	 * the last character is an int that has the number of O to be placed
	 */
	public String getString(NMMState position) {
		StringBuilder sb = new StringBuilder(gameSize+3);
		for (int boxLevel = 0; boxLevel < levelsOfBoxes; boxLevel++) {
			for (int elementInBox = 0; elementInBox < elementsInABox; elementInBox++) {
				sb.append(charFor(position.get(elementInBox + boxLevel * elementsInABox)));
			}
		}
		sb.append(position.getXPiecesToBePlacedAsChar());
		sb.append(position.getOPiecesToBePlacedAsChar());
		return sb.toString();

	}

	static char charFor(int piece) {
		switch (piece) {
		case 0:
			return ' ';
		case 1:
			return 'X';
		case 2:
			return 'O';
		default:
			return '?';
		}
	}


	// ISSUE WE NEED TO KNOW WHO'S TURN IT IS
	// WE HAVE TO HANDLE THIS -1 IS NOT CORRECT
	@Override
	public GameValue getValue(NMMState state) {
		return state.getValue(-1);
	}

	@Override 
	public Collection<NMMState> getStartingPositions() {
		NMMState result = myHasher.newState();
		return Collections.singleton(result);
	}

	@Override
	public NMMHasher getHasher() {
		return myHasher;
	}


	@Override
	protected Move[] getMoves() {

		ArrayList<Move> allMoves = new ArrayList<Move>();

		//generate moves that put a new piece on the board
		for (int place=0; place < gameSize; place++) {
			for (int toPlace = 0; toPlace < 9; toPlace++ ) {
				for (int onBoard = 0; onBoard < 9 - toPlace; onBoard++) {
					allMoves.add(new Move(place,0,1, gameSize+1,toPlace,toPlace-1, gameSize+3,onBoard,onBoard+1 ));
					allMoves.add(new Move(place,0,2, gameSize,toPlace,toPlace-1, gameSize+2,onBoard,onBoard+1 ));
				}
			}
		}

		/*
		 * ***************** <> <> <> <> <> <> <> <> <><> *********************************
		 * Andrew, I corrected our moves such that we generate moves that remove the opponent's piece
		 * only if it forms a NEW 3-in-a-row.
		 * 
		 * also i corrected it such that a player can only make a move if she has NO more pieces to place on the board
		 */
		//generate moves that move one piece to another
		int moveTo;
		for (int boxLevelofMoveTo = 0; boxLevelofMoveTo < levelsOfBoxes; boxLevelofMoveTo++) {
			for (int elementInBoxofMoveTo = 0; elementInBoxofMoveTo < elementsInABox; elementInBoxofMoveTo++) {
				moveTo = boxLevelofMoveTo * elementsInABox + elementInBoxofMoveTo;
				for (int moveFrom = 0; moveFrom<gameSize; moveFrom++) {
					if(moveFrom != moveTo ) {
						//make the move without removing a piece from board
						allMoves.add(new Move(moveFrom,1,0, moveTo,0,1, gameSize+1,0,0));
						allMoves.add(new Move(moveFrom,2,0, moveTo,0,2, gameSize,0,0));

						//go through all possible pieces to get rid of and pieces on board and make the move
						/* actually this is incorrect, we need to make sure that a three in a row exists (where moved)
						 * there are only horizontal or vertical possible 3-in a rows
						 * if the element is a corner of box, check those ones
						 * if it's in the middle of a box, check those ones
						 * check it with the same element in the other boxes (for all boxes != its box)
						 * add the existance of those same-colored pieces to the move
						 */
						for (int removeFrom=0; removeFrom<gameSize; removeFrom++) {
							for (int onBoard = 0; onBoard < 9; onBoard++) {	
								int mid1,corner1,mid2,corner2; //midX and cornerX form a possible three-in-a-row with the position of moveTo
								//if moveTo is a middle piece, then corner1-corner2 from a 3inarow, and corner3 and corner 4 do the same
								int diag1, diag2; //this is used to detect diagnol three in a rows
								if (elementInBoxofMoveTo % 2 == 0)  {//if it is on the corner.

									mid1 = ((elementInBoxofMoveTo + 1) % elementsInABox) + boxLevelofMoveTo*elementsInABox;
									corner1 = ((elementInBoxofMoveTo + 2) % elementsInABox) + boxLevelofMoveTo*elementsInABox;
									mid2 = ((elementInBoxofMoveTo - 1) % elementsInABox) + boxLevelofMoveTo*elementsInABox;
									corner2 = ((elementInBoxofMoveTo - 2) % elementsInABox) + boxLevelofMoveTo*elementsInABox;

									allMoves.add(new Move(moveFrom,1,0, moveTo,0,1, mid1,1,1, corner1,1,1,
											removeFrom,2,0, gameSize+2,onBoard,onBoard-1, gameSize+1,0,0));
									allMoves.add(new Move(moveFrom,1,0, moveTo,0,1, mid2,1,1, corner2,1,1,
											removeFrom,2,0, gameSize+2,onBoard,onBoard-1, gameSize+1,0,0));

									allMoves.add(new Move(moveFrom,2,0, moveTo,0,2, mid1,2,2, corner1,2,2,
											removeFrom,1,0, gameSize+3,onBoard,onBoard-1, gameSize,0,0));
									allMoves.add(new Move(moveFrom,2,0, moveTo,0,2, mid2,2,2, corner2,2,2,
											removeFrom,1,0, gameSize+3,onBoard,onBoard-1, gameSize,0,0));

								}

								else { //we are looking at a piece in the middle of a box so add the 3-in-a-row
									corner1 = ((elementInBoxofMoveTo + 1) % elementsInABox) + boxLevelofMoveTo*elementsInABox;
									corner2 = ((elementInBoxofMoveTo - 1) % elementsInABox) + boxLevelofMoveTo*elementsInABox;

									allMoves.add(new Move(moveFrom,1,0, moveTo,0,1, corner1,1,1, corner2,1,1,
											removeFrom,2,0, gameSize+2,onBoard,onBoard-1, gameSize+1,0,0));

									allMoves.add(new Move(moveFrom,2,0, moveTo,0,2, corner1,2,2, corner2,2,2,
											removeFrom,1,0, gameSize+3,onBoard,onBoard-1, gameSize,0,0));

								}
								// here we look at the "diagnols" the three in a rows that occur across different box levels
								diag1 = ((boxLevelofMoveTo + 1) % levelsOfBoxes)*elementsInABox + elementInBoxofMoveTo;
								diag2 = ((boxLevelofMoveTo + 2) % levelsOfBoxes)*elementsInABox + elementInBoxofMoveTo;

								allMoves.add(new Move(moveFrom,1,0, moveTo,0,1, diag1,1,1, diag2,1,1,
										removeFrom,2,0, gameSize+2,onBoard,onBoard-1, gameSize+1,0,0));
								allMoves.add(new Move(moveFrom,2,0, moveTo,0,2, diag1,2,2, diag2,2,2,
										removeFrom,1,0, gameSize+3,onBoard,onBoard-1, gameSize,0,0));		

							}
						}
					}
				}
			}
		}
		return allMoves.toArray(new Move[allMoves.size()]);
	}

	@Override
	protected boolean setNewRecordAndHasChildren(NMMState state, NMMRecord rec) {
		GameValue val = getValue(state);
		if (val == null) {
			rec.set(GameValue.DRAW);
			return true;
		} else {
			if (val == GameValue.TIE)
				rec.set(GameValue.TIE, 0);
			else if (val == GameValue.LOSE)
				rec.set(GameValue.LOSE, 0);
			else
				throw new RuntimeException("No other primitives");
			return false;
		}

	}

	@Override
	protected boolean combineValues(QuickLinkedList<NMMRecord> grList,
			NMMRecord gr) {
		return NMMRecord.combineValues(grList, gr);
	}

	@Override
	protected void previousPosition(NMMRecord gr, NMMRecord toFill) {
		toFill.previousPosition(gr);

	}

	@Override
	protected Class<NMMRecord> getGameRecordClass() {
		return NMMRecord.class;

	}
	
	int numPieces(NMMState state) {
		return state.get(gameSize+2) + state.get(gameSize+3);
	}

	@Override
	public GameRecord getRecord(NMMState position, NMMRecord fetchedRec) {
		return NMMRecord.getRecord(fetchedRec, gameSize - numPieces(position));
	}
	
	protected int maxVarianceLength() {
		return gameSize;
	}
}
