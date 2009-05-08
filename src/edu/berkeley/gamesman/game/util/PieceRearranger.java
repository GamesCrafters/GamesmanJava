package edu.berkeley.gamesman.game.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import edu.berkeley.gamesman.util.Pair;

/**
 * @author DNSpies
 */
public final class PieceRearranger implements Cloneable {
	private static final class HashPiece {
		private final int index;

		private char player;

		private long hash;

		private long nextO;

		private long nextX;

		private final HashGroup group;

		protected HashPiece(int pieces, int os, long hash, char player,
				HashGroup group) {
			this.player = player;
			this.index = pieces;
			this.hash = hash;
			if (hash == 0) {
				nextO = 0;
				nextX = 1;
			} else {
				nextO = hash * (pieces + 1) / (os + 1);
				nextX = hash * (pieces + 1) / (pieces + 1 - os);
			}
			this.group = group;
		}

		protected void set(int os, long hash, char player) {
			long xChange = 0;
			long oChange = 0;
			if (this.player == 'O') {
				xChange = xChange - (nextX - this.hash);
				oChange = oChange - (nextO - this.hash);
			}
			this.player = player;
			this.hash = hash;
			if (hash == (0)) {
				this.nextO = 0;
				this.nextX = 1;
			} else {
				this.nextO = hash * (index + 1) / (os + 1);
				this.nextX = hash * (index + 1) / (index + 1 - os);
			}
			if (player == 'O') {
				xChange = xChange + (nextX - hash);
				oChange = oChange + (nextO - hash);
			}
			group.setPiece(oChange, xChange);
		}

		public String toString() {
			return String.valueOf(player);
		}
	}

	private final class HashGroup {
		private int empty;

		private HashPiece lastPiece;

		private long addO = 0;

		private long addX = 0;

		private HashGroup(int index, HashPiece lastPiece) throws Exception {
			this.empty = index;
			this.lastPiece = lastPiece;
		}

		private void addPiece(HashPiece p) {
			if (p.player == 'O')
				setPiece(p.nextO - p.hash, p.nextX - p.hash);
			lastPiece = p;
			empty++;
		}

		private void setPiece(long addOChange, long addXChange) {
			addO = addO + (addOChange);
			addX = addX + (addXChange);
		}
	}

	/**
	 * The number of possible arrangements of the given number of X's and O's
	 */
	public final long arrangements;

	private final HashGroup[] groups;

	private final int numPieces, numOs;

	private long hash = 0;

	private int openX = 0, openO = 0;

	private boolean hasNext = true;

	private final HashPiece[] pieces;

	private final int numSpaces;

	/**
	 * @param s
	 *            A character representation of the board (in 'X' 'O' and ' ')
	 * @throws Exception
	 *             If the char array contains other characters
	 */
	public PieceRearranger(final char[] s) throws Exception {
		LinkedList<HashGroup> g = new LinkedList<HashGroup>();
		numSpaces = s.length;
		HashPiece lastPiece = new HashPiece(-1, 0, 0, 'O', null);
		HashGroup currentGroup = new HashGroup(0, lastPiece);
		pieces = new HashPiece[numSpaces];
		int numPieces = 0, numOs = 0;
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
					numOs++;
					lastPiece = new HashPiece(numPieces, numOs,
							lastPiece.nextO, 'O', currentGroup);
					hash = hash + lastPiece.hash;
					currentGroup.addPiece(lastPiece);
				} else if (s[i] == 'X') {
					if (onFX)
						openX++;
					else
						onFO = false;
					lastPiece = new HashPiece(numPieces, numOs,
							lastPiece.nextX, 'X', currentGroup);
					currentGroup.addPiece(lastPiece);
				} else
					throw new Exception("Bad String: " + String.valueOf(s));
				pieces[i] = lastPiece;
				numPieces++;
			}
		}
		if (onFO)
			hasNext = false;
		g.add(currentGroup);
		arrangements = lastPiece.nextX;
		groups = g.toArray(new HashGroup[g.size()]);
		this.numPieces = numPieces;
		this.numOs = numOs;
	}

	/**
	 * @param s
	 *            A character outline telling where the pieces and spaces are
	 * @param os
	 *            The number of O's on the board
	 * @param xs
	 *            The number of X's on the board
	 * @throws Exception
	 *             If the number of pieces in the outline is not os + xs
	 */
	public PieceRearranger(final char[] s, int os, int xs)
			throws Exception {
		LinkedList<HashGroup> g = new LinkedList<HashGroup>();
		numSpaces = s.length;
		HashPiece lastPiece = new HashPiece(-1, 0, 0, 'O', null);
		HashGroup currentGroup = new HashGroup(0, lastPiece);
		pieces = new HashPiece[numSpaces];
		int numPieces = 0, numOs = 0;
		for (int i = 0; i < numSpaces; i++) {
			if (s[i] == ' ') {
				g.add(currentGroup);
				currentGroup = new HashGroup(i + 1, lastPiece);
			} else {
				if (numOs < os) {
					numOs++;
					lastPiece = new HashPiece(numPieces, numOs,
							lastPiece.nextO, 'O', currentGroup);
					currentGroup.addPiece(lastPiece);
				} else {
					lastPiece = new HashPiece(numPieces, numOs,
							lastPiece.nextX, 'X', currentGroup);
					currentGroup.addPiece(lastPiece);
				}
				pieces[i] = lastPiece;
				numPieces++;
			}
		}
		g.add(currentGroup);
		arrangements = lastPiece.nextX;
		groups = g.toArray(new HashGroup[g.size()]);
		this.numPieces = numPieces;
		this.numOs = numOs;
		openO = os;
		openX = 0;
		if (os == 0)
			hasNext = false;
	}

	/**
	 * @param s
	 *            A character representation of the board (in 'X' 'O' and ' ')
	 * @throws Exception
	 *             If the char array contains other characters
	 */
	public PieceRearranger(String s) throws Exception {
		this(s.toCharArray());
	}

	/**
	 * 
	 * @param s
	 *            A character outline telling where the pieces and spaces are (' '
	 *            is space, anything else is piece)
	 * @param os
	 *            The number of O's on the board
	 * @param xs
	 *            The number of X's on the board
	 * @throws Exception
	 *             If the number of pieces in the outline is not os + xs
	 */
	public PieceRearranger(String s, int os, int xs) throws Exception {
		this(s.toCharArray(), os, xs);
	}

	/**
	 * @param player
	 *            'X' or 'O', the piece to be added to the board
	 * @return A collection of all the hashes after each possible move is made.
	 */
	public ArrayList<Pair<Integer, Long>> getChildren(char player) {
		ArrayList<Pair<Integer, Long>> result = new ArrayList<Pair<Integer, Long>>(
				groups.length);
		long move = hash;
		if (player == 'O') {
			move = move + groups[groups.length - 1].addO;
			for (int i = groups.length - 2; i >= 0; i--) {
				result.add(new Pair<Integer, Long>(groups[i].empty, move
						+ groups[i].lastPiece.nextO));
				move += groups[i].addO;
			}
		} else if (player == 'X') {
			move += groups[groups.length - 1].addX;
			for (int i = groups.length - 2; i >= 0; i--) {
				result.add(new Pair<Integer, Long>(groups[i].empty, move));
				move += groups[i].addX;
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
				pieces[index].set(i + 1, 0, 'O');
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
		changedPieces.add(new Pair<Integer, Character>(index, 'X'));
		if (lastPiece == null)
			pieces[index].set(0, 1, 'X');
		else
			pieces[index].set(openO - 1, lastPiece.nextX, 'X');
		lastPiece = pieces[index];
		index = nextPiece(index);
		changedPieces.add(new Pair<Integer, Character>(index, 'O'));
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
		hash = hash + (1);
		return changedPieces;
	}

	private int prevPiece(int piece) {
		piece--;
		if (piece < 0)
			return -1;
		while (pieces[piece] == null) {
			piece--;
			if (piece < 0)
				return -1;
		}
		return piece;
	}

	private int nextPiece(int piece) {
		piece++;
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
	 * @param piece
	 *            The index of the piece to return
	 * @return The character of the piece.
	 */
	public char get(int piece) {
		if (pieces[piece] == null)
			return ' ';
		else
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
	public PieceRearranger clone() {
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
	public long getHash() {
		return hash;
	}

	/**
	 * Sets the board to the position represented by the given hash
	 * 
	 * @param hash
	 *            The hash
	 * @return The new layout of the board
	 */
	public String unHash(long hash) {
		this.hash = hash;
		int index = prevPiece(pieces.length);
		openO = 0;
		openX = 0;
		long tryHash = arrangements * (numPieces - numOs);
		int numOs = this.numOs;
		for (int numPieces = this.numPieces; numPieces > 0; numPieces--) {
			tryHash = tryHash / numPieces;
			if (hash >= tryHash) {
				hash = hash - tryHash;
				pieces[index].set(numOs, tryHash, 'O');
				tryHash = tryHash * numOs;
				numOs--;
				if (openX > 0) {
					openO = 1;
					openX = 0;
				} else
					openO++;
			} else {
				pieces[index].set(numOs, tryHash, 'X');
				tryHash = tryHash * (numPieces - numOs - 1);
				openX++;
			}
			index = prevPiece(index);
		}
		return toString();
	}
}
