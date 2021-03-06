package edu.berkeley.gamesman.parallel.game.reversi;

import edu.berkeley.gamesman.propogater.common.Adder;
import edu.berkeley.gamesman.propogater.common.RawByteComparator;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.game.tree.GameTree;
import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.util.ReflectionUtils;

public class Reversi<T extends ReversiState<T>> extends GameTree<T> implements
		Configurable, SolveReader<T, GameRecord> {

	private static final int DEFAULT_WIDTH = 4, DEFAULT_HEIGHT = 4;
	private int height, width;
	private Class<T> stateClass;

	public Reversi() {
	}

	public Reversi(Configuration conf) {
		setConf(conf);
	}

	@Override
	public Collection<T> getRoots() {
		return Collections.singleton(newState());
	}

	private T newState() {
		return ReflectionUtils.newInstance(stateClass, getConf());
	}

	private T tempState;
	private int boardSize;

	@Override
	public void getChildren(T position, Adder<T> toFill) {
		tempState.set(position);
		boolean addedAny = false;
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				if (tempState.makeMove(row, col)) {
					toFill.add().set(tempState);
					tempState.set(position);
					addedAny = true;
				}
			}
		}
		if (!addedAny) {
			tempState.makePass();
			toFill.add().set(tempState);
		}
	}

	@Override
	public Class<T> getKeyClass() {
		return stateClass;
	}

	@Override
	public int getDivision(T position) {
		int passes = position.numPasses();
		return passes == 2 ? 2 * (boardSize + 1) : position.numPieces() * 2
				+ position.numPasses();
	}

	@Override
	public GameValue getPrimitiveValue(T position) {
		return position.getPrimitiveValue();
	}

	public static int getWidth(Configuration conf) {
		if (conf == null)
			return DEFAULT_WIDTH;
		int width = conf.getInt("reversi.width", DEFAULT_WIDTH);
		return width;
	}

	public static int getHeight(Configuration conf) {
		if (conf == null)
			return DEFAULT_HEIGHT;
		int height = conf.getInt("reversi.height", DEFAULT_HEIGHT);
		return height;
	}

	@Override
	public void configure(Configuration conf) {
		height = getHeight(conf);
		width = getWidth(conf);
		try {
			stateClass = Reversi.<T> getStateClass(width, height);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		boardSize = height * width;
		tempState = newState();
	}

	private static <T extends ReversiState<T>> Class<T> getStateClass(
			int width, int height) throws ClassNotFoundException {
		return (Class<T>) Class.forName(
				ReversiState.class.getName() + Integer.toString(width)
						+ Integer.toString(height)).asSubclass(
				ReversiState.class);
	}

	@Override
	public boolean isSingleLinear() {
		return true;
	}

	public static <T extends ReversiState<T>> void defineComparator(
			Class<T> stateClass) {
		WritableComparator.define(stateClass, RawByteComparator.instance);
	}

	@Override
	public T getPosition(String board) {
		T state = newState();
		state.setTurn(board.charAt(0));
		int i = 1;
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				state.set(row, col, board.charAt(i++));
			}
		}
		while (canPass(state)) {
			state.makePass();
		}
		return state;
	}

	// Warning!: inefficient
	private boolean canPass(T state) {
		if (state.numPasses() >= 2)
			return false;
		Collection<Pair<String, T>> children = getChildren(state);
		return children.iterator().next().car.equals("pass");
	}

	@Override
	public Collection<Pair<String, T>> getChildren(T position) {
		ArrayList<Pair<String, T>> result = new ArrayList<Pair<String, T>>(
				boardSize);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				T tempState = newState();
				tempState.set(position);
				if (tempState.makeMove(row, col)) {
					result.add(new Pair<String, T>(Character
							.toString((char) (col + 'A')) + (row + 1),
							tempState));
				}
			}
		}
		if (result.isEmpty()) {
			T tempState = newState();
			tempState.set(position);
			tempState.makePass();
			result.add(new Pair<String, T>("pass", tempState));
		}
		return result;
	}

	@Override
	public String getString(T position) {
		StringBuilder sb = new StringBuilder();
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				sb.append(position.getChar(row, col));
			}
		}
		return sb.toString();
	}

	@Override
	public GameRecord getRecord(T position, GameRecord fetchedRec) {
		return fetchedRec;
	}
}
