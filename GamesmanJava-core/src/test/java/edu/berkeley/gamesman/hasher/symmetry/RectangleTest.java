package edu.berkeley.gamesman.hasher.symmetry;

import org.junit.Assert;
import org.junit.Test;

public class RectangleTest {

	@Test
	public void testIndexGeneration() {
		RectangleSymmetryFinder s = new RectangleSymmetryFinder(3, 2);
		int[][] indices = s.getBoardIndices();
		Assert.assertArrayEquals(new int[][] { { 5, 2 }, { 4, 1 }, { 3, 0 } },
				indices);

		s = new RectangleSymmetryFinder(4, 5);
		indices = s.getBoardIndices();
		Assert.assertArrayEquals(
				new int[][] { { 7, 15, 19, 11, 3 }, { 6, 14, 18, 10, 2 },
						{ 5, 13, 17, 9, 1 }, { 4, 12, 16, 8, 0 } }, indices);
	}

	@Test
	public void testMapping() {
		RectangleSymmetryFinder s = new RectangleSymmetryFinder(3, 2);
		int[] points = s.getFixedPoints();
		Assert.assertArrayEquals(new int[] { 5, 5, 5, 5, 4, 3 }, points);

		s = new RectangleSymmetryFinder(4, 5);
		points = s.getFixedPoints();
		Assert.assertArrayEquals(new int[] { 19, 18, 17, 16, 15, 15, 15, 15,
				15, 14, 13, 12, 7, 7, 7, 7, 7, 6, 5, 4 }, points);
	}
}
