package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.util.Pair;

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
		Color opposite() {
			switch (this) {
			case BLACK:
				return RED;
			case RED:
				return BLACK;
			default:
				return null;
			}
		}
	}

	private static final class Column {
		private final Piece[] piece;
		private final BigInteger tier;
		private int colHeight = 0;
		private BigInteger hash = BigInteger.ZERO;
		private BigInteger addRed = BigInteger.ZERO;
		private BigInteger addBlack = BigInteger.ZERO;

		Column(int height, BigInteger tier) {
			piece = new Piece[height];
			this.tier = tier;
		}

		void addPiece(Piece p) {
			piece[colHeight] = p;
			hash = hash.add(p.getHash());
			addRed = addRed.add(p.addRed());
			addBlack = addBlack.add(p.addBlack());
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
			addRed = addRed.subtract(piece[row].addRed()).add(p.addRed());
			addBlack = addBlack.subtract(piece[row].addBlack()).add(
					p.addBlack());
			piece[row] = p;
		}

		int height() {
			return colHeight;
		}

		boolean isOpen() {
			return colHeight != piece.length;
		}

		BigInteger addRed() {
			return addRed;
		}

		BigInteger addBlack() {
			return addBlack;
		}

		BigInteger getTier() {
			return tier;
		}

		Piece get(int row) {
			return piece[row];
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

		Color getColor() {
			return color;
		}
	}

	private final Column[] columns;
	private final int height, width;
	private final int openColumns;
	private final BigInteger pieces, blackPieces;
	private final BigInteger tier, maxPHash;
	private final Color turn;
	private BigInteger firstAll, firstBlacks;
	private BigInteger arHash = BigInteger.ZERO;

	/**
	 * @param height The height of the board
	 * @param width The width of the board
	 * @param tier Each digit in base(height+1) is the height of a column
	 */
	public FastBoard(int height, int width, BigInteger tier) {
		this.height = height;
		this.width = width;
		this.tier = tier;
		columns = new Column[width];
		int[] colHeight = new int[width];
		int pInt = 0;
		int col;
		BigInteger[] divmod;
		int oc = width;
		BigInteger hBig = BigInteger.valueOf(height + 1);
		BigInteger colTier = BigInteger.ONE;
		for (col = 0; col < width; col++) {
			columns[col] = new Column(height, colTier);
			divmod = tier.divideAndRemainder(hBig);
			colTier = colTier.multiply(hBig);
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
		Piece p = null;
		for (i = BigInteger.ZERO; i.compareTo(blackPieces) < 0; i = i
				.add(BigInteger.ONE)) {
			while (colHeight[col] == 0)
				col++;
			p = new Piece(i, i.add(BigInteger.ONE), BigInteger.ZERO,
					Color.BLACK);
			columns[col].addPiece(p);
			colHeight[col]--;
		}
		for (; i.compareTo(pieces) < 0; i = i.add(BigInteger.ONE)) {
			while (colHeight[col] == 0)
				col++;
			p = new Piece(i, blackPieces, p.nextRed(), Color.RED);
			columns[col].addPiece(p);
			colHeight[col]--;
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
		while (row >= columns[col].height()) {
			col++;
			row = 0;
		}
		BigInteger count;
		if (firstAll.compareTo(firstBlacks) == 0
				|| firstBlacks.compareTo(BigInteger.ONE) == 0) {
			int i;
			int lbInt = lastBlack.intValue();
			for (i = 0; i < lbInt; i++) {
				row++;
				while (row >= columns[col].height()) {
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
			while (row >= columns[col].height()) {
				col++;
				row = 0;
			}
			columns[col].setPiece(row, new Piece(firstAll, blacks
					.add(BigInteger.ONE), BigInteger.ONE, Color.BLACK));
			if (firstBlacks.compareTo(BigInteger.ONE) == 0) {
				firstBlacks = BigInteger.ZERO;
				do {
					firstBlacks = firstBlacks.add(BigInteger.ONE);
					firstAll = firstAll.add(BigInteger.ONE);
					row++;
					while (col < width && row == columns[col].height()) {
						col++;
						row = 0;
					}
				} while (col < width && get(row, col).getColor() == Color.BLACK);
			} else
				firstAll = firstBlacks = lastBlack;
		} else {
			Piece p = null;
			firstBlacks = firstBlacks.subtract(BigInteger.ONE);
			firstAll = firstBlacks;
			for (count = BigInteger.ZERO; count.compareTo(firstBlacks) < 0; count = count
					.add(BigInteger.ONE)) {
				p = new Piece(count, count.add(BigInteger.ONE),
						BigInteger.ZERO, Color.BLACK);
				columns[col].setPiece(row, p);
				row++;
				while (row == columns[col].height()) {
					col++;
					row = 0;
				}
			}
			for (; count.compareTo(lastBlack) < 0; count = count
					.add(BigInteger.ONE)) {
				p = new Piece(count, firstBlacks, p.nextRed(), Color.RED);
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
	 * @return The corresponding tiers and hashes of all possible moves from
	 *         this position.
	 */
	public ArrayList<Pair<BigInteger, BigInteger>> moveHashes() {
		ArrayList<Pair<BigInteger, BigInteger>> al = new ArrayList<Pair<BigInteger, BigInteger>>(
				openColumns);
		BigInteger newHash = arHash;
		if (turn == Color.BLACK) {
			for (int col = width - 1; col >= 0; col--) {
				if (columns[col].isOpen()) {
					int c = col;
					BigInteger contr;
					while (c >= 0 && columns[c].height() == 0)
						c--;
					if (c >= 0)
						contr = columns[c].topPiece().nextBlack();
					else
						contr = BigInteger.ZERO;
					al.add(new Pair<BigInteger, BigInteger>(tier
							.add(columns[col].getTier()), newHash.add(contr)));
				}
				newHash = newHash.add(columns[col].addBlack());
			}
		} else {
			for (int col = width - 1; col >= 0; col--) {
				if (columns[col].isOpen())
					al.add(new Pair<BigInteger, BigInteger>(tier
							.add(columns[col].getTier()), newHash));
				newHash = newHash.add(columns[col].addRed());
			}
		}
		return al;
	}

	public String toString() {
		StringBuilder str = new StringBuilder(height * (width + 3) + 1);
		Piece p;
		for (int row = height - 1; row >= 0; row--) {
			str.append('|');
			for (int col = 0; col < width; col++) {
				p = get(row, col);
				if (p == null)
					str.append(' ');
				else {
					switch (p.getColor()) {
					case RED:
						str.append('X');
						break;
					case BLACK:
						str.append('O');
						break;
					default:
						str.append('?');
						break;
					}
				}
			}
			str.append("|\n");
		}
		str.append("\n");
		return str.toString();
	}

	/**
	 * @param row The board row
	 * @param col The board column
	 * @return The piece on the board
	 */
	public Piece get(int row, int col) {
		return columns[col].get(row);
	}

	/**
	 * Return primitive "value". Usually this value includes WIN, LOSE, and
	 * perhaps TIE Return UNDECIDED if this is not a primitive state (shouldn't
	 * usually be called)
	 * 
	 * @param piecesToWin The number of pieces in a row necessary to win
	 * @return the Record representing the state
	 * @see edu.berkeley.gamesman.core.Record
	 */
	public PrimitiveValue primitiveValue(int piecesToWin) {
		int colHeight;
		boolean moreMoves = false;
		for (int col = 0; col < width; col++) {
			colHeight = columns[col].height();
			if (!moreMoves && colHeight < height)
				moreMoves = true;
			if (colHeight > 0
					&& get(colHeight - 1, col).getColor() == turn.opposite()
					&& checkLastWin(colHeight - 1, col, piecesToWin))
				return PrimitiveValue.Lose;
		}
		if (moreMoves)
			return PrimitiveValue.Undecided;
		else
			return PrimitiveValue.Tie;
	}

	private boolean checkLastWin(int row, int col, int piecesToWin) {
		Color turn = get(row, col).getColor();
		int ext;
		int stopPos;

		// Check horizontal win
		ext = 1;
		stopPos = Math.min(col, piecesToWin - ext);
		for (int i = 1; i < stopPos; i++)
			if (get(row, col - i).getColor() == turn)
				ext++;
			else
				break;
		stopPos = Math.min(width - col, piecesToWin - ext);
		for (int i = 1; i < stopPos; i++)
			if (get(row, col + i).getColor() == turn)
				ext++;
			else
				break;
		if (ext >= piecesToWin)
			return true;

		// Check DownLeft/UpRight Win
		ext = 1;
		stopPos = Math.min(Math.min(row, col), piecesToWin - ext);
		for (int i = 1; i < stopPos; i++)
			if (get(row - i, col - i).getColor() == turn)
				ext++;
			else
				break;
		stopPos = Math.min(Math.min(height - row, width - col), piecesToWin
				- ext);
		for (int i = 1; i < stopPos; i++)
			if (get(row + i, col + i).getColor() == turn)
				ext++;
			else
				break;
		if (ext >= piecesToWin)
			return true;

		// Check UpLeft/DownRight Win
		ext = 1;
		stopPos = Math.min(Math.min(height - row, col), piecesToWin - ext);
		for (int i = 1; i < stopPos; i++)
			if (get(row + i, col - i).getColor() == turn)
				ext++;
			else
				break;
		stopPos = Math.min(Math.min(row, width - col), piecesToWin - ext);
		for (int i = 1; i < stopPos; i++)
			if (get(row - i, col + i).getColor() == turn)
				ext++;
			else
				break;
		if (ext >= piecesToWin)
			return true;

		// Check Vertical Win: Since it's assumed x,y is on top, it's only
		// necessary to look down, not up
		if (row >= piecesToWin - 1)
			for (ext = 1; ext < piecesToWin; ext++)
				if (get(row - ext, col).getColor() != turn)
					break;
		if (ext >= piecesToWin)
			return true;
		return false;
	}

	/**
	 * @return Has this board reached its maximum hash?
	 */
	public boolean hasNext() {
		return arHash.add(BigInteger.ONE).compareTo(maxPHash) < 0;
	}

	/**
	 * @param args Empty
	 */
	public static void main(String[] args) {
		FastBoard fb = new FastBoard(6, 7, BigInteger.valueOf(2801));
		System.out.println(fb);
		System.out.println(fb.moveHashes());
		while (fb.hasNext()) {
			fb.next();
			System.out.println(fb);
			System.out.println(fb.moveHashes());
		}
	}
}
