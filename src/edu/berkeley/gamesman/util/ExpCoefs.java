package edu.berkeley.gamesman.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Coefficients for the expansion of (1 + x + x^2 + x^3 ... + x^d)^n where d is
 * the degree
 * 
 * @author dnspies
 */
public class ExpCoefs {
	int degree;

	ArrayList<long[]> triangle;

	ArrayList<long[]> sumTriangle;

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
		triangle = new ArrayList<long[]>(rows);
		this.degree = degree;
		triangle.add(new long[] { 1 });
		while (triangle.size() < rows)
			addRow();
	}

	private void addRow() {
		int rows = triangle.size();
		int rowLen = rows * degree + 1;
		long[] newRow = new long[rowLen];
		triangle.add(newRow);
		for (int i = 0; i < rowLen; i++)
			newRow[i] = getCoef(rows, i - 1) + getCoef(rows - 1, i)
					- getCoef(rows - 1, i - (degree + 1));
	}

	/**
	 * @param n
	 *            The power to raise the polynomial to.
	 * @param k
	 *            The power of x to find the coefficient of.
	 * @return The coefficient of x^k.
	 */
	public long getCoef(int n, int k) {
		if (k < 0 || k > n * degree)
			return 0;
		while (n >= triangle.size())
			addRow();
		return triangle.get(n)[k];
	}

	/**
	 * @param n
	 *            The power to raise the polynomial to.
	 * @param k
	 *            The power of x to find the coefficient of.
	 * @return The sum of coefficients of x^i from i=0 to k (inclusive).
	 */
	public long getSum(int n, int k) {
		if (k < 0)
			return 0;
		while (n >= sumTriangle.size())
			addSumRow();
		int lastVal = n * degree;
		if (k > lastVal)
			return sumTriangle.get(n)[lastVal];
		return sumTriangle.get(n)[k];
	}

	private void addSumRow() {
		int rows = sumTriangle.size();
		int rowLen = rows * degree + 1;
		long[] newRow = new long[rowLen];
		sumTriangle.add(newRow);
		for (int i = 0; i < rowLen; i++)
			newRow[i] = getSum(rows, i - 1) + getCoef(rows, i);
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		for (long[] row : triangle)
			s.append(Arrays.toString(row) + "\n");
		return s.toString();
	}
}
