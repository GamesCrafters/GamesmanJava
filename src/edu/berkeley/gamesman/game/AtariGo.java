package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;

public class AtariGo extends RectangularDartboardGame {
	boolean[] checked = new boolean[gameSize];
	boolean current;

	public AtariGo(Configuration conf) {
		super(conf, NO_TIE);
	}

	@Override
	public Value primitiveValue() {
		boolean hasWin = false;
		char turn = getTier() % 2 == 0 ? 'X' : 'O';
		int i = 0;
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if ((!hasWin) && checked[i] == current && get(i) == turn)
					hasWin = !canBreathe(i, row, col, turn);
				else
					checked[i] = !current;
				i++;
			}
		}
		current = !current;
		return hasWin ? Value.LOSE : Value.UNDECIDED;
	}

	public boolean canBreathe(int i, int row, int col, char c) {
		if (row < 0 || row >= gameHeight || col < 0 || col >= gameWidth) {
			return false;
		}
		char cur = get(i);
		if (cur == ' ')
			return true;
		else if (cur != c)
			return false;
		else if (checked[i] != current)
			return false;
		else {
			checked[i] = !current;
			boolean a = canBreathe(i + gameWidth, row + 1, col, c), b = canBreathe(
					i + 1, row, col + 1, c), d = canBreathe(i - gameWidth,
					row - 1, col, c), e = canBreathe(i - 1, row, col - 1, c);
			return a || b || d || e;
		}
	}

	@Override
	public String describe() {
		return gameWidth + "x" + gameHeight + " Atari Go";
	}
}
