package edu.berkeley.gamesman.game.type;

public enum GameValue {
	LOSE(true), DRAW(false), TIE(true), WIN(true);
	private static GameValue[] values = values();

	public static GameValue valueOf(int i) {
		return values[i];
	}

	private GameValue opposite;
	public final boolean hasRemoteness;

	static {
		DRAW.opposite = DRAW;
		WIN.opposite = LOSE;
		LOSE.opposite = WIN;
		TIE.opposite = TIE;
	}

	private GameValue(boolean hasRemoteness) {
		this.hasRemoteness = hasRemoteness;
	}

	public GameValue opposite() {
		return opposite;
	}
}
