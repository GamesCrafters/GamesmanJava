package edu.berkeley.gamesman.hasher.symmetry;

import org.junit.Assert;
import org.junit.Test;

public class SquareTest {

	@Test
	public void testIndexGeneration() {
		for (int size = 1; size < 10; size++) {
			SquareSymmetryFinder s = new SquareSymmetryFinder(size);

			int[][] indices = s.getBoardIndices();

			int[][] answer = getCorrectBoardIndices(size);

			Assert.assertArrayEquals(answer, indices);
		}
	}

	@Test
	public void testMapping() {
		for (int size = 1; size < 6; size++) {
			SquareSymmetryFinder s = new SquareSymmetryFinder(size);

			int[] points = s.getFixedPoints();

			int[] answer = getCorrectFixedPoints(size);

			Assert.assertArrayEquals(answer, points);
		}
	}

	private int[] getCorrectFixedPoints(int size) {
		int[] f1 = { 0 };

		int[] f2 = { 3, 3, 3, 3 };

		int[] f3 = { 8, 7, 7, 7, 7, 3, 3, 3, 3 };

		int[] f4 = { 15, 15, 15, 15, 11, 11, 11, 11, 11, 11, 11, 11, 3, 3, 3, 3 };

		int[] f5 = { 24, 23, 23, 23, 23, 19, 19, 19, 19, 15, 15, 15, 15, 11,
				11, 11, 11, 11, 11, 11, 11, 3, 3, 3, 3 };

		int f6[] = new int[6 * 6];

		switch (size) {
		case 1:
			return f1;
		case 2:
			return f2;
		case 3:
			return f3;
		case 4:
			return f4;
		case 5:
			return f5;
		default:
			return f6;
		}
	}

	private int[][] getCorrectBoardIndices(int size) {
		int[][] b1 = { { 0 } };

		int[][] b2 = { { 0, 1 }, { 3, 2 } };

		int[][] b3 = { { 0, 4, 1 }, { 7, 8, 5 }, { 3, 6, 2 } };

		int[][] b4 = { { 0, 4, 5, 1 }, { 11, 12, 13, 6 }, { 10, 15, 14, 7 },
				{ 3, 9, 8, 2 } };

		int[][] b5 = { { 0, 4, 12, 5, 1 }, { 11, 16, 20, 17, 6 },
				{ 15, 23, 24, 21, 13 }, { 10, 19, 22, 18, 7 },
				{ 3, 9, 14, 8, 2 } };

		int[][] b6 = { { 0, 4, 12, 13, 5, 1 }, { 11, 20, 24, 25, 21, 6 },
				{ 19, 31, 32, 33, 26, 14 }, { 18, 30, 35, 34, 27, 15 },
				{ 10, 23, 29, 28, 22, 7 }, { 3, 9, 17, 16, 8, 2 } };

		int[][] b7 = { { 0, 4, 12, 20, 13, 5, 1 },
				{ 11, 24, 28, 36, 29, 25, 6 }, { 19, 35, 40, 44, 41, 30, 14 },
				{ 23, 39, 47, 48, 45, 37, 21 }, { 18, 34, 43, 46, 42, 31, 15 },
				{ 10, 27, 33, 38, 32, 26, 7 }, { 3, 9, 17, 22, 16, 8, 2 } };

		int[][] b8 = { { 0, 4, 12, 20, 21, 13, 5, 1 },
				{ 11, 28, 32, 40, 41, 33, 29, 6 },
				{ 19, 39, 48, 52, 53, 49, 34, 14 },
				{ 27, 47, 59, 60, 61, 54, 42, 22 },
				{ 26, 46, 58, 63, 62, 55, 43, 23 },
				{ 18, 38, 51, 57, 56, 50, 35, 15 },
				{ 10, 31, 37, 45, 44, 36, 30, 7 },
				{ 3, 9, 17, 25, 24, 16, 8, 2 } };

		int[][] b9 = { { 0, 4, 12, 20, 28, 21, 13, 5, 1 },
				{ 11, 32, 36, 44, 52, 45, 37, 33, 6 },
				{ 19, 43, 56, 60, 68, 61, 57, 38, 14 },
				{ 27, 51, 67, 72, 76, 73, 62, 46, 22 },
				{ 31, 55, 71, 79, 80, 77, 69, 53, 29 },
				{ 26, 50, 66, 75, 78, 74, 63, 47, 23 },
				{ 18, 42, 59, 65, 70, 64, 58, 39, 15 },
				{ 10, 35, 41, 49, 54, 48, 40, 34, 7 },
				{ 3, 9, 17, 25, 30, 24, 16, 8, 2 } };

		switch (size) {
		case 1:
			return b1;
		case 2:
			return b2;
		case 3:
			return b3;
		case 4:
			return b4;
		case 5:
			return b5;
		case 6:
			return b6;
		case 7:
			return b7;
		case 8:
			return b8;
		case 9:
			return b9;
		default:
			Assert.fail();
			return null;
		}
	}
}
