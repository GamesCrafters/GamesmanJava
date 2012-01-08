package edu.berkeley.gamesman.hadoop.game.baghchal;

public enum Direction {
	NORTH(1, 0, false), NE(1, 1, true), EAST(0, 1, false), SE(-1, 1, true), SOUTH(
			-1, 0, false), SW(-1, -1, true), WEST(0, -1, false), NW(1, -1, true);

	public final int dRow, dCol;
	public final boolean isDiagonal;

	private Direction(int dRow, int dCol, boolean isDiag) {
		this.dRow = dRow;
		this.dCol = dCol;
		this.isDiagonal = isDiag;
	}

	public static Direction valueOf(int dir) {
		return directions[dir];
	}

	private static Direction[] directions = values();
}
