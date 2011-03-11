package edu.berkeley.gamesman.hasher;

public class C4ChangedIterator {
	private int[][] piecesChanged;
	private int len;
	int count;

	public C4ChangedIterator(int maxChangeable) {
		piecesChanged = new int[maxChangeable][2];
	}

	public C4ChangedIterator() {
		this(0);
	}

	public void next(int[] point) {
		point[0] = piecesChanged[count][0];
		point[1] = piecesChanged[count][1];
		count++;
	}

	public boolean hasNext() {
		return count < len;
	}

	void add(int col, int row) {
		if (len == piecesChanged.length) {
			int[][] newChanged = new int[len + 1][2];
			System.arraycopy(piecesChanged, 0, newChanged, 0, len);
			piecesChanged = newChanged;
		}
		piecesChanged[len][0] = col;
		piecesChanged[len][1] = row;
		len++;
	}

	void reset() {
		len = 0;
		count = 0;
	}
}
