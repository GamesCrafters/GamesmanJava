package edu.berkeley.gamesman.parallel.game.ninemensmorris;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.Move;
//import edu.berkeley.gamesman.hasher.genhasher.Moves;
//import edu.berkeley.gamesman.parallel.ranges.MoveWritable;
//import edu.berkeley.gamesman.parallel.ranges.Range;
//import edu.berkeley.gamesman.parallel.ranges.RangeReducer;

import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.parallel.ranges.Suffix;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class NineMensMorris extends RangeTree<NMMState, NMMRecord> implements
		SolveReader<NMMState, NMMRecord> {

	private NMMHasher myHasher;
	private int levelsOfBoxes; // represents how many boxes are in the board. in
								// Typical NMM there are three concentric boxes
	private int elementsInABox; // each box has 8 elements in it.
	private int gameSize;
	private int pieces;
	private Configuration conf;
	private Move[] possibleMoves;

	@Override
	/*
	 * We assume that the String board is in the following format: The first 24
	 * characters in the string or either X or O, corresponding to pieces on the
	 * board the third to last character is an int that has the number of X to
	 * be placed the second to last character is an int that has the number of O
	 * to be placed the last character is whose turn it is.
	 */
	public void rangeTreeConfigure(Configuration conf) {
		possibleMoves = getMoves();
		levelsOfBoxes = conf.getInt("gamesman.game.levels", 3);
		elementsInABox = conf.getInt("gamesman.game.elements", 8);
		gameSize = levelsOfBoxes * elementsInABox;
		pieces = conf.getInt("gamesman.game.pieces", 9);
		myHasher = new NMMHasher(levelsOfBoxes, elementsInABox, 9);

	}

	public NMMState getPosition(String board) {

		assert board.length() == gameSize;
		int[] pos = new int[gameSize + 5];
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
		pos[gameSize + 4] = Integer.valueOf(
				board.substring(gameSize + 5, gameSize + 5)).intValue();
		pos[gameSize + 3] = xCount;
		pos[gameSize + 2] = oCount;
		pos[gameSize + 1] = Integer.valueOf(
				board.substring(gameSize + 1, gameSize + 1)).intValue();
		pos[gameSize] = Integer.valueOf(board.substring(gameSize, gameSize))
				.intValue();

		NMMState s = newState();
		getHasher().set(s, pos);
		return s;
	}

	private NMMState newState() {
		return myHasher.genHasherNewState();
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

	// WE ARE HERE ******************************

	@Override
	public Collection<Pair<String, NMMState>> getChildren(NMMState position) {
		ArrayList<Pair<String, NMMState>> children = new ArrayList<Pair<String, NMMState>>();
		for (Move m : possibleMoves) {
			NMMState s = newState();
			getHasher().set(s, position);
			if (playMove(s, m)) {
				// TODO: make sure that moves are indexed correctly
				children.add(new Pair<String, NMMState>(m.toString(), s));
			}
		}
		return children;
	}

	private boolean playMove(NMMState s, Move m) {
		// TODO Auto-generated method stub
		boolean made = false;
		if (m.matches(s) == -1)
			myHasher.makeMove(s, m);
		made = true;
		return made;
	}

	/*
	 * We assume that the String board is in the following format: The first 24
	 * characters in the string or either x or o, corresponding to pieces on the
	 * board the third to last character is an int that has the number of X
	 * pieces to be placed the second to last character is an int that has the
	 * number of O to be placed the last is a character specifying whose turn it
	 * is
	 */
	public String getString(NMMState position) {
		StringBuilder sb = new StringBuilder(gameSize + 3);
		for (int boxLevel = 0; boxLevel < levelsOfBoxes; boxLevel++) {
			for (int elementInBox = 0; elementInBox < elementsInABox; elementInBox++) {
				sb.append(charFor(position.get(elementInBox + boxLevel
						* elementsInABox)));
			}
		}
		sb.append(position.getXPiecesToBePlacedAsChar());
		sb.append(position.getOPiecesToBePlacedAsChar());
		sb.append(position.getTurnAsChar());
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

	@Override
	public GameValue getValue(NMMState state) {
		return state.getValue(state.get(gameSize + 4));
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

		// generate moves that put a new piece on the board
		for (int place = 0; place < gameSize; place++) {
			for (int toPlace = 0; toPlace < pieces; toPlace++) {
				for (int onBoard = 0; onBoard < pieces - toPlace; onBoard++) {
					allMoves.add(new Move(place, 0, 1, gameSize + 1, toPlace,
							toPlace - 1, gameSize + 3, onBoard, onBoard + 1,
							gameSize + 5, 1, 2));
					allMoves.add(new Move(place, 0, 2, gameSize, toPlace,
							toPlace - 1, gameSize + 2, onBoard, onBoard + 1,
							gameSize + 5, 2, 1));
				}
			}
		}

		/*
		 * ***************** <> <> <> <> <> <> <> <> <><>
		 * ********************************* Andrew, I corrected our moves such
		 * that we generate moves that remove the opponent's piece only if it
		 * forms a NEW 3-in-a-row.
		 * 
		 * also i corrected it such that a player can only make a move if she
		 * has NO more pieces to place on the board
		 */
		// generate moves that move one piece to another
		int moveTo;
		for (int boxLevelofMoveTo = 0; boxLevelofMoveTo < levelsOfBoxes; boxLevelofMoveTo++) {
			for (int elementInBoxofMoveTo = 0; elementInBoxofMoveTo < elementsInABox; elementInBoxofMoveTo++) {
				moveTo = boxLevelofMoveTo * elementsInABox
						+ elementInBoxofMoveTo;
				for (int numPieces = 3; numPieces < pieces; numPieces++) {
					Integer[] moveFromArray = generateMoveFroms(
							boxLevelofMoveTo, elementInBoxofMoveTo, numPieces);
					for (int moveFrom : moveFromArray) {
						if (moveFrom != moveTo) {
							// make the move without removing a piece from board
							allMoves.add(new Move(moveFrom, 1, 0, moveTo, 0, 1,
									gameSize + 1, 0, 0, gameSize + 5, 1, 2));
							allMoves.add(new Move(moveFrom, 2, 0, moveTo, 0, 2,
									gameSize, 0, 0, gameSize + 5, 2, 1));

							// go through all possible pieces to get rid of and
							// pieces on board and make the move
							/*
							 * actually this is incorrect, we need to make sure
							 * that a three in a row exists (where moved) there
							 * are only horizontal or vertical possible 3-in a
							 * rows if the element is a corner of box, check
							 * those ones if it's in the middle of a box, check
							 * those ones check it with the same element in the
							 * other boxes (for all boxes != its box) add the
							 * existance of those same-colored pieces to the
							 * move
							 */
							for (int removeFrom = 0; removeFrom < gameSize; removeFrom++) {
								for (int onBoard = 0; onBoard < 9; onBoard++) {
									int mid1, corner1, mid2, corner2; // midX
																		// and
																		// cornerX
																		// form
																		// a
																		// possible
																		// three-in-a-row
																		// with
																		// the
																		// position
																		// of
																		// moveTo
									// if moveTo is a middle piece, then
									// corner1-corner2 from a 3inarow, and
									// corner3 and corner 4 do the same
									int diag1, diag2; // this is used to detect
														// diagonal three in a
														// rows
									if (elementInBoxofMoveTo % 2 == 0) {// if it
																		// is on
																		// the
																		// corner.

										mid1 = ((elementInBoxofMoveTo + 1) % elementsInABox)
												+ boxLevelofMoveTo
												* elementsInABox;
										corner1 = ((elementInBoxofMoveTo + 2) % elementsInABox)
												+ boxLevelofMoveTo
												* elementsInABox;
										mid2 = ((elementInBoxofMoveTo - 1) % elementsInABox)
												+ boxLevelofMoveTo
												* elementsInABox;
										corner2 = ((elementInBoxofMoveTo - 2) % elementsInABox)
												+ boxLevelofMoveTo
												* elementsInABox;

										allMoves.add(new Move(moveFrom, 1, 0,
												moveTo, 0, 1, mid1, 1, 1,
												corner1, 1, 1, removeFrom, 2,
												0, gameSize + 2, onBoard,
												onBoard - 1, gameSize + 1, 0,
												0, gameSize + 5, 1, 2));
										allMoves.add(new Move(moveFrom, 1, 0,
												moveTo, 0, 1, mid2, 1, 1,
												corner2, 1, 1, removeFrom, 2,
												0, gameSize + 2, onBoard,
												onBoard - 1, gameSize + 1, 0,
												0, gameSize + 5, 1, 2));

										allMoves.add(new Move(moveFrom, 2, 0,
												moveTo, 0, 2, mid1, 2, 2,
												corner1, 2, 2, removeFrom, 1,
												0, gameSize + 3, onBoard,
												onBoard - 1, gameSize, 0, 0,
												gameSize + 5, 2, 1));
										allMoves.add(new Move(moveFrom, 2, 0,
												moveTo, 0, 2, mid2, 2, 2,
												corner2, 2, 2, removeFrom, 1,
												0, gameSize + 3, onBoard,
												onBoard - 1, gameSize, 0, 0,
												gameSize + 5, 2, 1));

									}

									else { // we are looking at a piece in the
											// middle of a box so add the
											// 3-in-a-row
										corner1 = ((elementInBoxofMoveTo + 1) % elementsInABox)
												+ boxLevelofMoveTo
												* elementsInABox;
										corner2 = ((elementInBoxofMoveTo - 1) % elementsInABox)
												+ boxLevelofMoveTo
												* elementsInABox;

										allMoves.add(new Move(moveFrom, 1, 0,
												moveTo, 0, 1, corner1, 1, 1,
												corner2, 1, 1, removeFrom, 2,
												0, gameSize + 2, onBoard,
												onBoard - 1, gameSize + 1, 0,
												0, gameSize + 5, 1, 2));

										allMoves.add(new Move(moveFrom, 2, 0,
												moveTo, 0, 2, corner1, 2, 2,
												corner2, 2, 2, removeFrom, 1,
												0, gameSize + 3, onBoard,
												onBoard - 1, gameSize, 0, 0,
												gameSize + 5, 2, 1));

									}
									// here we look at the "diagonals" the three
									// in a rows that occur across different box
									// levels
									diag1 = ((boxLevelofMoveTo + 1) % levelsOfBoxes)
											* elementsInABox
											+ elementInBoxofMoveTo;
									diag2 = ((boxLevelofMoveTo + 2) % levelsOfBoxes)
											* elementsInABox
											+ elementInBoxofMoveTo;

									allMoves.add(new Move(moveFrom, 1, 0,
											moveTo, 0, 1, diag1, 1, 1, diag2,
											1, 1, removeFrom, 2, 0,
											gameSize + 2, onBoard, onBoard - 1,
											gameSize + 1, 0, 0, gameSize + 5,
											1, 2));
									allMoves.add(new Move(moveFrom, 2, 0,
											moveTo, 0, 2, diag1, 2, 2, diag2,
											2, 2, removeFrom, 1, 0,
											gameSize + 3, onBoard, onBoard - 1,
											gameSize, 0, 0, gameSize + 5, 2, 1));

								}
							}
						}
					}
				}
			}
		}
		return allMoves.toArray(new Move[allMoves.size()]);
	}

	private Integer[] generateMoveFroms(int boxLevelofMoveTo,
			int elementInBoxofMoveTo, int numPieces) {
		int adj1;
		int adj2;
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		if (numPieces == 3) {
			for (int i = 0; i < gameSize; i++) {
				toReturn.add(i);
			}
		} else {
			adj1 = ((elementInBoxofMoveTo + 1) % elementsInABox)
					+ boxLevelofMoveTo * levelsOfBoxes;
			adj2 = ((elementInBoxofMoveTo - 1) % elementsInABox)
					+ boxLevelofMoveTo * levelsOfBoxes;
			toReturn.add(adj1);
			toReturn.add(adj2);

			if (boxLevelofMoveTo == 0) {
				toReturn.add(elementInBoxofMoveTo + elementsInABox);
			} else if (boxLevelofMoveTo == levelsOfBoxes - 1) {
				toReturn.add(elementInBoxofMoveTo - elementsInABox);
			} else {
				toReturn.add(elementInBoxofMoveTo + elementsInABox);
				toReturn.add(elementInBoxofMoveTo - elementsInABox);
			}

		}

		return toReturn.toArray(new Integer[toReturn.size()]);
	}

	@Override
	public int getDivision(Suffix<NMMState> suff) {
		int len = suff.length();
		int invariant = suff.get(len - 1);
		for (int i = len - 2; i >= len - 5; i--) {
			invariant <<= 4;
			invariant |= suff.get(i);
		}
		return invariant;
	}

	// TODO do this
	/*
	 * public boolean validMove(NMMState state, Move move) { return false; }
	 */

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
		return state.get(gameSize + 2) + state.get(gameSize + 3);
	}

	@Override
	public GameRecord getRecord(NMMState position, NMMRecord fetchedRec) {
		return NMMRecord.getRecord(fetchedRec, gameSize - numPieces(position));
	}

	protected int maxVarianceLength() {
		return gameSize;
	}
}
