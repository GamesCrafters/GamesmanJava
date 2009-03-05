package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.game.util.Rearranger;
import edu.berkeley.gamesman.util.Pair;

public class TicTacToe extends TieredIterGame {
	int piecesToWin;
	final int[][] position;
	int numPieces;
	Rearranger piece_empty;
	Rearranger o_x;

	public TicTacToe(Configuration conf) {
		super(conf);
		piecesToWin = Integer.parseInt(conf
				.getProperty("tictactoe.pieces", "3"));
		position = new int[gameHeight][gameWidth];
	}

	@Override
	public TicTacToe clone() {
		TicTacToe other = new TicTacToe(conf);
		other.setState(getState());
		return other;
	}

	private void setOXs() {
		String sxo = piece_empty.toString();
		sxo.replace('O', 'T');
		sxo.replace('X', ' ');
		try {
			o_x = new Rearranger(sxo, numPieces / 2, (numPieces + 1) / 2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String displayState() {
		String s = stateToString();
		StringBuffer str = new StringBuffer((gameWidth+3)*gameHeight);
		for(int row=gameHeight-1;row>=0;row--){
			str.append("|"+s.substring(row*gameWidth,row*(gameWidth+1)-1)+"|\n");
		}
		return str.toString();
	}

	@Override
	public ItergameState getState() {
		return new ItergameState(getTier(), piece_empty.getHash().multiply(
				o_x.arrangements).add(o_x.getHash()));
	}

	@Override
	public int getTier() {
		return numPieces;
	}

	@Override
	public boolean hasNextHashInTier() {
		return (o_x.hasNext() || piece_empty.hasNext());
	}

	@Override
	public void nextHashInTier() {
		if (o_x.hasNext())
			o_x.next();
		else {
			piece_empty.next();
			setOXs();
		}
	}

	@Override
	public BigInteger numHashesForTier() {
		return piece_empty.arrangements.multiply(o_x.arrangements);
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public PrimitiveValue primitiveValue() {
		char lastTurn = (numPieces % 2 == 1) ? 'X' : 'O';
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if(checkWinOnPiece(row,col,lastTurn))
					return PrimitiveValue.Lose;
			}
		}
		if(numPieces == gameWidth*gameHeight)
			return PrimitiveValue.Tie;
		else
			return PrimitiveValue.Undecided;
	}

	private boolean checkWinOnPiece(int row, int col, char lastTurn) {
		if(get(row,col)!=lastTurn)
			return false;
		int i;
		for(i=1;i<piecesToWin;i++){
			if(!(exists(row+i,col)&&get(row+i,col)==lastTurn))
				break;
		}
		if(i==piecesToWin)
			return true;
		for(i=1;i<piecesToWin;i++){
			if(!(exists(row,col+i)&&get(row,col+i)==lastTurn))
				break;
		}
		if(i==piecesToWin)
			return true;
		for(i=1;i<piecesToWin;i++){
			if(!(exists(row+i,col+i)&&get(row+i,col+i)==lastTurn))
				break;
		}
		if(i==piecesToWin)
			return true;
		for(i=1;i<piecesToWin;i++){
			if(!(exists(row+i,col-i)&&get(row+i,col-i)==lastTurn))
				break;
		}
		if(i==piecesToWin)
			return true;
		return false;
	}

	private boolean exists(int row, int col) {
		return row >= 0 && row < gameHeight && col >= 0 && col < gameWidth;
	}

	private char get(int row, int col) {
		return o_x.get(position[row][col]);
	}

	@Override
	public void setStartingPosition(int n) {
		setTier(0);
	}

	@Override
	public void setState(ItergameState pos) {
		setTier(pos.tier());
		BigInteger[] hashes = pos.hash().divideAndRemainder(o_x.arrangements);
		piece_empty.unHash(hashes[0]);
		setOXs();
		o_x.unHash(hashes[1]);
	}

	@Override
	public void setTier(int tier) {
		numPieces = tier;
		char[] pieces = new char[gameHeight * gameWidth];
		Arrays.fill(pieces, 'T');
		try {
			piece_empty = new Rearranger(pieces, numPieces, gameHeight
					* gameWidth - numPieces);
			setOXs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setToString(String pos) {
		String pieces = pos.replace('X', 'O').replace(' ', 'X');
		try {
			piece_empty = new Rearranger(pieces);
			o_x = new Rearranger(pos);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String stateToString() {
		return o_x.toString();
	}
	
	private Pair<Integer, Integer> rowCol(int piece){
		return new Pair<Integer, Integer>(piece/gameWidth,piece%gameWidth);
	}

	@Override
	public Collection<Pair<String, ItergameState>> validMoves() {
		Collection<Pair<Integer, BigInteger>> moves = o_x
				.getChildren((numPieces % 2 == 1) ? 'O' : 'X');
		ArrayList<Pair<String, ItergameState>> retMoves = new ArrayList<Pair<String, ItergameState>>(
				moves.size());
		for(Pair<Integer, BigInteger> move:moves){
			//TODO: Finish method; more complicated than it seems
		}
		return retMoves;
	}

	@Override
	public String describe() {
		return "TicTacToe";
	}

	@Override
	public int getDefaultBoardHeight() {
		return 3;
	}

	@Override
	public int getDefaultBoardWidth() {
		return 3;
	}

	@Override
	public char[] pieces() {
		return new char[] { 'X', 'O' };
	}
}
