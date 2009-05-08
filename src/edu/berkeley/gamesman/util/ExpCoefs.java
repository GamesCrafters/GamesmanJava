package edu.berkeley.gamesman.util;

import java.util.ArrayList;

/**
 * Coefficients for the expansion of (1 + x + x^2 + x^3 ... + x^d)^n where d is
 * the degree
 * 
 * @author dnspies
 */
public class ExpCoefs {
	int degree;

	ArrayList<int[]> triangle;

	/**
	 * @param degree
	 *            The degree of the triangle. This will be Pascal's Triangle if
	 *            degree is 1.
	 */
	public ExpCoefs(int degree) {
		this(degree, 1);
	}

	/**
	 * @param degree
	 *            The degree of the triangle. This will be Pascal's Triangle if
	 *            degree is 1.
	 * @param rows
	 *            The number of rows to initialize the triangle with
	 */
	public ExpCoefs(int degree, int rows) {
		triangle = new ArrayList<int[]>(rows);
		triangle.add(new int[] { 1 });
		while (rows >= triangle.size())
			addRow();
		this.degree = degree;
	}

	private void addRow() {
		int size = triangle.size();
		int rowLen = size * degree + 1;
		int[] newRow = new int[rowLen];
		triangle.add(newRow);
		for (int i = 0; i < rowLen; i++)
			newRow[i] = getCoef(size, i - 1) + getCoef(size - 1, i)
					+ getCoef(size - 1, i - (degree + 1));
	}

	/**
	 * @param n
	 *            The power to raise the polynomial to.
	 * @param k
	 *            The power of x to find the coefficient of.
	 * @return The coefficient of x^k.
	 */
	public int getCoef(int n, int k) {
		if (k < 0 || k > n * degree)
			return 0;
		while (n >= triangle.size())
			addRow();
		return triangle.get(n)[k];
	}
}
