package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredIterGame;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.game.util.PieceRearranger;
import edu.berkeley.gamesman.util.ExpCoefs;
import edu.berkeley.gamesman.util.Pair;

/**
 * Implementation of Connect 4 using the general IterArrangerHasher
 * 
 * @author DNSpies
 */
public class RConnect4 extends TieredIterGame {
	private final int[][] indices;

	private final int[] colHeights;

	private final int piecesToWin;

	private final int gameHeight, gameWidth, gameSize;

	private final ArrayList<Pair<Integer, Integer>> pieces;

	private final long[] multiplier;

	private final long[] moveArrangement;

	private final BitSetBoard bsb;

	private final ExpCoefs ec;

	private long pieceArrangement;

	private boolean hasNextPieceArrangement = false;

	private PieceRearranger iah;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public RConnect4(Configuration conf) {
		super(conf);
		gameWidth = conf.getInteger("gamesman.game.width", 7);
		gameHeight = conf.getInteger("gamesman.game.height", 6);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 4);
		indices = new int[gameHeight][gameWidth];
		for (int row = 0; row < gameHeight; row++)
			for (int col = 0; col < gameWidth; col++)
				indices[row][col] = -1;
		gameSize = gameWidth * gameHeight;
		pieces = new ArrayList<Pair<Integer, Integer>>(gameSize);
		moveArrangement = new long[gameWidth];
		colHeights = new int[gameWidth];
		bsb = new BitSetBoard(gameHeight, gameWidth);
		ec = new ExpCoefs(gameHeight, gameWidth + 1);
		multiplier = new long[gameSize + 1];
		multiplier[0] = 1;
		for (int i = 1; i <= gameSize; i++)
			multiplier[i] = multiplier[i - 1] * i / ((i + 1) / 2);
	}

	@Override
	public RConnect4 clone() {
		RConnect4 other = new RConnect4(conf);
		if (iah != null)
			other.setFromString(stateToString());
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
		return new ItergameState(pieces.size(), pieceArrangement
				* iah.colorArrangements + iah.getHash());
	}

	@Override
	public int getTier() {
		return pieces.size();
	}

	@Override
	public boolean hasNextHashInTier() {
		return iah.hasNext() || hasNextPieceArrangement;
	}

	@Override
	public void nextHashInTier() {
		if (iah.hasNext())
			changeBitSet(iah.next());
		else
			nextPieceArrangement();
	}

	private void nextPieceArrangement() {
		int col = 0;
		while (colHeights[col] == 0) {
			col++;
		}
		int pieceCount = 0;
		do {
			pieceCount += colHeights[col];
			col++;
		} while (colHeights[col] == gameHeight);
		colHeights[col]++;
		int numPieces = pieceCount - 1;
		pieceCount = 0;
		for (int i = 0; i < col; i++) {
			if (numPieces - pieceCount > gameHeight) {
				pieceCount += gameHeight;
				colHeights[i] = gameHeight;
			} else {
				colHeights[i] = numPieces - pieceCount;
				pieceCount = numPieces;
			}
		}
		if (numPieces == 0) {
			for (col++; col < gameWidth; col++)
				if (colHeights[col] < gameHeight)
					break;
			if (col == gameWidth)
				hasNextPieceArrangement = false;
		}
		pieceArrangement++;
		setToColHeights(pieces.size());
	}

	private void changeBitSet(PieceRearranger.ChangedPieces cp) {
		Pair<Integer, Integer> rowCol;
		while (cp.hasNext()) {
			rowCol = pieces.get(cp.next());
			bsb.flipPiece(rowCol.car, rowCol.cdr);
		}
	}

	@Override
	public BigInteger numHashesForTier() {
		return BigInteger.valueOf(ec.getCoef(gameWidth, pieces.size())
				* iah.colorArrangements);
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public PrimitiveValue primitiveValue() {
		char lastTurn = (pieces.size() % 2 == 1 ? 'X' : 'O');
		if (bsb.xInALine(piecesToWin, lastTurn))
			return PrimitiveValue.LOSE;
		else if (pieces.size() == gameSize)
			return PrimitiveValue.TIE;
		else
			return PrimitiveValue.UNDECIDED;
	}

	private char get(int row, int col) {
		if (indices[row][col] == -1)
			return ' ';
		else
			return iah.get(indices[row][col]);
	}

	@Override
	public void setStartingPosition(int n) {
		setNumPieces(0);
	}

	@Override
	public void setState(ItergameState pos) {
		setNumPieces(pos.tier());
		long mult = iah.colorArrangements;
		long hash = pos.hash();
		setArrangement(hash / mult);
		iah.setFromHash(hash % mult);
		setBSBfromIAH();
	}

	private void setArrangement(long arrange) {
		int pieceCount = pieces.size();
		for (int col = gameWidth - 1; col >= 0; col--) {
			colHeights[col] = 0;
			long tryHash = ec.getCoef(col, pieceCount);
			while (arrange >= tryHash) {
				arrange -= tryHash;
				pieceCount--;
				colHeights[col]++;
				tryHash = ec.getCoef(col, pieceCount);
			}
		}
		setToColHeights(pieces.size());
	}

	@Override
	public void setNumPieces(int numPieces) {
		int pieceCount = 0;
		for (int col = 0; col < gameWidth; col++) {
			if (numPieces - pieceCount > gameHeight) {
				pieceCount += gameHeight;
				colHeights[col] = gameHeight;
			} else {
				colHeights[col] = numPieces - pieceCount;
				pieceCount = numPieces;
			}
		}
		setToColHeights(numPieces);
	}

	private void setToColHeights(int numPieces) {
		int col = 0, row = 0;
		StringBuilder rearrangeString = new StringBuilder(numPieces + gameWidth);
		pieceArrangement = 0;
		hasNextPieceArrangement = false;
		int os = numPieces / 2;
		pieces.clear();
		bsb.clear();
		for (int i = 0; i < numPieces; i++) {
			while (row >= colHeights[col]) {
				if (colHeights[col] < gameHeight) {
					if (i > colHeights[col])
						hasNextPieceArrangement = true;
					rearrangeString.append(' ');
					for (; row < gameHeight; row++)
						indices[row][col] = -1;
				}
				col++;
				row = 0;
			}
			indices[row][col] = i;
			pieces.add(new Pair<Integer, Integer>(row, col));
			bsb.addPiece(row, col, i < os ? 'O' : 'X');
			rearrangeString.append('T');
			pieceArrangement += ec.getCoef(col, i + 1);
			row++;
		}
		if(colHeights[col]<gameHeight)
			rearrangeString.append(' ');
		for(col++;col<gameWidth;col++)
			rearrangeString.append(' ');
		int i = numPieces;
		long addValue = 0;
		for (col = gameWidth - 1; col >= 0; col--) {
			addValue += ec.getCoef(col, i + 1);
			moveArrangement[col] = pieceArrangement+addValue;
			i -= colHeights[col];
			addValue -= ec.getCoef(col, i + 1);
		}
		iah = new PieceRearranger(rearrangeString.toString(), os, numPieces
				- os);
	}

	@Override
	public BigInteger numHashesForTier(int numPieces) {
		return BigInteger.valueOf(ec.getCoef(gameWidth, numPieces)
				* multiplier[numPieces]);
	}

	@Override
	public void setFromString(String pos) {
		int numPieces = 0;
		StringBuilder iahString = new StringBuilder(gameSize);
		for (int col = 0; col < gameWidth; col++) {
			colHeights[col] = 0;
			for (int row = 0; row < gameHeight; row++) {
				char c = pos.charAt(row * gameWidth + col);
				if (c == ' ') {
					iahString.append(' ');
					break;
				} else {
					iahString.append(c);
					colHeights[col]++;
					numPieces++;
				}
			}
		}
		setToColHeights(numPieces);
		iah = new PieceRearranger(iahString.toString());
		setBSBfromIAH();
	}

	private void setBSBfromIAH(){
		bsb.clear();
		for(int i=0;i<pieces.size();i++){
			Pair<Integer,Integer> rowCol=pieces.get(i);
			bsb.addPiece(rowCol.car, rowCol.cdr, iah.get(i));
		}
	}
	
	@Override
	public String stateToString() {
		StringBuilder board = new StringBuilder(gameSize);
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				board.append(get(row, col));
			}
		}
		return board.toString();
	}

	@Override
	public Collection<Pair<String, ItergameState>> validMoves() {
		long[] children = iah.getChildren(pieces.size() % 2 == 1 ? 'O' : 'X');
		int nextNumPieces = pieces.size() + 1;
		ArrayList<Pair<String, ItergameState>> moves = new ArrayList<Pair<String, ItergameState>>(
				children.length);
		int col = 0;
		for (int i = 0; i < children.length; i++) {
			while (colHeights[col] == gameHeight)
				col++;
			moves.add(new Pair<String, ItergameState>(String.valueOf(col),
					new ItergameState(nextNumPieces, moveArrangement[col]
							* multiplier[nextNumPieces] + children[i])));
			col++;
		}
		return moves;
	}

	@Override
	public String describe() {
		return String.format("%dx%d RConnect %d", gameWidth, gameHeight,
				piecesToWin);
	}

	@Override
	public int numberOfTiers() {
		return gameSize + 1;
	}

}
