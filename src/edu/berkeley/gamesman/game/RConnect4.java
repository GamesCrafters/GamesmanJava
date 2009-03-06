package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.game.connect4.C4Board;
import edu.berkeley.gamesman.game.connect4.C4Piece;
import edu.berkeley.gamesman.game.util.PieceRearranger;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * Implementation of Connect 4 using the general IterArrangerHasher
 * 
 * @author DNSpies
 */
public class RConnect4 extends TieredIterGame {
	private final int[][] indices;
	private final int piecesToWin;
	private final ArrayList<Pair<Integer, Integer>> pieces;
	private final int[] moveTiers;
	private int tier;
	private C4Board myBoard;
	private PieceRearranger iah;

	/**
	 * @param conf The configuration object
	 */
	public RConnect4(Configuration conf) {
		super(conf);
		indices = new int[gameHeight][gameWidth];
		pieces = new ArrayList<Pair<Integer, Integer>>(gameWidth * gameHeight);
		piecesToWin = Integer
				.parseInt(conf.getProperty("connect4.pieces", "4"));
		moveTiers = new int[gameWidth];
	}

	@Override
	public RConnect4 clone() {
		RConnect4 other = new RConnect4(conf);
		if (myBoard != null)
			other.setToString(stateToString());
		return other;
	}

	@Override
	public String displayState() {
		String s = stateToString();
		StringBuffer str = new StringBuffer((gameWidth + 3) * gameHeight);
		for (int row = gameHeight - 1; row >= 0; row--)
			str.append("|"
					+ s.substring(row * gameWidth, (row + 1) * gameWidth)
					+ "|\n");
		return str.toString();
	}

	@Override
	public ItergameState getState() {
		return new ItergameState(tier, iah.getHash());
	}

	@Override
	public int getTier() {
		return tier;
	}

	@Override
	public boolean hasNextHashInTier() {
		return iah.hasNext();
	}

	@Override
	public void nextHashInTier() {
		iah.next();
	}

	@Override
	public BigInteger numHashesForTier() {
		return iah.arrangements;
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public PrimitiveValue primitiveValue() {
		return myBoard.primitiveValue(piecesToWin);
	}

	private char get(int row, int col) {
		return iah.get(indices[row][col]);
	}

	@Override
	public void setStartingPosition(int n) {
		setTier(0);
	}

	@Override
	public void setState(ItergameState pos) {
		setTier(pos.tier());
		iah.unHash(pos.hash());
	}

	@Override
	public void setTier(int tier) {
		int colHeights[] = new int[gameWidth];
		int colHash = 1;
		this.tier = tier;
		pieces.clear();
		int numPieces = 0;
		StringBuilder s = new StringBuilder(gameWidth * gameHeight);
		for (int col = 0; col < gameWidth; col++) {
			colHeights[col] = tier % (gameHeight + 1);
			numPieces += colHeights[col];
			tier /= (gameHeight + 1);
			int row;
			for (row = 0; row < colHeights[col]; row++) {
				s.append('T');
				indices[row][col] = pieces.size();
				pieces.add(new Pair<Integer, Integer>(row, col));
			}
			for (; row < gameHeight; row++)
				indices[row][col] = pieces.size();
			if (colHeights[col] < gameHeight) {
				s.append(' ');
				pieces.add(new Pair<Integer, Integer>(row, col));
				moveTiers[col] = this.tier + colHash;
			}
			colHash *= gameHeight + 1;
		}
		try {
			iah = new PieceRearranger(s.toString(), numPieces / 2,
					(numPieces + 1) / 2);
			myBoard = new C4Board(makePieceBoard(),
					(numPieces % 2 == 1) ? C4Piece.BLACK : C4Piece.RED,
					colHeights);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public BigInteger numHashesForTier(int tier) {
		int totPieces = 0;
		for (int col = 0; col < gameWidth; col++) {
			totPieces += tier % (gameHeight + 1);
			tier /= (gameHeight + 1);
		}
		return BigInteger.valueOf(Util.nCr(totPieces, totPieces / 2));
	}

	private C4Piece[][] makePieceBoard() {
		C4Piece[][] board = new C4Piece[gameHeight][gameWidth];
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board[row][col] = C4Piece.toPiece(get(row, col));
			}
		}
		return board;
	}

	@Override
	public void setToString(String pos) {
		int colHeights[] = new int[gameWidth];
		pieces.clear();
		StringBuilder iahPos = new StringBuilder(gameWidth * gameHeight);
		int numPieces = 0;
		int row = 0, col = 0;
		int index = 0;
		for (int i = 0; i < pos.length(); i++) {
			switch (pos.charAt(i)) {
			case 'O':
				indices[row][col] = index;
				iahPos.append('O');
				pieces.add(new Pair<Integer, Integer>(row, col));
				numPieces++;
				break;
			case 'X':
				indices[row][col] = index;
				iahPos.append('X');
				pieces.add(new Pair<Integer, Integer>(row, col));
				numPieces++;
				break;
			case ' ':
				iahPos.append(' ');
				pieces.add(new Pair<Integer, Integer>(row, col));
				for (; row < gameHeight; row++) {
					i++;
					indices[row][col] = index;
				}
				break;
			}
			row++;
			if (row >= gameHeight) {
				row = 0;
				col++;
			}
			index++;
		}
		try {
			iah = new PieceRearranger(iahPos.toString(), numPieces / 2,
					(numPieces + 1) / 2);
			myBoard = new C4Board(makePieceBoard(),
					numPieces % 2 == 1 ? C4Piece.BLACK : C4Piece.RED,
					colHeights);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String stateToString() {
		StringBuilder board = new StringBuilder(gameHeight * gameWidth);
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board.append(get(row, col));
			}
		}
		return board.toString();
	}

	@Override
	public Collection<Pair<String, ItergameState>> validMoves() {
		Collection<Pair<Integer, BigInteger>> children = iah
				.getChildren(myBoard.getTurn().toChar());
		ArrayList<Pair<String, ItergameState>> moves = new ArrayList<Pair<String, ItergameState>>(
				children.size());
		for (Pair<Integer, BigInteger> p : children) {
			int col = pieces.get(p.car).cdr;
			moves.add(new Pair<String, ItergameState>("c" + col,
					new ItergameState(moveTiers[col], p.cdr)));
		}
		return moves;
	}

	@Override
	public String describe() {
		return "IAH Connect 4";
	}

	@Override
	public int getDefaultBoardHeight() {
		return 6;
	}

	@Override
	public int getDefaultBoardWidth() {
		return 7;
	}

	@Override
	public char[] pieces() {
		return new char[] { 'X', 'O' };
	}

	@Override
	public String toString() {
		return "Connect 4";
	}

	@Override
	public int numberOfTiers() {
		return (int) Util.longpow(gameHeight + 1, gameWidth);
	}

}
