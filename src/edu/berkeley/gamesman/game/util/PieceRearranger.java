package edu.berkeley.gamesman.game.util;

/**
 * @author DNSpies
 */
public final class PieceRearranger implements Cloneable {
	/**
	 * Iterates over all the pieces whose colors flipped
	 * 
	 * @author dnspies
	 */
	public static final class ChangedPieces {
		private int firstEnd, secondEnd, thirdEnd;

		private int next;

		/**
		 * @param firstSwitchedX
		 *            The number of x's that moved to the right
		 * @param firstSwitchedO
		 *            The number of o's that moved to the left
		 */
		private void reset(int firstSwitchedX, int firstSwitchedO) {
			next = 0;
			if (firstSwitchedX < firstSwitchedO) {
				firstEnd = firstSwitchedX;
				secondEnd = firstSwitchedO;
			} else {
				firstEnd = firstSwitchedO;
				secondEnd = firstSwitchedX;
			}
			thirdEnd = firstSwitchedX + firstSwitchedO + 2;
			if (firstEnd == 0)
				next = secondEnd;
		}

		/**
		 * @return Was another piece flipped?
		 */
		public boolean hasNext() {
			return next < thirdEnd;
		}

		/**
		 * @return The index of the next flipped piece
		 */
		public int next() {
			int result = next;
			next++;
			if (next >= firstEnd && next < secondEnd)
				next = secondEnd;
			return result;
		}
	}

	private static final class HashPiece {
		private final int index;

		private char player;

		private long hash;

		private long nextO;

		private long nextX;

		private HashGroup group;

		private HashPiece(int pieces, int os, long hash, char player,
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

		private void set(int os, long hash, char player) {
			long xChange = 0;
			long oChange = 0;
			if (this.player == 'O') {
				xChange -= nextX - this.hash;
				oChange -= nextO - this.hash;
			}
			this.player = player;
			this.hash = hash;
			if (hash == 0) {
				nextO = 0;
				nextX = 1;
			} else {
				nextO = hash * (index + 1) / (os + 1);
				nextX = hash * (index + 1) / (index + 1 - os);
			}
			if (player == 'O') {
				xChange += nextX - hash;
				oChange += nextO - hash;
			}
			group.setPiece(oChange, xChange);
		}

		public String toString() {
			return String.valueOf(player);
		}
	}

	private static final class HashGroup {
		private HashPiece lastPiece;

		private long addO = 0;

		private long addX = 0;

		private HashGroup(HashPiece lastPiece) {
			this.lastPiece = lastPiece;
		}

		private void addPiece(HashPiece p) {
			if (p.player == 'O')
				setPiece(p.nextO - p.hash, p.nextX - p.hash);
			lastPiece = p;
		}

		private void setPiece(long addOChange, long addXChange) {
			addO += (addOChange);
			addX += (addXChange);
		}

		public void reset(HashPiece lastPiece) {
			this.lastPiece = lastPiece;
			addO = 0;
			addX = 0;
		}
	}

	/**
	 * The number of possible arrangements of the given number of X's and O's
	 */
	public final long colorArrangements;

	private final HashGroup[] groups;

	private int numGroups;

	private final int numOs;

	private long hash = 0;

	private int openX = 0, openO = 0;

	private boolean hasNext;

	private HashPiece[] pieces;

	private int piecesLength;

	private final HashPiece lowPiece = new HashPiece(-1, 0, 0, 'O', null);

	private final ChangedPieces cp = new ChangedPieces();

	/**
	 * @param s
	 *            A character representation of the board (in 'X' 'O' and ' ')
	 */
	public PieceRearranger(final char[] s) {
		groups = new HashGroup[s.length + 1];
		numGroups = 0;
		HashPiece lastPiece = lowPiece;
		HashGroup currentGroup = new HashGroup(lastPiece);
		pieces = new HashPiece[s.length];
		piecesLength = 0;
		int numOs = 0;
		boolean onFX = true, onFO = true;
		for (int i = 0; i < s.length; i++) {
			if (s[i] == ' ') {
				groups[numGroups++] = currentGroup;
				currentGroup = new HashGroup(lastPiece);
			} else {
				if (s[i] == 'O') {
					if (onFO) {
						onFX = false;
						openO++;
					}
					numOs++;
					lastPiece = new HashPiece(piecesLength, numOs,
							lastPiece.nextO, 'O', currentGroup);
					hash += lastPiece.hash;
					currentGroup.addPiece(lastPiece);
				} else if (s[i] == 'X') {
					if (onFX)
						openX++;
					else
						onFO = false;
					lastPiece = new HashPiece(piecesLength, numOs,
							lastPiece.nextX, 'X', currentGroup);
					currentGroup.addPiece(lastPiece);
				} else
					new Exception("Bad String: " + String.valueOf(s))
							.printStackTrace();
				pieces[piecesLength++] = lastPiece;
			}
		}
		hasNext = !onFO;
		groups[numGroups++] = currentGroup;
		colorArrangements = lastPiece.nextX;
		this.numOs = numOs;
	}

	/**
	 * @param s
	 *            A character outline telling where the pieces and spaces are
	 * @param os
	 *            The number of O's on the board
	 * @param xs
	 *            The number of X's on the board
	 */
	public PieceRearranger(final char[] s, int os, int xs) {
		groups = new HashGroup[s.length + 1];
		numGroups = 0;
		HashPiece lastPiece = lowPiece;
		HashGroup currentGroup = new HashGroup(lastPiece);
		pieces = new HashPiece[s.length];
		int numOs = 0;
		for (int i = 0; i < s.length; i++) {
			if (s[i] == ' ') {
				groups[numGroups++] = currentGroup;
				currentGroup = new HashGroup(lastPiece);
			} else {
				if (numOs < os) {
					numOs++;
					lastPiece = new HashPiece(piecesLength, numOs,
							lastPiece.nextO, 'O', currentGroup);
					currentGroup.addPiece(lastPiece);
				} else {
					lastPiece = new HashPiece(piecesLength, numOs,
							lastPiece.nextX, 'X', currentGroup);
					currentGroup.addPiece(lastPiece);
				}
				pieces[piecesLength++] = lastPiece;
			}
		}
		groups[numGroups++] = currentGroup;
		colorArrangements = lastPiece.nextX;
		this.numOs = numOs;
		openO = os;
		openX = 0;
		hasNext = xs > 0 && os > 0;
	}

	/**
	 * Puts all the Os before all the Xs
	 */
	public void reset() {
		int i;
		for (i = 0; i < numOs; i++)
			pieces[i].set(i, 0, 'O');
		HashPiece lastPiece;
		if (numOs > 0)
			lastPiece = pieces[numOs - 1];
		else
			lastPiece = lowPiece;
		for (; i < piecesLength; i++) {
			pieces[i].set(numOs, lastPiece.nextX, 'X');
			lastPiece = pieces[i];
		}
		openO = numOs;
		openX = 0;
		hasNext = piecesLength > numOs && numOs > 0;
		hash = 0;
	}

	/**
	 * Sets the groups to the provided sizes
	 * 
	 * @param groupSizes
	 *            The size of each group
	 * @param numMoves
	 *            The number of groups
	 */
	public void setGroupSizes(int[] groupSizes, int numMoves) {
		this.numGroups = numMoves + 1;
		int k = 0;
		int totSize = 0;
		HashPiece lastPiece = lowPiece;
		for (int i = 0; i < numMoves; i++) {
			if (groups[i] == null)
				groups[i] = new HashGroup(lastPiece);
			else
				groups[i].reset(lastPiece);
			for (totSize += groupSizes[i]; k < totSize; k++) {
				lastPiece = pieces[k];
				lastPiece.group = groups[i];
				groups[i].addPiece(lastPiece);
			}
		}
		if (groups[numMoves] == null)
			groups[numMoves] = new HashGroup(lastPiece);
		else
			groups[numMoves].reset(lastPiece);
		for (; k < piecesLength; k++) {
			lastPiece = pieces[k];
			lastPiece.group = groups[numMoves];
			groups[numMoves].addPiece(lastPiece);
		}
	}

	/**
	 * @param s
	 *            A character representation of the board (in 'X' 'O' and ' ')
	 */
	public PieceRearranger(String s) {
		this(s.toCharArray());
	}

	/**
	 * @param s
	 *            A character outline telling where the pieces and spaces are
	 *            (' ' is space, anything else is piece)
	 * @param os
	 *            The number of O's on the board
	 * @param xs
	 *            The number of X's on the board
	 */
	public PieceRearranger(String s, int os, int xs) {
		this(s.toCharArray(), os, xs);
	}

	/**
	 * @param player
	 *            'X' or 'O', the piece to be added to the board
	 * @param children
	 *            The children array to put the values in
	 * @return A collection of all the hashes after each possible move is made.
	 */
	public int getChildren(final char player, long[] children) {
		long move = hash;
		if (player == 'O') {
			HashGroup g;
			move += groups[numGroups - 1].addO;
			for (int i = numGroups - 2; i >= 0; i--) {
				g = groups[i];
				children[i] = move + g.lastPiece.nextO;
				move += g.addO;
			}
		} else if (player == 'X') {
			move += groups[numGroups - 1].addX;
			for (int i = numGroups - 2; i >= 0; i--) {
				children[i] = move;
				move += groups[i].addX;
			}
		}
		return numGroups - 1;
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
	 * win-checking (if possible). Since PieceRearranger retains a reference to
	 * the returned ChangePieces in order to conserve space. It's important the
	 * calling class finish using changePieces before the next call to next() or
	 * else it risks nondeterministic behavior.
	 * 
	 * @return An iterator over the pieces that changed
	 */
	public ChangedPieces next() {
		int newOpenO = openO - 1;
		int totalOpen = openX + newOpenO;
		cp.reset(openX, newOpenO);
		int i;
		if (openX > 0 && newOpenO > 0) {
			for (i = 0; i < newOpenO; i++)
				pieces[i].set(i + 1, 0, 'O');
			for (i = newOpenO; i < totalOpen; i++)
				pieces[i].set(newOpenO, pieces[i - 1].nextX, 'X');
		}
		long firstXHash = (totalOpen == 0 ? 1 : pieces[totalOpen - 1].nextX);
		pieces[totalOpen].set(newOpenO, firstXHash, 'X');
		pieces[totalOpen + 1].set(newOpenO + 1, pieces[totalOpen].nextO, 'O');
		if (newOpenO > 0) {
			openX = 0;
			openO = newOpenO;
		} else {
			openX++;
			i = totalOpen + 1;
			for (i = totalOpen + 2; i < piecesLength; i++) {
				if (pieces[i].player == 'O')
					openO++;
				else
					break;
			}
			if (i == piecesLength)
				hasNext = false;
		}
		hash++;
		return cp;
	}

	/**
	 * @param piece
	 *            The index of the piece to return
	 * @return The character of the piece.
	 */
	public char get(final int piece) {
		return pieces[piece].player;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(piecesLength + numGroups);
		int i = 0;
		HashGroup g = groups[i++];
		while (g.lastPiece == lowPiece) {
			str.append(' ');
			g = groups[i++];
		}
		for (HashPiece piece : pieces) {
			str.append(piece.player);
			while (g.lastPiece == piece) {
				str.append(' ');
				g = groups[i++];
			}
		}
		return str.substring(0, str.length() - 1);
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
	 *            The hash to set from
	 */
	public void setFromHash(long hash) {
		this.hash = hash;
		openO = 0;
		openX = 0;
		long tryHash;
		if (piecesLength > 0)
			tryHash = colorArrangements * (piecesLength - numOs) / piecesLength;
		else
			tryHash = 0;
		int oCount = numOs;
		hasNext = false;
		for (int i = piecesLength - 1; i >= 0; i--) {
			if (hash >= tryHash) {
				hash -= tryHash;
				pieces[i].set(oCount, tryHash, 'O');
				if (i > 0)
					tryHash = tryHash * oCount / i;
				oCount--;
				if (openX > 0) {
					openO = 1;
					openX = 0;
					hasNext = true;
				} else
					openO++;
			} else {
				pieces[i].set(oCount, tryHash, 'X');
				if (i > 0)
					tryHash = tryHash * (i - oCount) / i;
				openX++;
			}
		}
	}
}
