package edu.berkeley.gamesman.hadoop.game.tictactoe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import edu.berkeley.gamesman.propogater.writable.list.WritableList;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;

import edu.berkeley.gamesman.game.tree.GameTree;
import edu.berkeley.gamesman.game.type.GameValue;

public class TicTacToe extends GameTree<TTTState> implements
		SolveReader<TTTState> {

	public TicTacToe() {
	}

	@Override
	public Collection<TTTState> getRoots() {
		return Collections.singleton(new TTTState());
	}

	@Override
	public void getChildren(TTTState position, WritableList<TTTState> toFill) {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				if (position.get(row, col) == 0) {
					TTTState state = toFill.add();
					state.set(position);
					state.play(row, col);
				}
			}
		}
	}

	@Override
	protected GameValue getPrimitiveValue(TTTState position) {
		if (position.isWin()) {
			return GameValue.LOSE;
		} else if (position.isFull()) {
			return GameValue.TIE;
		} else
			return null;
	}

	@Override
	public Class<TTTState> getKeyClass() {
		return TTTState.class;
	}

	@Override
	public int getDivision(TTTState position) {
		return position.numPieces();
	}

	@Override
	public boolean isSingleLinear() {
		return true;
	}

	@Override
	public TTTState getPosition(String board) {
		TTTState state = new TTTState();
		int i = 0;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				state.set(row, col, board.charAt(i++));
			}
		}
		return state;
	}

	@Override
	public Collection<Pair<String, TTTState>> getChildren(TTTState position) {
		ArrayList<Pair<String, TTTState>> result = new ArrayList<Pair<String, TTTState>>(
				9);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				if (position.get(row, col) == 0) {
					TTTState nextState = new TTTState();
					nextState.set(position);
					nextState.play(row, col);
					result.add(new Pair<String, TTTState>(Character
							.toString((char) (col + 'A')) + (row + 1),
							nextState));
				}
			}
		}
		return result;
	}

	@Override
	public String getString(TTTState position) {
		StringBuilder sb = new StringBuilder(9);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				sb.append(position.getChar(row, col));
			}
		}
		return sb.toString();
	}
}
