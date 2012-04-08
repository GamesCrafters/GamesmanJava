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
import edu.berkeley.gamesman.parallel.game.connect4.C4Hasher;
import edu.berkeley.gamesman.parallel.game.connect4.C4State;
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

	@Override
	public GameValue getValue(NMMState state) {
		return state.getValue();
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

	
	//WE ARE HERE RIGHT NOWWWWWWWWWWWWWWWWWWWW (#$&%#$@(%*& #)$(%*) *#
	@Override
	protected Move[] getMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int suffixLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected boolean setNewRecordAndHasChildren(NMMState state, NMMRecord rec) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean combineValues(QuickLinkedList<NMMRecord> grList,
			NMMRecord gr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void previousPosition(NMMRecord gr, NMMRecord toFill) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Class<NMMRecord> getGameRecordClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GameRecord getRecord(NMMState position, NMMRecord fetchedRec) {
		// TODO Auto-generated method stub
		return null;
	}

}
