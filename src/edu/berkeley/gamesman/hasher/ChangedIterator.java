package edu.berkeley.gamesman.hasher;

public class ChangedIterator {
	private int[] piecesChanged;
	private int len;
	int count;

	public ChangedIterator(int maxChangeable) {
		piecesChanged = new int[maxChangeable];
	}

	public ChangedIterator() {
		this(0);
	}

	public int next() {
		return piecesChanged[count++];
	}

	public boolean hasNext() {
		return count < len;
	}

	void add(int piece) {
		if (len == piecesChanged.length) {
			int[] newChanged = new int[len + 1];
			System.arraycopy(piecesChanged, 0, newChanged, 0, len);
			piecesChanged = newChanged;
		}
		piecesChanged[len++] = piece;
	}

	void reset() {
		len = 0;
		count = 0;
	}
}
