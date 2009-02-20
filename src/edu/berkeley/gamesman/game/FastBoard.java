package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.PrimitiveValue;

/**
 * @author DNSpies Implements super-fast move-hashing and cycling. Use next() to
 *         cycle to the next board hash. Use getMoves() to get the hashes of all
 *         possible moves in a given board. Warning 1: Use only with a
 *         tier-solver. To write an equivalent class for a top-down solver,
 *         certain final fields must be made variable, move functions must be
 *         rewritten, and cycling capability is unnecessary. Warning 2:
 *         Unhashing is both unnecessary and virtually impossible in any
 *         reasonable amount of time. Hashing is a strictly one-way function.
 */
/*
 * Please do not make any changes to this class. There are still bugs but I'd
 * like to fix them myself. If you auto-format (ctrl-shift-f in eclipse) you
 * die. It took me a long time to comment this and I'm not gonna have some
 * brainless IDE messing it up
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

	/* Why must a column be a class as opposed to just the second dimension of an array?
	 * Because there's so many beneficial ways a column can speed up hashing!
	 */
	private static final class Column {
		private final Piece[] piece;
		private final int tier;					//When a piece is added to this column, the tier always increases by this amount.
		private int colHeight = 0;
		private BigInteger hash = BigInteger.ZERO;		//The sum of each of the hash contributions of the pieces in the column
		private BigInteger addRed = BigInteger.ZERO;	//The sum of each of the addRed's of the pieces in the columns
		private BigInteger addBlack = BigInteger.ZERO;	//Take a guess

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
		private final BigInteger index;	//This piece's index
		private BigInteger hash;	//This piece's contribution to the board hash (if this piece is black, if it's red the contribution is always zero)
		private BigInteger addRed;	//This piece's contribution to the board hash after a red piece is added somewhere to the left of it (if this piece is black)
		private BigInteger addBlack;	//This piece's contribution after a black piece is added...
		private Color color;

		Piece(final BigInteger index, final BigInteger black, final BigInteger hashNum,
				final Color color) {
			hash = hashNum;
			this.index=index;
			if (hashNum.compareTo(BigInteger.ZERO) > 0) {							//
				BigInteger addVal = hashNum.multiply(index.add(BigInteger.ONE));	// Yay for combinatorics
				addRed = addVal.divide(index.add(BigInteger.ONE)					// Most of the combinatorial
						.subtract(black));											// work is done right here
				addBlack = addVal.divide(black.add(BigInteger.ONE));				// 
			} else {																// Thanks to memoizing the previous
				addRed = BigInteger.ONE;											// piece's hash, all chooses are
				addBlack = BigInteger.ZERO;											// calculated in constant time
			}																		// (One multiplication and one division)
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
			return addBlack;			// Through a fortunate coincidence, addBlack is also the next piece's
		}								// hash value if the next piece is black.

		BigInteger nextRed() {
			return addRed;				// Through another no less astonishing coincidence, the same applies
		}								// to addRed and red pieces (except that red pieces don't really have
										// a hash value of their own.  They just help keep track of black hash values)
		BigInteger addBlack() {
			if (color == Color.BLACK)				// So suppose I already know the current hash
				return addBlack.subtract(hash);		// and I just want to calculate the change
			else									// whenever a black move is made to the left...
				return BigInteger.ZERO;				// Subtraction to the rescue
		}

		BigInteger addRed() {						// Same goes for red pieces
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
	private final int openColumns;					// = number of possible moves... now I know what size array to return.
	private final BigInteger pieces, blackPieces;	// Pieces on the board, black pieces on the board (in case you didn't guess)
	private final int tier;
	private final BigInteger maxPHash;	// maxPHash = number of positions in this tier
	private final Color turn;
	private BigInteger firstAll, firstBlacks; 	// firstAll = the index at which the first uninterupted
												// (possibly empty) chain of red pieces followed by the first
												// uninterrupted (never empty) chain of black pieces terminates.
												//
												// firstBlacks = the number of black pieces in firstAll
	private BigInteger arHash = BigInteger.ZERO;	//The hash itself

	/**
	 * First position = All black pieces followed by all red pieces (hash is zero)
	 * @param height The height of the board
	 * @param width The width of the board
	 * @param tier Each digit in base(height+1) is the height of a column
	 */
	public FastBoard(final int height, final int width, int tier) {
		this.height = height;
		this.width = width;
		this.tier = tier;
		columns = new Column[width];
		int[] colHeight = new int[width];	//For temporary use
		int pInt = 0;		//--> Makes things easier and faster to
		int col;			//    use ints instead of BigIntegers
		int oc = width;	// Open columns, not Orange County
		int colTier=1;
		for (col = 0; col < width; col++) {				// a) Determines column heights by unhashing tier number
			columns[col] = new Column(height, colTier);	//   (If cycling through tiers, this can be done in constant time
			colTier *= (height + 1);					//    but since there are so many more hashes than tiers, it hardly
			colHeight[col] = tier%(height+1);			//    seems worthwhile)
			if (colHeight[col] == height)				// 
				oc--;									// b) Initializes columns along with their tier values
			pInt += colHeight[col];						//
			tier /= height + 1;							// c) Determines the number of pieces on the board
		}												//
		pieces = BigInteger.valueOf(pInt);				//
		turn = (pInt % 2 == 1) ? Color.BLACK : Color.RED;	// Red always goes first: Smoke before... my ass
		openColumns = oc;	// I told you.  This program is SoCal-free
		firstAll = firstBlacks = blackPieces = BigInteger.valueOf(pInt / 2);	//Got some serious initialization goin' on up in here
		col = 0;
		BigInteger i;
		Piece p = null;	//Compiler complains if I don't have the = null; part
		for (i = BigInteger.ZERO; i.compareTo(blackPieces) < 0; i = i	//for(i=0;i<n;i++) in BigInteger notation
				.add(BigInteger.ONE)) {
			while (colHeight[col] == 0)
				col++;
			p = new Piece(i, i.add(BigInteger.ONE), BigInteger.ZERO,
					Color.BLACK);
			columns[col].addPiece(p);
			colHeight[col]--;	//Told you it was temporary.  Column objects keep track of their own heights
		}
		for (; i.compareTo(pieces) < 0; i = i.add(BigInteger.ONE)) {			// If it's not black, it's red
			while (colHeight[col] == 0)											//
				col++;															// All other colors are illusions
			if (p == null)														// created by the media to blind us
				p = new Piece(i, BigInteger.ZERO, BigInteger.ONE, Color.RED);	// from the truth...
			else																//
			p = new Piece(i, blackPieces, p.nextRed(), Color.RED);				// That Keanu Reeves can't act
			columns[col].addPiece(p);											//
			colHeight[col]--;													//
		}
		if (this.tier == 0)
			maxPHash = BigInteger.ONE;
		else
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
	/*
	 * A quick explanation of the iteration process:
	 * Given some lineup: RRRBBBRRBBRBB
	 * To find the next lineup, switch all the first R's with all but one of the first B's
	 * BBRRRBRRBBRBB
	 * (Ignore this step if there's no R's at the beginning or only one B following them)
	 * Then switch the next B with the next R
	 * BBRRRRBRBBRBB
	 * Unfortunately, it's slightly more complicated to do this while keeping track
	 * of hash contributions.  But still possible (in the same order of time)
	 */
	public void next() {
		int col = 0, row = 0;
		BigInteger lastBlack = firstAll.subtract(BigInteger.ONE);	// The index of the last black piece
																	// in the first line of black pieces
																	// This one's special
		while (columns[col].height() == 0)
			col++;
		BigInteger count;
		if (firstAll.compareTo(firstBlacks) == 0			// A little strange that these two seemingly
				|| firstBlacks.equals(BigInteger.ONE)) {	// unrelated conditions have almost the same result:
															// Ignore the initial mass switch and only do the small switch
			row = lastBlack.intValue();				//
			while (row >= columns[col].height()) {	//Find piece at index of lastBlack
				row -= columns[col].height();		//
				col++;								//
			}
			BigInteger blacks;
			if (firstBlacks.equals(BigInteger.ONE)) //
				blacks = BigInteger.ZERO;			// The conditions are on opposite sides
			else									// (n choose 0) = (n choose n) equals (one)
				blacks = lastBlack;					//
			set(row, col, blacks, BigInteger.ONE, Color.RED); // Change black to red
			Piece p=get(row,col);
			row++;										// And now we see that
			while (row >= columns[col].height()) {		// this code block is
				col++;									// for moving to
				row = 0;								// the next piece on the board
			}											// In col-major order
			set(row, col, blacks.add(BigInteger.ONE), p.nextBlack(),	//Change red to black
					Color.BLACK);
			if (firstBlacks.equals(BigInteger.ONE)) {				// The actual operations may be
				firstBlacks = BigInteger.ZERO;						// the same, but the new board
				do {												// looks different depending on
					firstBlacks = firstBlacks.add(BigInteger.ONE);	// why you ignored the switch
					firstAll = firstAll.add(BigInteger.ONE);		// Memoization can be a hastle sometimes
					p=get(row,col);
					row++;
					while (col < width && row >= columns[col].height()) {	// The extra condition
						col++;												// eliminates exceptions
						row = 0;											// when finding the last
					}														// possible position: RRRBRBBB
				} while (col < width && get(row, col).getColor() == Color.BLACK);
			} else
				firstAll = firstBlacks = lastBlack;	//That's all??? Yup, it sure is
		} else {	//When the mass switch can't be ignored:
			Piece p = null;
			firstBlacks = firstBlacks.subtract(BigInteger.ONE);
			firstAll = firstBlacks;
			for (count = BigInteger.ZERO; count.compareTo(firstBlacks) < 0; count = count
					.add(BigInteger.ONE)) {						//
				set(row, col, count.add(BigInteger.ONE),		//
						BigInteger.ZERO, Color.BLACK);			//
				p = get(row, col);								//
				row++;											// Put the black ones in front
				while (row >= columns[col].height()) {			//
					col++;										//
					row = 0;									//
				}												//
			}
			for (; count.compareTo(lastBlack) < 0; count = count			//
					.add(BigInteger.ONE)) {									//
				set(row, col, firstBlacks, p.nextRed(), Color.RED);	// Followed by the red ones
				p=get(row,col);												//
				row++;														//
				while (row >= columns[col].height()) {						//
					col++;													//
					row = 0;												//
				}															//
			}																//
			set(row, col, firstBlacks, p.nextRed(), Color.RED);		// Black to red
			p = get(row, col);
			row++;
			while (row >= columns[col].height()) {
				col++;
				row = 0;
			}
			set(row, col, firstBlacks.add(BigInteger.ONE), p.nextBlack(),	//Red to black
					Color.BLACK);
		}
		arHash = arHash.add(BigInteger.ONE);	// The hash equivalent of the last 75 lines of code
	}

	private void set(final int row, final int col, final BigInteger black,
			final BigInteger hashNum, final Color color) {
		columns[col].setPiece(row, black, hashNum, color);
	}

	/**
	 * @return The hash of the current board
	 */
	public BigInteger getHash() {
		return arHash;
	}
	
	/**
	 * @return The respective tiers and hashes of all possible moves from
	 *         this position.  Should I include column number as well?
	 */
	/*
	 * Adding a red piece to the rightmost column never changes the hash.
	 * Adding a black piece only by the piece's own hash contribution.
	 * The following code is surprisingly short thanks to all the information
	 * the columns and pieces maintain about themselves.
	 * Who would have thought you could simultaneously hash all the moves of a
	 * full-size connect four board by adding eight or nine numbers together?
	 */
	public ArrayList<BigInteger> moveHashes() {
		ArrayList<BigInteger> al = new ArrayList<BigInteger>(openColumns);
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
					al.add(newHash.add(contr));
				}
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
	
	/**
	 * @return The tier of this board
	 */
	public int getTier() {
		return tier;
	}
	
	/**
	 * @return the tiers of each of the possible moves on this board
	 */
	public ArrayList<Integer> moveTiers() {
		ArrayList<Integer> al = new ArrayList<Integer>();
		for (int col = width - 1; col >= 0; col--) {
			al.add(tier + columns[col].getTier());
		}
		return al;
	}

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

	/**
	 * @param row The board row
	 * @param col The board column
	 * @return The piece on the board
	 */
	public Piece get(final int row, final int col) {
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
	/*
	 * Look for pieces that might have been the last move made (at the top of
	 * they're column and of the right color.  Then return lose if the piece
	 * is part of a four-in-a-row.  Otherwise return Tie or Undecided depending
	 * on whether the board is full or not.
	 */
	public PrimitiveValue primitiveValue(final int piecesToWin) {
		int colHeight;
		for (int col = 0; col < width; col++) {
			colHeight = columns[col].height();
			if (colHeight > 0
					&& columns[col].topPiece().getColor() == turn.opposite()
					&& checkLastWin(colHeight - 1, col, piecesToWin))
				return PrimitiveValue.Lose;
		}
		if (openColumns>0)
			return PrimitiveValue.Undecided;
		else
			return PrimitiveValue.Tie;
	}

	/*
	 * Looks for a win that uses the given piece.
	 */
	private boolean checkLastWin(final int row, final int col, final int piecesToWin) {
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

	/**
	 * @return Has this board reached its maximum hash?
	 */
	public boolean hasNext() {
		return arHash.compareTo(maxPHash.subtract(BigInteger.ONE)) < 0;
	}

	/**
	 * @param args Empty
	 */
	public static void main(String[] args) {
		int height = 4, width = 4, piecesToWin = 4;
		FileDatabase fd = new FileDatabase("file:///tmp/database.db");
		BigInteger tierOffset = BigInteger.ZERO;
		for (int tier = (int) (Math.pow(height + 1, width) - 1); tier >= 0; tier--) {
			FastBoard fb = new FastBoard(height, width, tier);
			fd.setOffset(fb.getTier(), tierOffset);
			ArrayList<Integer> moveTiers = fb.moveTiers();
			ArrayList<BigInteger> moveOffsets = new ArrayList<BigInteger>(
					moveTiers.size());
			for (int i = 0; i < moveTiers.size(); i++) {
				moveOffsets.add(fd.getOffset(moveTiers.get(i)));
			}
			fb.addHash(/*fd, tierOffset, moveOffsets,*/ piecesToWin);
			System.out.println(fb.getHash());
			System.out.println(fb);
			while (fb.hasNext()) {
				fb.next();
				fb.addHash(fd, tierOffset, moveOffsets, piecesToWin);
				System.out.println(fb.getHash());
				System.out.println(fb);
			}
			tierOffset = tierOffset.add(fb.maxHash());
		}
	}

	private void addHash(FileDatabase fd, BigInteger tierOffset,
			ArrayList<BigInteger> moveOffsets, int piecesToWin) {
		Record r = primitiveValue(piecesToWin);
		if (r == PrimitiveValue.Undecided) {
			Record bestMove = null;
			ArrayList<BigInteger> m = moveHashes();
			for (int i = 0; i < m.size(); i++) {
				r = fd.getRecord(m.get(i).add(moveOffsets.get(i)));
				if (bestMove == null || r.isPreferableTo(bestMove))
					bestMove = r;
			}
			System.out.println(m);
		}else
			System.out.println(pv);
		fd.putRecord(getHash().add(tierOffset), r);
	}
}
