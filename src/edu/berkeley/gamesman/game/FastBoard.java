package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * @author DNSpies Implements super-fast move-hashing and cycling. Use next() to
 *         cycle to the next board hash. Use getMoves() to get the hashes of all
 *         possible moves in a given board. Warning 1: Use only with a
 *         tier-solver. To write an equivalent class for a top-down solver,
 *         certain final fields must be made variable, move functions must be
 *         rewritten, and cycling capability is unnecessary. Warning 2:
 *         Unhashing (while unnecessary) is virtually impossible in any
 *         reasonable amount of time. Hashing is a strictly one-way function.
 *         Please do not make any changes to this class. It's guaranteed to be
 *         buggy, but I'd like to fix it myself. My commenting is not currently
 *         sufficient for anyone else to understand it.
 */
public final class FastBoard {
	private static enum Color {
		BLACK, RED;
	}

	private static final class Column {
		private final Piece[] piece;
		private int colHeight = 0;
		private BigInteger hash = BigInteger.ZERO;
		private BigInteger addRed = BigInteger.ZERO;
		private BigInteger addBlack = BigInteger.ZERO;

		Column(int height) {
			piece = new Piece[height];
		}

		void addPiece(Piece p) {
			piece[colHeight] = p;
			hash.add(p.getHash());
			addRed.add(p.addRed());
			addBlack.add(p.addBlack());
			colHeight++;
		}

		Piece topPiece() {
			if (colHeight > 0)
				return piece[colHeight - 1];
			else
				return null;
		}

		void setPiece(int row, Piece p) {
			hash = hash.subtract(piece[row].getHash()).add(p.getHash());
			addRed = hash.subtract(piece[row].addRed()).add(p.addRed());
			addBlack = hash.subtract(piece[row].addBlack()).add(p.addBlack());
			piece[row] = p;
		}

		int height() {
			return colHeight;
		}

		boolean isOpen() {
			return colHeight != piece.length;
		}

		public BigInteger addRed() {
			return addRed;
		}

		public BigInteger addBlack() {
			return addBlack;
		}
	}

	private static final class Piece {
		private final BigInteger hash;
		private final BigInteger addRed;
		private final BigInteger addBlack;
		private final Color color;

		Piece(BigInteger index, BigInteger black, BigInteger hashNum,
				Color color) {
			this.hash = hashNum;
			if (hashNum.compareTo(BigInteger.ZERO) > 0) {
				BigInteger addVal = hashNum.multiply(index.add(BigInteger.ONE));
				addRed = addVal.divide(index.add(BigInteger.ONE)
						.subtract(black));
				addBlack = addVal.divide(black.add(BigInteger.ONE));
			} else {
				addRed = BigInteger.ONE;
				addBlack = BigInteger.ZERO;
			}
			this.color = color;
		}

		BigInteger nextBlack() {
			return addBlack;
		}

		BigInteger nextRed() {
			return addRed;
		}

		BigInteger addBlack() {
			if (color == Color.BLACK)
				return addBlack.subtract(hash);
			else
				return BigInteger.ZERO;
		}

		BigInteger addRed() {
			if (color == Color.BLACK)
				return addRed.subtract(hash);
			else
				return BigInteger.ZERO;
		}

		BigInteger getHash() {
			return hash;
		}
	}

	private final Column[] columns;
	private final int width;
	private final int openColumns;
	private final BigInteger pieces, blackPieces;
	private final BigInteger maxPHash;
	private final Color turn;
	private BigInteger firstAll, firstBlacks;
	private BigInteger arHash = BigInteger.ZERO;

	/**
	 * @param height The height of the board
	 * @param width The width of the board
	 * @param tier Each digit in base(height+1) is the height of a column
	 */
	public FastBoard(int height, int width, BigInteger tier) {
		this.width = width;
		this.columns = new Column[width];
		int[] colHeight = new int[width];
		int pInt = 0;
		int col;
		BigInteger[] divmod;
		int oc = width;
		BigInteger hBig = BigInteger.valueOf(height + 1);
		for (col = 0; col < width; col++) {
			divmod = tier.divideAndRemainder(hBig);
			colHeight[col] = divmod[1].intValue();
			if (colHeight[col] == height)
				oc--;
			pInt += colHeight[col];
			tier = divmod[0];
		}
		pieces = BigInteger.valueOf(pInt);
		turn = (pInt % 2 == 1) ? Color.BLACK : Color.RED;
		openColumns = oc;
		firstAll = firstBlacks = blackPieces = BigInteger.valueOf(pInt / 2);
		col = 0;
		BigInteger i;
		for (i = BigInteger.ZERO; i.compareTo(blackPieces) < 0; i = i
				.add(BigInteger.ONE)) {
			while (colHeight[col] == 0)
				col++;
			columns[col].addPiece(new Piece(i, i.add(BigInteger.ONE),
					BigInteger.ZERO, Color.BLACK));
			colHeight[col]--;
		}
		Piece p = columns[col].topPiece();
		for (; i.compareTo(pieces) < 0; i = i.add(BigInteger.ONE)) {
			while (colHeight[col] == 0)
				col++;
			p = new Piece(i, blackPieces, p.nextRed(), Color.RED);
			columns[col].addPiece(p);
		}
		maxPHash = p.nextRed();
	}

	/**
	 * @return The number of possible combinations
	 */
	public BigInteger maxHash() {
		return maxPHash;
	}

	/**
	 * Changes the positions of the pieces such that the board now hashes to one
	 * more than it previously did. Will throw an exception if already at the
	 * last possible hash.
	 */
	public void next() {
		int col = 0, row = 0;
		BigInteger lastBlack = firstAll.subtract(BigInteger.ONE);
		while (row == columns[col].height()) {
			col++;
			row = 0;
		}
		BigInteger i;
		if (firstAll.compareTo(firstBlacks) == 0
				|| firstBlacks.compareTo(BigInteger.ONE) == 0) {
			for (i = BigInteger.ZERO; i.compareTo(lastBlack) < 0; i = i
					.add(BigInteger.ONE)) {
				row++;
				while (row == columns[col].height()) {
					col++;
					row = 0;
				}
			}
			BigInteger blacks;
			if (firstBlacks.compareTo(BigInteger.ONE) == 0)
				blacks = BigInteger.ZERO;
			else
				blacks = lastBlack;
			columns[col].setPiece(row, new Piece(lastBlack, blacks,
					BigInteger.ONE, Color.RED));
			row++;
			while (row == columns[col].height()) {
				col++;
				row = 0;
			}
			columns[col].setPiece(row, new Piece(firstAll, blacks
					.add(BigInteger.ONE), BigInteger.ONE, Color.RED));
			if (firstBlacks.compareTo(BigInteger.ONE) == 0)
				firstAll = firstAll.add(BigInteger.ONE);
			else
				firstAll = firstBlacks = lastBlack;
		} else {
			Piece p = null;
			firstAll = firstBlacks = firstBlacks.subtract(BigInteger.ONE);
			for (i = BigInteger.ZERO; i.compareTo(firstBlacks) < 0; i = i
					.add(BigInteger.ONE)) {
				p = new Piece(i, i.add(BigInteger.ONE), BigInteger.ZERO,
						Color.BLACK);
				columns[col].setPiece(row, p);
				row++;
				while (row == columns[col].height()) {
					col++;
					row = 0;
				}
			}
			for (; i.compareTo(lastBlack) < 0; i = i.add(BigInteger.ONE)) {
				p = new Piece(i, blackPieces, p.nextRed(), Color.RED);
				columns[col].setPiece(row, p);
				row++;
				while (row == columns[col].height()) {
					col++;
					row = 0;
				}
			}
			p = new Piece(lastBlack, firstBlacks, p.nextRed(), Color.RED);
			columns[col].setPiece(row, p);
			row++;
			while (row == columns[col].height()) {
				col++;
				row = 0;
			}
			p = new Piece(lastBlack.add(BigInteger.ONE), firstBlacks
					.add(BigInteger.ONE), p.nextBlack(), Color.BLACK);
			columns[col].setPiece(row, p);
		}
		arHash = arHash.add(BigInteger.ONE);
	}

	/**
	 * @return The hashes of all possible moves from this position
	 */
	public ArrayList<BigInteger> moveHashes() {
		ArrayList<BigInteger> al = new ArrayList<BigInteger>(openColumns);
		BigInteger newHash = arHash;
		if (turn == Color.BLACK) {
			for (int col = width - 1; col >= 0; col--) {
				if (columns[col].isOpen())
					al.add(newHash.add(columns[col].topPiece().nextBlack()));
				newHash = newHash.add(columns[col].addBlack());
			}
		} else {
			for (int col = width - 1; col >= 0; col--) {
				if (columns[col].isOpen())
					al.add(newHash);
				newHash = newHash.add(columns[col].addRed());
			}
		}
		return al;
	}
}
