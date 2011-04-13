package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public final class QuartoMinorHasher {
	private static long pick(int n, int k) {
		if (k <= 0)
			return 1L;
		else
			return n * pick(n - 1, k - 1);
	}

	private static final class Position {
		private final Position[] inner;
		private final long offset;
		private final long numHashes;

		Position(QuickLinkedList<Piece> unused, boolean[] fixedWall,
				long offset, int remainingPieces) {
			this.offset = offset;
			if (remainingPieces <= 0
					|| (fixedWall[0] && fixedWall[1] && fixedWall[2])) {
				inner = null;
				numHashes = pick(unused.size(), remainingPieces);
			} else {
				inner = new Position[16];
				QuickLinkedList<Piece>.QLLIterator pieceIter = unused
						.iterator();
				long numHashes = 0L;
				while (pieceIter.hasNext()) {
					Piece p = pieceIter.next();
					if (isLowest(p, fixedWall)) {
						boolean[] newFixedWall = fixedWall.clone();
						for (int i = 0; i < 3; i++) {
							if (p.get(i) > p.get(i + 1))
								newFixedWall[i] = true;
						}
						int pNum = p.pieceNum;
						pieceIter.remove();
						inner[pNum] = new Position(unused, newFixedWall,
								offset, remainingPieces - 1);
						offset += inner[pNum].numHashes;
						numHashes += inner[pNum].numHashes;
						pieceIter.add(p);
					}
				}
				this.numHashes = numHashes;
			}
		}

		private boolean isLowest(Piece p, boolean[] fixedWall) {
			for (int i = 0; i < 3; i++) {
				if (p.get(i) < p.get(i + 1) && !fixedWall[i])
					return false;
			}
			return true;
		}
	}

	private static final class Rotation implements Cloneable {
		private final int places[];
		private final boolean[] fixedWall;

		private Rotation() {
			places = new int[] { 0, 1, 2, 3 };
			fixedWall = new boolean[3];
		}

		private Rotation(Rotation r) {
			places = r.places.clone();
			fixedWall = r.fixedWall.clone();
		}

		private void dropState(Piece p) {
			for (int i = 0; i < 3; i++) {
				if (!fixedWall[i]) {
					if (p.get(i) < p.get(i + 1)) {
						makeSwitch(p, i, i + 1);
						if (i > 0)
							i -= 2;
					}
				}
			}
			for (int i = 0; i < 3; i++) {
				fixedWall[i] |= p.get(i) > p.get(i + 1);
			}
		}

		private void makeSwitch(Piece p, int i1, int i2) {
			int n1 = p.get(i1), n2 = p.get(i2);
			p.set(i1, n2);
			p.set(i2, n1);
			int t = places[i1];
			places[i1] = places[i2];
			places[i2] = t;
		}

		@Override
		public Rotation clone() {
			return new Rotation(this);
		}

		@Override
		public String toString() {
			return Arrays.toString(places);
		}

		public void reset() {
			for (int i = 0; i < 4; i++) {
				places[i] = i;
			}
			Arrays.fill(fixedWall, false);
		}
	}

	private final class Piece {
		int pieceNum;
		Position myPos;

		public Piece(int i) {
			pieceNum = i;
		}

		public Piece() {
			this(0);
		}

		private void applyRotation(Rotation r) {
			int newNum = 0;
			for (int i = 0; i < 4; i++) {
				newNum = QuartoMinorHasher.set(newNum, i, get(r.places[i]));
			}
			pieceNum = newNum;
		}

		private int get(int i) {
			return QuartoMinorHasher.get(pieceNum, i);
		}

		private void set(int i, int n) {
			pieceNum = QuartoMinorHasher.set(pieceNum, i, n);
		}

		@Override
		public String toString() {
			return Integer.toString(pieceNum);
		}
	}

	private static final class Count {
		int usedPieces = 0;
		long possibilities;
		int lastPiece;
		final boolean[] used = new boolean[16];

		public Count() {
		}

		public Count(int numPieces) {
			reset(numPieces);
		}

		public void reset(int numPieces) {
			Arrays.fill(used, false);
			usedPieces = 0;
			possibilities = pick(16, numPieces);
		}

		public void addPiece(int n) {
			possibilities /= (16 - usedPieces);
			used[n] = true;
			lastPiece = n;
			usedPieces++;
		}

		public long addNext(long hash) {
			possibilities /= 16 - usedPieces;
			lastPiece = (int) (hash / possibilities);
			long pieceHash = lastPiece * possibilities;
			for (int i = 0; i <= lastPiece; i++) {
				if (used[i])
					lastPiece++;
			}
			used[lastPiece] = true;
			usedPieces++;
			return pieceHash;
		}

		public long lastHash() {
			int pCount = lastPiece;
			for (int i = 0; i < lastPiece; i++) {
				if (used[i])
					pCount--;
			}
			return pCount * possibilities;
		}

		// public void removePiece(int n) {
		// usedPieces--;
		// lastPiece = -1;
		// used[n] = false;
		// possibilities *= (16 - usedPieces);
		// }
	}

	private static int set(int num, int i, int n) {
		if (get(num, i) != n)
			return num ^ (1 << i);
		else
			return num;
	}

	private static int get(int num, int i) {
		return (num >> i) & 1;
	}

	private final Position[] tierTables = new Position[17];
	private final Piece[] pieces;
	private final Piece[] child;
	private long hash;
	private final Count numType;
	private final Count count;
	private final Rotation rot;
	private int numPieces;

	public QuartoMinorHasher() {
		QuickLinkedList<Piece> unused = new QuickLinkedList<Piece>();
		for (int i = 1; i < 16; i++) {
			unused.add(new Piece(i));
		}
		for (int i = 0; i <= 16; i++) {
			tierTables[i] = new Position(unused, new boolean[] { false, false,
					false }, 0L, i - 1);
		}
		pieces = new Piece[16];
		child = new Piece[16];
		for (int i = 0; i < 16; i++) {
			pieces[i] = new Piece();
			child[i] = new Piece();
		}
		numPieces = 0;
		numType = new Count();
		count = new Count();
		rot = new Rotation();
	}

	public void setTier(int tier) {
		numPieces = tier;
		numType.reset(tier);
		Position p = tierTables[tier];
		for (int i = 0; i < tier; i++) {
			pieces[i].pieceNum = i;
			pieces[i].myPos = p;
			if (p != null) {
				if (p.inner == null)
					p = null;
				else {
					p = p.inner[i + 1];
					assert p != null;
				}
			}
			numType.addPiece(i);
		}
		hash = 0L;
	}

	public long numHashesForTier(int tier) {
		return tierTables[tier].numHashes;
	}

	public long numHashesForTier() {
		return numHashesForTier(numPieces);
	}

	public long getHash() {
		return hash;
	}

	public long hash(int[] board) {
		for (int i = 0; i < numPieces; i++) {
			pieces[i].pieceNum = board[i] ^ board[0];
		}
		numType.reset(numPieces);
		rot.reset();
		Position p = tierTables[numPieces];
		if (numPieces > 0) {
			numType.addPiece(0);
			pieces[0].myPos = p;
		}
		int i;
		for (i = 1; i < numPieces; i++) {
			if (p.inner == null)
				break;
			pieces[i].applyRotation(rot);
			rot.dropState(pieces[i]);
			numType.addPiece(pieces[i].pieceNum);
			p = p.inner[pieces[i].pieceNum];
			pieces[i].myPos = p;
			if (p == null)
				throw new NullPointerException();
		}
		hash = p.offset;
		for (; i < numPieces; i++) {
			pieces[i].applyRotation(rot);
			pieces[i].myPos = null;
			numType.addPiece(pieces[i].pieceNum);
			long pieceHash = numType.lastHash();
			hash += pieceHash;
		}
		return hash;
	}

	public void setTierAndHash(int tier, int[] board) {
		numPieces = tier;
		hash(board);
	}

	public void unhash(long hash) {
		Position p = tierTables[numPieces];
		numType.reset(numPieces);
		this.hash = hash;
		if (numPieces > 0) {
			pieces[0].pieceNum = 0;
			numType.addPiece(0);
			pieces[0].myPos = p;
		}
		int i;
		for (i = 1; i < numPieces; i++) {
			if (p.inner == null)
				break;
			Position nextP = null;
			int nextK = -1;
			for (int k = 0; k < 16; k++) {
				if (p.inner[k] != null) {
					if (p.inner[k].offset <= hash) {
						nextP = p.inner[k];
						nextK = k;
					} else
						break;
				}
			}
			assert nextP != null;
			pieces[i].pieceNum = nextK;
			numType.addPiece(nextK);
			p = nextP;
			pieces[i].myPos = p;
		}
		hash -= p.offset;
		for (; i < numPieces; i++) {
			long pieceHash = numType.addNext(hash);
			int piece = numType.lastPiece;
			pieces[i].pieceNum = piece;
			pieces[i].myPos = null;
			hash -= pieceHash;
		}
		assert hash == 0L;
	}

	public int get(int index) {
		return pieces[index].pieceNum;
	}

	public void nextHashInTier() {
		hashlessIncrement(numPieces - 1);
		hash++;
	}

	private void hashlessIncrement(int i) {
		int current = pieces[i].pieceNum;
		if (current >= 0)
			numType.used[current] = false;
		current++;
		while (current < 16
				&& (numType.used[current] || notBase(pieces[i - 1].myPos,
						current))) {
			current++;
		}
		if (current == 16) {
			hashlessIncrement(i - 1);
			pieces[i].pieceNum = -1;
			hashlessIncrement(i);
		} else {
			pieces[i].pieceNum = current;
			pieces[i].myPos = getPos(pieces[i - 1].myPos, current);
			numType.used[current] = true;
		}
	}

	private Position getPos(Position pos, int current) {
		return pos == null || pos.inner == null ? null : pos.inner[current];
	}

	private boolean notBase(Position pos, int current) {
		return pos != null && pos.inner != null && pos.inner[current] == null;
	}

	public void reset() {
		setTier(numPieces);
	}

	public int getChildren(long[] children) {
		if (numPieces == 16)
			return 0;
		int childNum = 0;
		for (int childPiece = 0; childPiece < 16; childPiece++) {
			if (numType.used[childPiece])
				continue;
			for (int i = 0; i <= numPieces; i++) {
				long childHash = -1L;
				count.reset(numPieces + 1);
				rot.reset();
				Position place = tierTables[numPieces + 1];
				for (int k = 0; k <= numPieces; k++) {
					int pieceNum;
					if (k < i)
						pieceNum = pieces[k].pieceNum;
					else if (k > i)
						pieceNum = pieces[k - 1].pieceNum;
					else
						pieceNum = childPiece;
					if (i == 0)
						pieceNum ^= childPiece;
					child[k].pieceNum = pieceNum;
					child[k].applyRotation(rot);
					rot.dropState(child[k]);
					pieceNum = child[k].pieceNum;
					count.addPiece(pieceNum);
					if (place == null)
						childHash += count.lastHash();
					else if (place.inner == null) {
						childHash = place.offset;
						place = null;
						childHash += count.lastHash();
					} else if (k > 0)
						place = place.inner[pieceNum];
				}
				if (childHash < 0)
					childHash = place.offset;
				children[childNum++] = childHash;
			}
		}
		return childNum;
	}

	public static void main(String[] args) {
		QuartoMinorHasher qmh = new QuartoMinorHasher();
		qmh.setTier(5);
		long[] sizes = new long[17];
		for (int i = 1; i <= 16; i++) {
			sizes[i] = qmh.numHashesForTier(i);
		}
		System.out.println(Arrays.toString(sizes));
		int[] b = new int[5];
		int[] c = new int[6];
		long[] children = new long[66];
		for (long i = 0L; i < 1575; i++) {
			qmh.unhash(i);
			for (int k = 0; k < 5; k++) {
				b[k] = qmh.pieces[k].pieceNum;
			}
			int numChildren = qmh.getChildren(children);
			if (numChildren != 66)
				throw new Error("Wrong number of children");
			qmh.setTier(6);
			qmh.unhash(children[34]);
			for (int k = 0; k < 6; k++) {
				c[k] = qmh.pieces[k].pieceNum;
			}
			System.out.println(children[34] + ": " + Arrays.toString(c));
			qmh.setTier(5);
			long v = qmh.hash(b);
			if (i != v)
				throw new RuntimeException("Houston, we have a problem!");
			System.out.println(i + ": " + Arrays.toString(b));
		}
	}

	public boolean used(int i) {
		return numType.used[i];
	}

	/*
	 * Returns null if there isn't enough memory to cache all possible positions
	 * up to place
	 */
	public long[] getCache(int place, long availableMem) {
		return getCache(place, -1, availableMem);
	}

	public long[] getCache(int place, int piece, long availableRecordSpace) {
		assert place >= 0;
		assert place <= numPieces;
		assert piece >= 0 || piece == -1;
		assert piece < 16;
		Count count = new Count(numPieces + 1);
		Position pos = tierTables[numPieces + 1];
		Rotation rot = new Rotation();
		long neededRecordSpace = pos.numHashes;
		long startHash = 0L;
		Piece[] newPieces = new Piece[numPieces + 1];
		int piece0 = place == 0 ? piece : 0;
		newPieces[0] = new Piece(0);
		count.addPiece(0);
		for (int i = 1; neededRecordSpace > availableRecordSpace; i++) {
			int p;
			if (i < place)
				p = pieces[i].pieceNum;
			else if (piece == -1)
				return null;
			else if (i > place)
				p = pieces[i - 1].pieceNum;
			else
				p = piece;
			newPieces[i] = new Piece(p ^ piece0);
			newPieces[i].applyRotation(rot);
			rot.dropState(newPieces[i]);
			p = newPieces[i].pieceNum;
			count.addPiece(p);
			if (pos != null)
				if (pos.inner == null) {
					pos = null;
				} else {
					pos = pos.inner[p];
					assert pos != null;
					startHash = pos.offset;
					neededRecordSpace = pos.numHashes;
				}
			if (pos == null) {
				startHash += count.lastHash();
				neededRecordSpace = count.possibilities;
			}
		}
		return new long[] { startHash, neededRecordSpace };
	}

	public int[] getBoard() {
		int[] newBoard = new int[numPieces];
		for (int i = 0; i < numPieces; i++) {
			newBoard[i] = pieces[i].pieceNum;
		}
		return newBoard;
	}

	public void dropState(int[] board) {
		Rotation rot = new Rotation();
		Piece[] pieces = new Piece[board.length];
		for (int i = 0; i < pieces.length; i++) {
			pieces[i] = new Piece();
		}
		for (int k = 0; k < board.length; k++) {
			int pieceNum = board[k];
			pieceNum ^= board[0];
			pieces[k].pieceNum = pieceNum;
			pieces[k].applyRotation(rot);
			rot.dropState(pieces[k]);
			pieceNum = pieces[k].pieceNum;
		}
		for (int i = 0; i < pieces.length; i++) {
			board[i] = pieces[i].pieceNum;
		}
	}

	public String toString() {
		return Arrays.toString(Arrays.copyOf(pieces, numPieces));
	}
}