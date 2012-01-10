package edu.berkeley.gamesman.hadoop.game.baghchal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.game.tree.GameTree;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.propogater.writable.list.WritableList;

public class BaghChal<T extends BaghChalState<T>> extends GameTree<T> implements
		Configurable {
	private static final HashMap<List<Integer>, Class<? extends BaghChalState>> stateMap = new HashMap<List<Integer>, Class<? extends BaghChalState>>();

	private static final int DEFAULT_WIDTH = 3, DEFAULT_HEIGHT = 3;

	private static final int DEFAULT_TIGERS = 2, DEFAULT_GOATS = 6;

	static {
		stateMap.put(Arrays.asList(5, 5, 4, 20), StandardBCState.class);
		stateMap.put(Arrays.asList(3, 3, 2, 6), TinyBCState.class);
	}

	private final class Square {
		public final int row, col, index;
		public final Square[] neighbors;

		public Square(int row, int col, int index) {
			this.row = row;
			this.col = col;
			this.index = index;
			neighbors = new BaghChal.Square[8];
		}
	}

	private Class<T> keyClass;
	private Square[][] board;
	private Square[] squares;
	private int width, height, boardSize;
	private int numStartingTigers, numStartingGoats;

	public BaghChal(Configuration conf) {
		setConf(conf);
	}

	public BaghChal() {
	}

	@Override
	protected GameValue getPrimitiveValue(T position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<T> getRoots() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getChildren(T position, WritableList<T> toFill) {
		// TODO Auto-generated method stub

	}

	@Override
	public Class<T> getKeyClass() {
		return keyClass;
	}

	@Override
	protected void configure(Configuration conf) {
		this.width = conf.getInt("game.width", DEFAULT_WIDTH);
		this.height = conf.getInt("game.height", DEFAULT_HEIGHT);
		boardSize = width * height;
		this.numStartingTigers = conf.getInt("game.tigers", DEFAULT_TIGERS);
		this.numStartingGoats = conf.getInt("game.goats", DEFAULT_GOATS);

		board = new BaghChal.Square[height][width];
		squares = new BaghChal.Square[boardSize];
		int index = 0;
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				board[row][col] = new Square(row, col, index);
				squares[index] = board[row][col];
				index++;
			}
		}
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				for (Direction d : Direction.values()) {
					if ((row + col) % 2 == 0 || (!d.isDiagonal)) {
						board[row][col].neighbors[d.ordinal()] = get(row
								+ d.dRow, col + d.dCol);
					}
				}
			}
		}
		keyClass = (Class<T>) stateMap.get(Arrays.asList(width, height,
				numStartingTigers, numStartingGoats));
	}

	private Square get(int row, int col) {
		if (inBounds(row, col))
			return board[row][col];
		else
			return null;
	}

	private boolean inBounds(int row, int col) {
		return row >= 0 && row < height && col >= 0 && col < width;
	}
}
