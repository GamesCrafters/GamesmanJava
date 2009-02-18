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
 *         Unhashing is both unnecessary and virtually impossible in any
 *         reasonable amount of time. Hashing is a strictly one-way function.
 */
/*
 *         Please do not make any changes to this class. I think I've removed all
 *         the bugs, but I'd like to fix it myself if there's an issue.
 *         If you auto-format (ctrl-shift-f in eclipse) you die.  It took me a long
 *         time to comment this and I'm not gonna have some brainless IDE messing it up
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
		private final BigInteger tier;					//When a piece is added to this column, the tier always increases by this amount.
		private int colHeight = 0;
		private BigInteger hash = BigInteger.ZERO;		//The sum of each of the hash contributions of the pieces in the column
		private BigInteger addRed = BigInteger.ZERO;	//The sum of each of the addRed's of the pieces in the columns
		private BigInteger addBlack = BigInteger.ZERO;	//Take a guess

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
			return piece[colHeight - 1];
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
		private final BigInteger hash;		//This piece's contribution to the board hash (if this piece is black, if it's red the contribution is always zero)
		private final BigInteger addRed;	//This piece's contribution to the board hash after a red piece is added somewhere to the left of it (if this piece is black)
		private final BigInteger addBlack;	//This piece's contribution after a black piece is added...
		private final Color color;

		Piece(BigInteger index, BigInteger black, BigInteger hashNum,
				Color color) {
			this.hash = hashNum;
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
	private final BigInteger tier, maxPHash;		// maxPHash = number of positions in this tier
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
	public FastBoard(int height, int width, BigInteger tier) {
		this.height = height;
		this.width = width;
		this.tier = tier;
		columns = new Column[width];
		int[] colHeight = new int[width];	//For temporary use
		int pInt = 0;		//--> Makes things easier and faster to
		int col;			//    use ints instead of BigIntegers
		BigInteger[] divmod;
		int oc = width;		//Open columns, not Orange County
		BigInteger hBig = BigInteger.valueOf(height + 1);
		BigInteger colTier = BigInteger.ONE;
		for (col = 0; col < width; col++) {				//
			columns[col] = new Column(height, colTier);	// a) Determines column heights by unhashing tier number
			divmod = tier.divideAndRemainder(hBig);		//   (If cycling through tiers, this can be done in constant time
			colTier = colTier.multiply(hBig);			//    but since there are so many more hashes than tiers, it hardly
			colHeight[col] = divmod[1].intValue();		//    seems worthwhile)
			if (colHeight[col] == height)				// 
				oc--;									// b) Initializes columns along with their tier values
			pInt += colHeight[col];						//
			tier = divmod[0];							// c) Determines the number of pieces on the board
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
		for (; i.compareTo(pieces) < 0; i = i.add(BigInteger.ONE)) {	// If it's not black, it's red
			while (colHeight[col] == 0)									//
				col++;													// All other colors are illusions
			p = new Piece(i, blackPieces, p.nextRed(), Color.RED);		// created by the media to blind us
			columns[col].addPiece(p);									// from the truth
			colHeight[col]--;											//
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
		while (row >= columns[col].height()) {	// The only place
			col++;								// where this block of code
			row = 0;							// isn't preceded by row++;
		}										// If it doesn't make sense now, it will.
		BigInteger count;
		if (firstAll.compareTo(firstBlacks) == 0			// A little strange that these two seemingly
				|| firstBlacks.equals(BigInteger.ONE)) {	// unrelated conditions have almost the same result:
			int i;											// Ignore the initial mass switch and only do the small switch
			int lbInt = lastBlack.intValue();	// BigIntegers are evil
			for (i = 0; i < lbInt; i++) {
				row++;									// And now we see that
				while (row >= columns[col].height()) {	// this code block is
					col++;								// for moving to
					row = 0;							// the next piece on the board
				}										// In col-major order
			}
			BigInteger blacks;
			if (firstBlacks.equals(BigInteger.ONE)) //
				blacks = BigInteger.ZERO;			// The conditions are on opposite sides
			else									// (n choose 0) = (n choose n) equals (one)
				blacks = lastBlack;					//
			set(row, col, new Piece(lastBlack, blacks, BigInteger.ONE,	// Change black to red
					Color.RED));										//
			row++;										//
			while (row >= columns[col].height()) {		//
				col++;									// Next piece
				row = 0;								//
			}											//
			set(row, col, new Piece(firstAll, blacks.add(BigInteger.ONE),	// Change red to black
					BigInteger.ONE, Color.BLACK));							//
			if (firstBlacks.equals(BigInteger.ONE)) {				// The actual operations may be
				firstBlacks = BigInteger.ZERO;						// the same, but the new board
				do {												// looks different depending on
					firstBlacks = firstBlacks.add(BigInteger.ONE);	// why you ignored the switch
					firstAll = firstAll.add(BigInteger.ONE);		// Memoization can be a hastle sometimes
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
				p = new Piece(count, count.add(BigInteger.ONE),	//
						BigInteger.ZERO, Color.BLACK);			//
				set(row,col,p);									//
				row++;											// Put the black ones in front
				while (row >= columns[col].height()) {			//
					col++;										//
					row = 0;									//
				}												//
			}
			for (; count.compareTo(lastBlack) < 0; count = count			//
					.add(BigInteger.ONE)) {									//
				p = new Piece(count, firstBlacks, p.nextRed(), Color.RED);	//
				set(row,col,p);												// Followed by the red ones
				row++;														//
				while (row >= columns[col].height()) {						//
					col++;													//
					row = 0;												//
				}															//
			}																//
			p = new Piece(lastBlack, firstBlacks, p.nextRed(), Color.RED);		// Black to red
			set(row,col,p);														//
			row++;
			while (row >= columns[col].height()) {
				col++;
				row = 0;
			}
			p = new Piece(lastBlack.add(BigInteger.ONE), firstBlacks
					.add(BigInteger.ONE), p.nextBlack(), Color.BLACK);			// Red to black
			set(row,col,p);														//
		}
		arHash = arHash.add(BigInteger.ONE);	// The hash equivalent of the last 63 lines of code
	}

	private void set(int row, int col, Piece p) {
		columns[col].setPiece(row, p);
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
	/*
	 * Look for pieces that might have been the last move made (at the top of
	 * they're column and of the right color.  Then return lose if the piece
	 * is part of a four-in-a-row.  Otherwise return Tie or Undecided depending
	 * on whether the board is full or not.
	 */
	public PrimitiveValue primitiveValue(int piecesToWin) {
		int colHeight;
		for (int col = 0; col < width; col++) {
			colHeight = columns[col].height();
			if (colHeight > 0
					&& get(colHeight - 1, col).getColor() == turn.opposite()
					&& checkLastWin(colHeight - 1, col, piecesToWin))
				return PrimitiveValue.Lose;
		}
		if (openColumns>0)
			return PrimitiveValue.Undecided;
		else
			return PrimitiveValue.Tie;
	}

	/*
	 * Looks for a win making use of the given piece.
	 */
	private boolean checkLastWin(int row, int col, int piecesToWin) {
		Color turn = get(row, col).getColor();
		int ext;
		int stopPos;

		// Check horizontal win
		ext = 1;
		stopPos = Math.min(col, piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++)
			if (get(row, col - i).getColor() == turn)
				ext++;
			else
				break;
		stopPos = Math.min(width - 1 - col, piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++)
			if (get(row, col + i).getColor() == turn)
				ext++;
			else
				break;
		if (ext >= piecesToWin)
			return true;

		// Check DownLeft/UpRight Win
		ext = 1;
		stopPos = Math.min(Math.min(row, col), piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++)
			if (get(row - i, col - i).getColor() == turn)
				ext++;
			else
				break;
		stopPos = Math.min(Math.min(height - 1 - row, width - 1 - col), piecesToWin
				- ext);
		for (int i = 1; i <= stopPos; i++)
			if (get(row + i, col + i).getColor() == turn)
				ext++;
			else
				break;
		if (ext >= piecesToWin)
			return true;

		// Check UpLeft/DownRight Win
		ext = 1;
		stopPos = Math.min(Math.min(height - 1 - row, col), piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++)
			if (get(row + i, col - i).getColor() == turn)
				ext++;
			else
				break;
		stopPos = Math.min(Math.min(row, width - 1 - col), piecesToWin - ext);
		for (int i = 1; i <= stopPos; i++)
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
		return arHash.compareTo(maxPHash.subtract(BigInteger.ONE)) < 0;
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
