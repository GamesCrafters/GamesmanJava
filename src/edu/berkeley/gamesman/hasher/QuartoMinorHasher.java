package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;
import edu.berkeley.gamesman.util.qll.Factory;

public final class QuartoMinorHasher {
	private static long pick(int n, int k) {
		if (k <= 0)
			return 1L;
		else
			return n * pick(n - 1, k - 1);
	}

	private final class Position {
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

	private final class Rotation implements Cloneable {
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
		}
	}

	private final class Piece {
		int pieceNum;

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

		public void reverseRotation(Rotation r) {
			int newNum = 0;
			for (int i = 0; i < 4; i++) {
				newNum = QuartoMinorHasher.set(newNum, r.places[i], get(i));
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
			StringBuilder sb = new StringBuilder(4);
			for (int i = 3; i >= 0; i--) {
				sb.append(get(i));
			}
			return sb.toString();
		}
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
	private final Position[] symPositions;
	private int syms;
	private long hash;
	private final Pool<Rotation> rotPool = new Pool<Rotation>(
			new Factory<Rotation>() {

				@Override
				public Rotation newObject() {
					return new Rotation();
				}

				@Override
				public void reset(Rotation t) {
					t.reset();
				}

			});
	private final Pool<boolean[]> usedPool = new Pool<boolean[]>(
			new Factory<boolean[]>() {
				@Override
				public boolean[] newObject() {
					return new boolean[16];
				}

				@Override
				public void reset(boolean[] t) {
					Arrays.fill(t, false);
				}
			});
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
		symPositions = new Position[16];
		for (int i = 0; i < 16; i++) {
			pieces[i] = new Piece();
		}
		numPieces = 0;
	}

	public void setTier(int tier) {
		numPieces = tier;
		Position curPos = tierTables[tier];
		syms = 1;
		for (int i = 0; i < tier; i++) {
			pieces[i].pieceNum = i;
			symPositions[i] = curPos;
			if (curPos == null || curPos.inner == null)
				curPos = null;
			else {
				curPos = curPos.inner[i + 1];
				syms = i + 2;
			}
		}
		hash = 0L;
	}

	public long numHashesForTier(int tier) {
		return tierTables[tier].numHashes;
	}

	public long getHash() {
		return hash;
	}

	public long hash(int[] board) {
		for (int i = 0; i < numPieces; i++) {
			pieces[i].pieceNum = board[i] ^ board[0];
		}
		Rotation rot = rotPool.get();
		Position p = tierTables[numPieces];
		symPositions[0] = p;
		int i;
		for (i = 1; i < numPieces; i++) {
			if (p.inner == null)
				break;
			pieces[i].applyRotation(rot);
			rot.dropState(pieces[i]);
			p = p.inner[pieces[i].pieceNum];
			symPositions[i] = p;
			if (p == null)
				throw new NullPointerException();
		}
		syms = i;
		hash = p.offset;
		for (; i < numPieces; i++) {
			symPositions[i] = null;
			pieces[i].applyRotation(rot);
			int num = pieces[i].pieceNum;
			for (int k = 0; k < i; k++)
				if (pieces[k].pieceNum < pieces[i].pieceNum)
					num--;
			hash += num * pick(16 - i - 1, numPieces - i - 1);
		}
		rotPool.release(rot);
		return hash;
	}

	public void setTierAndHash(int tier, int[] board) {
		numPieces = tier;
		hash(board);
	}

	public void unhash(long hash) {
		Position p = tierTables[numPieces];
		this.hash = hash;
		pieces[0].pieceNum = 0;
		boolean[] used = usedPool.get();
		used[0] = true;
		symPositions[0] = p;
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
			pieces[i].pieceNum = nextK;
			used[nextK] = true;
			p = nextP;
			symPositions[i] = p;
		}
		syms = i;
		hash -= p.offset;
		for (; i < numPieces; i++) {
			symPositions[i] = null;
			long div = pick(16 - i - 1, numPieces - i - 1);
			int nextNum = (int) (hash / div);
			hash = hash % div;
			for (int k = 0; k <= nextNum; k++) {
				if (used[k])
					nextNum++;
			}
			pieces[i].pieceNum = nextNum;
			used[nextNum] = true;
		}
		usedPool.release(used);
	}

	public int get(int index) {
		return pieces[index].pieceNum;
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
		for (long i = 0L; i < 1575; i++) {
			qmh.unhash(i);
			for (int k = 0; k < 5; k++) {
				b[k] = qmh.pieces[k].pieceNum;
			}
			System.out.println(i + ": " + Arrays.toString(b));
		}
	}

	public void nextHashInTier() {
		unhash(hash + 1);
	}

	public void reset() {
		setTier(numPieces);
	}
}