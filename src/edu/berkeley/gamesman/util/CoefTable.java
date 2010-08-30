package edu.berkeley.gamesman.util;

public class CoefTable {
	long[][] posTable;
	long[][] negTable;
	int[] nLength;
	final int degree;

	public static void main(String[] args) {
		CoefTable ct = new CoefTable(Integer.parseInt(args[2]));
		System.out.println(ct.get(Integer.parseInt(args[0]),
				Integer.parseInt(args[1])));
	}

	public CoefTable() {
		this(1);
	}

	public CoefTable(int degree) {
		if (degree <= 0)
			throw new ArithmeticException("Degree must be positive");
		this.degree = degree;
	}

	public long get(int n, int k) {
		if (n < 0)
			return getNeg(-n - 1, k);
		if (k < 0)
			return 0;
		ensureLength(n);
		if (k >= nLength[n])
			return 0;
		else if (k >= nLength[n] / 2)
			k = nLength[n] - (k + 1);
		ensureRow(n, k);
		if (posTable[n][k] == 0) {
			if (n == 0)
				posTable[n][k] = 1;
			else
				posTable[n][k] = get(n - 1, k) + get(n, k - 1)
						- get(n - 1, k - (degree + 1));
		}
		return posTable[n][k];
	}

	private long getNeg(int n, int k) {
		if (k < 0)
			return 0;
		ensureNegLength(n, k);
		if (negTable[n][k] == Long.MIN_VALUE) {
			if (n == 0)
				negTable[n][k] = k % (degree + 1) <= 1 ? -(k % (degree + 1) * 2 - 1)
						: 0;
			else
				negTable[n][k] = getNeg(n - 1, k) - getNeg(n - 1, k - 1)
						+ getNeg(n, k - (degree + 1));
		}
		return negTable[n][k];
	}

	private void ensureNegLength(int n, int k) {
		if (negTable == null) {
			createNegTable(n, k);
		} else if (negTable.length <= n) {
			expandNegTable(n, k);
		} else if (negTable[n] == null) {
			createNegRow(n, k);
		} else if (negTable[n].length <= k) {
			expandNegRow(n, k);
		}
	}

	private void createNegTable(int n, int k) {
		negTable = new long[n + 1][];
		createNegRow(n, k);
	}

	private void expandNegTable(int n, int k) {
		long[][] newNegTable = new long[n + 1][];
		System.arraycopy(negTable, 0, newNegTable, 0, negTable.length);
		negTable = newNegTable;
		createNegRow(n, k);
	}

	private void createNegRow(int n, int k) {
		negTable[n] = new long[k + 1];
		for (int i = 0; i <= k; i++)
			negTable[n][i] = Long.MIN_VALUE;
	}

	private void expandNegRow(int n, int k) {
		long[] newNegRow = new long[k + 1];
		System.arraycopy(negTable[n], 0, newNegRow, 0, negTable[n].length);
		for (int i = negTable[n].length; i <= k; i++)
			newNegRow[i] = Long.MIN_VALUE;
		negTable[n] = newNegRow;
	}

	private void ensureLength(int n) {
		if (posTable == null)
			createTable(n);
		else if (posTable.length <= n)
			expandTable(n);
	}

	private void ensureRow(int n, int k) {
		if (posTable[n] == null) {
			createRow(n, k);
		} else if (posTable[n].length <= k)
			expandRow(n, k);
	}

	private void createTable(int n) {
		posTable = new long[n + 1][];
		nLength = new int[n + 1];
		int totLength = 1;
		for (int i = 0; i <= n; i++) {
			nLength[i] = totLength;
			totLength += degree;
		}
	}

	private void expandTable(int n) {
		long[][] newTable = new long[n + 1][];
		System.arraycopy(posTable, 0, newTable, 0, posTable.length);
		int[] newLength = new int[n + 1];
		System.arraycopy(nLength, 0, newLength, 0, nLength.length);
		int totLength = nLength[nLength.length - 1];
		for (int i = nLength.length - 1; i <= n; i++) {
			newLength[i] = totLength;
			totLength += degree;
		}
		nLength = newLength;
	}

	private void createRow(int n, int k) {
		posTable[n] = new long[k + 1];
	}

	private void expandRow(int n, int k) {
		long[] newRow = new long[k + 1];
		System.arraycopy(posTable[n], 0, newRow, 0, posTable[n].length);
		posTable[n] = newRow;
	}
}
