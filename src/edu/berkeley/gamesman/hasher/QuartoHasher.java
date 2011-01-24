package edu.berkeley.gamesman.hasher;

import java.util.LinkedList;
import java.util.TreeSet;

import edu.berkeley.gamesman.util.qll.QuickLinkedList;

public class QuartoHasher {
	private class Position {
		private final Rotation rotation;
		private final long arrangements;

		private final Position[] inner;

		Position(Rotation rotation, long arrangements,
				QuickLinkedList<Piece> unused) {
			this.rotation = rotation;
			this.arrangements = arrangements;
			if (rotation.fixedWall[0] && rotation.fixedWall[1]
					&& rotation.fixedWall[2])
				inner = null;
			else {
				inner = new Position[16];
				QuickLinkedList<Piece>.QLLIterator pieceIter = unused
						.iterator();
				while (pieceIter.hasNext()) {
					Piece p = pieceIter.next();
					int pNum = p.pieceNum;
					p.applyRotation(rotation);
					pieceIter.remove();
					Rotation newRot = rotation.clone();
					newRot.dropState(p);
					inner[pNum] = new Position(newRot, arrangements
							/ unused.size(), unused);
					p.reverseRotation(rotation);
					pieceIter.add(p);
				}
			}
		}
	}

	private class Rotation implements Cloneable {
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
					if (p.get(i) < p.get(i + 1))
						fixedWall[i] = true;
					else if (p.get(i) > p.get(i + 1)) {
						makeSwitch(p, i, i + 1);
						fixedWall[i] = true;
						if (i > 0)
							i -= 2;
					}
				}
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
	}

	private class Piece {
		int pieceNum;

		private void applyRotation(Rotation r) {
			int newNum = 0;
			for (int i = 0; i < 4; i++) {
				newNum = QuartoHasher.set(newNum, i, get(r.places[i]));
			}
			pieceNum = newNum;
		}

		public void reverseRotation(Rotation r) {
			int newNum = 0;
			for (int i = 0; i < 4; i++) {
				newNum = QuartoHasher.set(newNum, r.places[i], get(i));
			}
			pieceNum = newNum;
		}

		private int get(int i) {
			return QuartoHasher.get(pieceNum, i);
		}

		private void set(int i, int n) {
			pieceNum = QuartoHasher.set(pieceNum, i, n);
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

	public QuartoHasher() {

	}
}