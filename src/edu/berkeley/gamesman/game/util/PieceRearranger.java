package edu.berkeley.gamesman.game.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import edu.berkeley.gamesman.util.Pair;


/**
 * <p>
 * Given a string of X's, O's, and spaces, iterates over all rearrangements of
 * X's and O's and finds all hash values when X's/O's are inserted in the
 * spaces.
 * </p>
 * <p>
 * This class is most ideally suited for tiered games such as Tic Tac Toe or
 * hex, in which there are a set number of spaces; all of which are available
 * until a player plays in them. For instance, a tic tac toe board might be
 * passed as such: ("T   T   T",2,1). The board will be initialized to:<br />
 * <code>
 * |O--|<br />
 * |-O-|<br />
 * |--X|<br />
 * </code> and iterate over<br />
 * <code>
 * |O--|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|X--|<br />
 * |-X-| and |-O-|<br />
 * |--O|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|--O|<br />
 * </code>
 * </p>
 * <p>
 * In each scenario, getChildren('X') will return 6 pairs; one for each of x's 6
 * possible moves at positions 2, 3, 4, 6, 7, and 8
 * </p>
 * <p>
 * While this is not ideally suited to Connect Four, it's not too difficult to
 * implement. Reduce all space above an unfilled column to just one and then
 * implement the necessary math in the caller to determine which position is
 * which
 * </p>
 * 
 * @author DNSpies
 */
public final class PieceRearranger implements Cloneable{
	private static final class HashPiece {
		private final int index;
		char player;
		BigInteger hash;
		BigInteger nextO;
		BigInteger nextX;
		private final HashGroup group;

		protected HashPiece(int pieces, int os, BigInteger hash, char player,
				HashGroup group) {
			this.player = player;
			this.index = pieces;
			this.hash = hash;
			if (hash.equals(BigInteger.ZERO)) {
				nextO = BigInteger.ZERO;
				nextX = BigInteger.ONE;
			} else {
				nextO = hash.multiply(BigInteger.valueOf(pieces + 1)).divide(
						BigInteger.valueOf(os + 1));
				nextX = hash.multiply(BigInteger.valueOf(pieces + 1)).divide(
						BigInteger.valueOf(pieces + 1 - os));
			}
			this.group = group;
		}

		protected void set(int os, BigInteger hash, char player) {
			BigInteger xChange = BigInteger.ZERO;
			BigInteger oChange = BigInteger.ZERO;
			if (this.player == 'O') {
				xChange = xChange.subtract(nextX.subtract(this.hash));
				oChange = oChange.subtract(nextO.subtract(this.hash));
			}
			this.player = player;
			this.hash = hash;
			if (hash.equals(BigInteger.ZERO)) {
				this.nextO = BigInteger.ZERO;
				this.nextX = BigInteger.ONE;
			} else {
				this.nextO = hash.multiply(BigInteger.valueOf(index + 1))
						.divide(BigInteger.valueOf(os + 1));
				this.nextX = hash.multiply(BigInteger.valueOf(index + 1))
						.divide(BigInteger.valueOf(index + 1 - os));
			}
			if (player == 'O') {
				xChange = xChange.add(nextX.subtract(hash));
				oChange = oChange.add(nextO.subtract(hash));
			}
			group.setPiece(oChange, xChange);
		}

		public String toString() {
			return String.valueOf(player);
		}
	}

	private final class HashGroup {
		int empty;
		HashPiece lastPiece;
		BigInteger addO = BigInteger.ZERO;
		BigInteger addX = BigInteger.ZERO;

		HashGroup(int index, HashPiece lastPiece) throws Exception {
			this.empty = index;
			this.lastPiece = lastPiece;
		}
		
		void addPiece(HashPiece p) {
			if(p.player == 'O')
				setPiece(p.nextO.subtract(p.hash), p.nextX.subtract(p.hash));
			lastPiece = p;
			empty++;
		}

		void setPiece(BigInteger addOChange, BigInteger addXChange) {
			addO = addO.add(addOChange);
			addX = addX.add(addXChange);
		}
	}

	/**
	 * The number of possible arrangements of the given number of X's and O's
	 */
	public final BigInteger arrangements;
	private final HashGroup[] groups;
	private final int numPieces, numOs;
	private BigInteger hash = BigInteger.ZERO;
	private int openX = 0, openO = 0;
	private boolean hasNext = true;
	private final HashPiece[] pieces;
	private final int numSpaces;

	/**
	 * @param s A character representation of the board (in 'X' 'O' and ' ')
	 * @throws Exception If the char array contains other characters
	 */
	public PieceRearranger(final char[] s) throws Exception {
		LinkedList<HashGroup> g = new LinkedList<HashGroup>();
		numSpaces = s.length;
		HashPiece lastPiece = new HashPiece(-1, 0, BigInteger.ZERO, 'O', null);
		HashGroup currentGroup = new HashGroup(0, lastPiece);
		pieces = new HashPiece[numSpaces];
		int numPieces1 = 0, numOs1 = 0;
		boolean onFX = true, onFO = true;
		for (int i = 0; i < numSpaces; i++) {
			if (s[i] == ' ') {
				g.add(currentGroup);
				currentGroup = new HashGroup(i + 1, lastPiece);
			} else {
				if (s[i] == 'O') {
					if (onFO) {
						onFX = false;
						openO++;
					}
					numOs1++;
					lastPiece = new HashPiece(numPieces1, numOs1,
							lastPiece.nextO, 'O', currentGroup);
					hash = hash.add(lastPiece.hash);
					currentGroup.addPiece(lastPiece);
				} else if (s[i] == 'X') {
					if (onFX)
						openX++;
					else
						onFO = false;
					lastPiece = new HashPiece(numPieces1, numOs1,
							lastPiece.nextX, 'X', currentGroup);
					currentGroup.addPiece(lastPiece);
				} else
					throw new Exception("Bad String: " + String.valueOf(s));
				pieces[i] = lastPiece;
				numPieces1++;
			}
		}
		if(onFO)
			hasNext = false;
		g.add(currentGroup);
		arrangements = lastPiece.nextX;
		groups = g.toArray(new HashGroup[g.size()]);
		this.numPieces = numPieces1;
		this.numOs = numOs1;
	}

	/**
	 * @param s A character outline telling where the pieces and spaces are
	 * @param os The number of O's on the board
	 * @param xs The number of X's on the board
	 * @throws Exception If the number of pieces in the outline is not os + xs
	 */
	public PieceRearranger(final char[] s, int os, int xs) throws Exception {
		LinkedList<HashGroup> g = new LinkedList<HashGroup>();
		numSpaces = s.length;
		HashPiece lastPiece = new HashPiece(-1, 0, BigInteger.ZERO, 'O', null);
		HashGroup currentGroup = new HashGroup(0, lastPiece);
		pieces = new HashPiece[numSpaces];
		int numPieces1 = 0, numOs1 = 0;
		for (int i = 0; i < numSpaces; i++) {
			if (s[i] == ' ') {
				g.add(currentGroup);
				currentGroup = new HashGroup(i + 1, lastPiece);
			} else {
				if (numOs1 < os){
					numOs1++;
					lastPiece = new HashPiece(numPieces1, numOs1, lastPiece.nextO, 'O', currentGroup);
					currentGroup.addPiece(lastPiece);
				}else{
					lastPiece = new HashPiece(numPieces1, numOs1, lastPiece.nextX, 'X', currentGroup);
					currentGroup.addPiece(lastPiece);
				}
				pieces[i] = lastPiece;
				numPieces1++;
			}
		}
		g.add(currentGroup);
		arrangements = lastPiece.nextX;
		groups = g.toArray(new HashGroup[g.size()]);
		this.numPieces = numPieces1;
		this.numOs = numOs1;
		openO = os;
		openX = 0;
		if(os==0)
			hasNext=false;
	}

	/**
	 * @param s A character representation of the board (in 'X' 'O' and ' ')
	 * @throws Exception If the char array contains other characters
	 */
	public PieceRearranger(String s) throws Exception {
		this(s.toCharArray());
	}

	/**
	 * 
	 * @param s A character outline telling where the pieces and spaces are (' ' is space, anything else is piece)
	 * @param os The number of O's on the board
	 * @param xs The number of X's on the board
	 * @throws Exception If the number of pieces in the outline is not os + xs
	 */
	public PieceRearranger(String s, int os, int xs) throws Exception {
		this(s.toCharArray(),os,xs);
	}

	/**
	 * @param player 'X' or 'O', the piece to be added to the board
	 * @return A collection of all the hashes after each possible move is made.
	 */
	public ArrayList<Pair<Integer, BigInteger>> getChildren(char player) {
		ArrayList<Pair<Integer, BigInteger>> result = new ArrayList<Pair<Integer, BigInteger>>(
				groups.length);
		BigInteger move = hash;
		if (player == 'O'){
			move = move.add(groups[groups.length - 1].addO);
			for (int i = groups.length - 2; i >= 0; i--) {
				result.add(new Pair<Integer, BigInteger>(groups[i].empty, move
						.add(groups[i].lastPiece.nextO)));
				move = move.add(groups[i].addO);
			}
		}else if (player == 'X'){
			move = move.add(groups[groups.length - 1].addX);
			for (int i = groups.length - 2; i >= 0; i--) {
				result.add(new Pair<Integer, BigInteger>(groups[i].empty, move));
				move = move.add(groups[i].addX);
			}
		}
		return result;
	}

	/**
	 * @return Whether these characters have another arrangement.
	 */
	public boolean hasNext() {
		return hasNext;
	}

	/**
	 * Each time next() is called, the pieces assume their new positions in the
	 * next hash and a list of all the pieces that were changed is returned.
	 * It's expected that the calling program will use this list to speed up
	 * win-checking (if possible).
	 * 
	 * @return The indices of all the pieces that were changed paired with the
	 *         characters they were changed to
	 */
	public Collection<Pair<Integer, Character>> next() {
		ArrayList<Pair<Integer, Character>> changedPieces = new ArrayList<Pair<Integer, Character>>(
				2 * Math.min(openO - 1, openX) + 2);
		int index = -1;
		index = nextPiece(index);
		int i;
		HashPiece lastPiece;
		if (openX > 0 && openO > 1) {
			lastPiece = null;
			for (i = 0; i < openO - 1; i++) {
				if (get(index) == 'X')
					changedPieces.add(new Pair<Integer, Character>(index, 'O'));
				pieces[index].set(i + 1, BigInteger.ZERO, 'O');
				lastPiece = pieces[index];
				index = nextPiece(index);
			}
			for (i = 0; i < openX; i++) {
				if (get(index) == 'O')
					changedPieces.add(new Pair<Integer, Character>(index, 'X'));
				pieces[index].set(openO - 1, lastPiece.nextX, 'X');
				lastPiece = pieces[index];
				index = nextPiece(index);
			}
		} else if (openX == 0 && openO == 1) {
			lastPiece = null;
		} else {
			for (i = 0; i < openX + openO - 2; i++)
				index = nextPiece(index);
			lastPiece = pieces[index];
			index = nextPiece(index);
		}
		changedPieces.add(new Pair<Integer,Character>(index, 'X'));
		if (lastPiece == null)
			pieces[index].set(0, BigInteger.ONE, 'X');
		else
			pieces[index].set(openO - 1, lastPiece.nextX, 'X');
		lastPiece = pieces[index];
		index = nextPiece(index);
		changedPieces.add(new Pair<Integer,Character>(index, 'O'));
		pieces[index].set(openO, lastPiece.nextO, 'O');
		if (openO > 1) {
			openX = 0;
			openO--;
		} else {
			openX++;
			index = nextPiece(index);
			if (index == -1)
				hasNext = false;
			else
				while (get(index) == 'O') {
					openO++;
					index = nextPiece(index);
					if (index == -1) {
						hasNext = false;
						break;
					}
				}
		}
		hash = hash.add(BigInteger.ONE);
		return changedPieces;
	}

	private int prevPiece(final int inpiece) {
		int piece = inpiece-1;
		if (piece < 0)
			return -1;
		while (pieces[piece] == null) {
			piece--;
			if (piece < 0)
				return -1;
		}
		return piece;
	}
	
	private int nextPiece(final int inpiece) {
		int piece = inpiece+1;
		if (piece >= pieces.length)
			return -1;
		while (pieces[piece] == null) {
			piece++;
			if (piece >= pieces.length)
				return -1;
		}
		return piece;
	}

	/**
	 * @param piece The index of the piece to return
	 * @return The character of the piece.
	 */
	public char get(int piece) {
		if(pieces[piece]==null)
			return ' ';
		return pieces[piece].player;
	}

	@Override
	public String toString() {
		char[] p = new char[pieces.length];
		for (int i = 0; i < pieces.length; i++) {
			if (pieces[i] == null)
				p[i] = ' ';
			else
				p[i] = pieces[i].player;
		}
		return String.valueOf(p);
	}
	
	@Override
	public PieceRearranger clone(){
		try {
			return new PieceRearranger(toString().toCharArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return The current hash value
	 */
	public BigInteger getHash() {
		return hash;
	}
	
	/**
	 * Sets the board to the position represented by the given hash
	 * @param inhash The hash
	 * @return The new layout of the board
	 */
	public String unHash(BigInteger inhash) {
		hash = inhash;
		int index = prevPiece(pieces.length);
		openO = 0;
		openX = 0;
		BigInteger tryHash = arrangements.multiply(BigInteger.valueOf(numPieces
				- numOs));
		int numOs1 = this.numOs;
		for (int numPieces1 = this.numPieces; numPieces1 > 0; numPieces1--) {
			tryHash = tryHash.divide(BigInteger.valueOf(numPieces1));
			if (hash.compareTo(tryHash) >= 0) {
				hash = hash.subtract(tryHash);
				pieces[index].set(numOs1, tryHash, 'O');
				tryHash = tryHash.multiply(BigInteger.valueOf(numOs1));
				numOs1--;
				if(openX>0){
					openO = 1;
					openX = 0;
				}else
					openO++;
			} else {
				pieces[index].set(numOs1, tryHash, 'X');
				tryHash = tryHash.multiply(BigInteger
						.valueOf(numPieces1 - numOs1 - 1));
				openX++;
			}
			index = prevPiece(index);
		}
		return toString();
	}
}
