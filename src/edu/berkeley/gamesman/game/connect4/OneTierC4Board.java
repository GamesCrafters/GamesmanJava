package edu.berkeley.gamesman.game.connect4;

import java.math.BigInteger;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 * 
 */
public final class OneTierC4Board {
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

	/*
	 * Why must a column be a class as opposed to just the second dimension of
	 * an array? Because there's so many beneficial ways a column can speed up
	 * hashing!
	 */
	private static final class Column {
		private final Piece[] piece;
		private final int tier;
		private int colHeight = 0;
		private BigInteger hash = BigInteger.ZERO;
		private BigInteger addRed = BigInteger.ZERO;
		private BigInteger addBlack = BigInteger.ZERO;

		Column(final int height, final int tier) {
			piece = new Piece[height];
			this.tier = tier;
		}

		void addPiece(final Piece p) {
			piece[colHeight] = p;
			hash = hash.add(p.getHash());
			addRed = addRed.add(p.addRed());
			addBlack = addBlack.add(p.addBlack());
			colHeight++;
		}

		Piece topPiece() {
			return piece[colHeight - 1];
		}

		void setPiece(final int row, final BigInteger black,
				final BigInteger hashNum, final Color color) {
			hash = hash.subtract(piece[row].getHash());
			addRed = addRed.subtract(piece[row].addRed());
			addBlack = addBlack.subtract(piece[row].addBlack());
			piece[row].reset(black, hashNum, color);
			hash = hash.add(piece[row].getHash());
			addRed = addRed.add(piece[row].addRed());
			addBlack = addBlack.add(piece[row].addBlack());
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

		int getTier() {
			return tier;
		}

		Piece get(final int row) {
			return piece[row];
		}
	}

	private static final class Piece {
		private final BigInteger index;
		private BigInteger hash;
		private BigInteger addRed;
		private BigInteger addBlack;
		private Color color;

		Piece(final BigInteger index, final BigInteger black,
				final BigInteger hashNum, final Color color) {
			hash = hashNum;
			this.index = index;
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

		/*
		 * This method is exactly the same as the contructor. It's to avoid
		 * having to constantly be creating and destroying pieces
		 */
		void reset(final BigInteger black, final BigInteger hashNum,
				final Color color) {
			hash = hashNum;
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
	private final int height, width, piecesToWin;
	private final int openColumns;
	private final BigInteger pieces, blackPieces;
	private final int tier;
	private final BigInteger maxPHash;
	private final Color turn;
	private final ArrayList<Integer> moveTiers;
	private BigInteger firstAll, firstBlacks;
	private BigInteger arHash = BigInteger.ZERO;

	OneTierC4Board(int width, int height, int piecesToWin, int tier) {
		this.width = width;
		this.height = height;
		this.piecesToWin = piecesToWin;
		this.tier = tier;
		columns = new Column[width];
		int[] colHeight = new int[width];
		int pInt = 0;
		int col;
		int oc = width;
		int colTier = 1;
		for (col = 0; col < width; col++) {
			columns[col] = new Column(height, colTier);
			colTier *= (height + 1);
			colHeight[col] = tier % (height + 1);
			if (colHeight[col] == height)
				oc--;
			pInt += colHeight[col];
			tier /= height + 1;
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
			if (p == null)
				p = new Piece(i, BigInteger.ZERO, BigInteger.ONE, Color.RED);
			else
				p = new Piece(i, blackPieces, p.nextRed(), Color.RED);
			columns[col].addPiece(p);
			colHeight[col]--;
		}
		if (this.tier == 0)
			maxPHash = BigInteger.ONE;
		else
			maxPHash = p.nextRed();
		moveTiers = new ArrayList<Integer>(openColumns);
		for (col = width - 1; col >= 0; col--) {
			if (columns[col].isOpen())
				moveTiers.add(tier + columns[col].getTier());
		}
	}

	public BigInteger numHashesForTier() {
		return maxPHash;
	}

	/*
	 * A quick explanation of the iteration process: Given some lineup:
	 * RRRBBBRRBBRBB To find the next lineup, switch all the first R's with all
	 * but one of the first B's BBRRRBRRBBRBB (Ignore this step if there's no
	 * R's at the beginning or only one B following them) Then switch the next B
	 * with the next R BBRRRRBRBBRBB Unfortunately, it's slightly more
	 * complicated to do this while keeping track of hash contributions. But
	 * still possible (in the same order of time)
	 */
	public void next() {
		int col = 0, row = 0;
		BigInteger lastBlack = firstAll.subtract(BigInteger.ONE);
		while (columns[col].height() == 0)
			col++;
		BigInteger count;
		if (firstAll.compareTo(firstBlacks) == 0
				|| firstBlacks.equals(BigInteger.ONE)) {
			row = lastBlack.intValue();
			while (row >= columns[col].height()) {
				row -= columns[col].height();
				col++;
			}
			BigInteger blacks;
			if (firstBlacks.equals(BigInteger.ONE))
				blacks = BigInteger.ZERO;
			else
				blacks = lastBlack;
			set(row, col, blacks, BigInteger.ONE, Color.RED);
			Piece p = get(row, col);
			row++;
			while (row >= columns[col].height()) {
				col++;
				row = 0;
			}
			set(row, col, blacks.add(BigInteger.ONE), p.nextBlack(),
					Color.BLACK);
			if (firstBlacks.equals(BigInteger.ONE)) {
				firstBlacks = BigInteger.ZERO;
				do {
					firstBlacks = firstBlacks.add(BigInteger.ONE);
					firstAll = firstAll.add(BigInteger.ONE);
					p = get(row, col);
					row++;
					while (col < width && row >= columns[col].height()) {
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
				set(row, col, count.add(BigInteger.ONE), BigInteger.ZERO,
						Color.BLACK);
				p = get(row, col);
				row++;
				while (row >= columns[col].height()) {
					col++;
					row = 0;
				}
			}
			for (; count.compareTo(lastBlack) < 0; count = count
					.add(BigInteger.ONE)) {
				set(row, col, firstBlacks, p.nextRed(), Color.RED);
				p = get(row, col);
				row++;
				while (row >= columns[col].height()) {
					col++;
					row = 0;
				}
			}
			set(row, col, firstBlacks, p.nextRed(), Color.RED);
			p = get(row, col);
			row++;
			while (row >= columns[col].height()) {
				col++;
				row = 0;
			}
			set(row, col, firstBlacks.add(BigInteger.ONE), p.nextBlack(),
					Color.BLACK);
		}
		arHash = arHash.add(BigInteger.ONE);
	}

	private void set(final int row, final int col, final BigInteger black,
			final BigInteger hashNum, final Color color) {
		columns[col].setPiece(row, black, hashNum, color);
	}

	public BigInteger getHash() {
		return arHash;
	}

	/*
	 * Adding a red piece to the rightmost column never changes the hash. Adding
	 * a black piece only by the piece's own hash contribution. The following
	 * code is surprisingly short thanks to all the information the columns and
	 * pieces maintain about themselves. Who would have thought you could
	 * simultaneously hash all the moves of a full-size connect four board by
	 * adding eight or nine numbers together?
	 */
	public ArrayList<Pair<Integer, BigInteger>> moveHashes() {
		ArrayList<Pair<Integer, BigInteger>> al = new ArrayList<Pair<Integer, BigInteger>>(
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
					al.add(new Pair<Integer, BigInteger>(col, newHash
							.add(contr)));
				}
				newHash = newHash.add(columns[col].addBlack());
			}
		} else {
			for (int col = width - 1; col >= 0; col--) {
				if (columns[col].isOpen())
					al.add(new Pair<Integer, BigInteger>(col, newHash));
				newHash = newHash.add(columns[col].addRed());
			}
		}
		return al;
	}

	public int getTier() {
		return tier;
	}

	public ArrayList<Integer> moveTiers() {
		return moveTiers;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(height * (2 * width + 2) + 1);
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
				str.append('|');
			}
			str.append('\n');
		}
		str.append('\n');
		return str.toString();
	}

	Piece get(final int row, final int col) {
		return columns[col].get(row);
	}

	/*
	 * Look for pieces that might have been the last move made (at the top of
	 * they're column and of the right color. Then return lose if the piece is
	 * part of a four-in-a-row. Otherwise return Tie or Undecided depending on
	 * whether the board is full or not.
	 */
	public PrimitiveValue primitiveValue() {
		int colHeight;
		for (int col = 0; col < width; col++) {
			colHeight = columns[col].height();
			if (colHeight > 0
					&& columns[col].topPiece().getColor() == turn.opposite()
					&& checkLastWin(colHeight - 1, col))
				return PrimitiveValue.Lose;
		}
		if (openColumns > 0)
			return PrimitiveValue.Undecided;
		else
			return PrimitiveValue.Tie;
	}

	public void unhash(BigInteger hash) {
		BigInteger thisHash = numHashesForTier();
		BigInteger pieces;
		BigInteger blackPieces = this.blackPieces;
		int col = width - 1;
		int row = columns[col].colHeight;
		for (pieces = this.pieces; pieces.compareTo(BigInteger.ZERO) > 0; pieces = pieces
				.subtract(BigInteger.ONE)) {
			thisHash = thisHash.multiply(pieces.subtract(blackPieces)).divide(
					pieces);
			row--;
			while (row < 0) {
				col--;
				row = columns[col].colHeight - 1;
			}
			if (hash.compareTo(thisHash) >= 0) {
				get(row, col).reset(blackPieces, thisHash, Color.BLACK);
				blackPieces = blackPieces.subtract(BigInteger.ONE);
			}else
				get(row, col).reset(blackPieces, thisHash, Color.RED);
		}
	}
	
	/*
	 * Looks for a win that uses the given piece.
	 */
	private boolean checkLastWin(final int row, final int col) {
		Color turn = get(row, col).getColor();
		int ext;
		int stopPos;
		Piece p;

		// Check horizontal win
		ext = 1;
		stopPos = Math.min(col, piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++) {
			p = get(row, col - i);
			if (p != null && p.getColor() == turn)
				ext++;
			else
				break;
		}
		stopPos = Math.min(width - 1 - col, piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++) {
			p = get(row, col + i);
			if (p != null && p.getColor() == turn)
				ext++;
			else
				break;
		}
		if (ext >= piecesToWin)
			return true;

		// Check DownLeft/UpRight Win
		ext = 1;
		stopPos = Math.min(Math.min(row, col), piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++) {
			p = get(row - i, col - i);
			if (p != null && p.getColor() == turn)
				ext++;
			else
				break;
		}
		stopPos = Math.min(Math.min(height - 1 - row, width - 1 - col),
				piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++) {
			p = get(row + i, col + i);
			if (p != null && p.getColor() == turn)
				ext++;
			else
				break;
		}
		if (ext >= piecesToWin)
			return true;

		// Check UpLeft/DownRight Win
		ext = 1;
		stopPos = Math.min(Math.min(height - 1 - row, col), piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++) {
			p = get(row + i, col - i);
			if (p != null && p.getColor() == turn)
				ext++;
			else
				break;
		}
		stopPos = Math.min(Math.min(row, width - 1 - col), piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++) {
			p = get(row - i, col + i);
			if (p != null && p.getColor() == turn)
				ext++;
			else
				break;
		}
		if (ext >= piecesToWin)
			return true;

		// Check Vertical Win: Since it's assumed x,y is on top, it's only
		// necessary to look down, not up
		if (row >= piecesToWin - 1)
			for (ext = 1; ext < piecesToWin; ext++) {
				if (get(row - ext, col).getColor() != turn)
					break;
			}
		if (ext >= piecesToWin)
			return true;
		return false;
	}
}
