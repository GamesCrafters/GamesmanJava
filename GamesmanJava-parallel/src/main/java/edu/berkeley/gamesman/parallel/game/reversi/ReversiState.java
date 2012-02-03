package edu.berkeley.gamesman.parallel.game.reversi;

import edu.berkeley.gamesman.game.type.GameValue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.io.WritableComparable;

abstract class ReversiState<T extends ReversiState<T>> implements
		WritableComparable<T> {
	final QuadSet board = new QuadSet();
	final int[] count = new int[3];
	public final int width, height;
	int turn;
	int passes;

	public ReversiState(int width, int height) {
		this.width = width;
		this.height = height;
		board.setNumEntries(width * height);
		Arrays.fill(count, 0);
		count[0] = width * height;
		set(height / 2 - 1, width / 2 - 1, 1);
		set(height / 2, width / 2, 1);
		set(height / 2 - 1, width / 2, 2);
		set(height / 2, width / 2 - 1, 2);
		passes = 0;
		turn = 1;
	}

	public int numPasses() {
		return passes;
	}

	public GameValue getPrimitiveValue() {
		if (passes < 2)
			return null;
		else {
			int opposite = flip(turn);
			if (count[turn] < count[opposite])
				return GameValue.LOSE;
			else if (count[turn] > count[opposite])
				return GameValue.WIN;
			else
				return GameValue.TIE;
		}
	}

	@Override
	public void write(DataOutput out) throws IOException {
		board.write(out);
		byte lastByte = makeLastByte();
		out.writeByte(lastByte);
	}

	private byte makeLastByte() {
		assert turn == 1 || turn == 2;
		assert passes >= 0 && passes <= 2;
		return (byte) ((turn << 2) | passes);
	}

	public int numPieces() {
		return count[1] + count[2];
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		board.readFields(in);
		Arrays.fill(count, 0);
		for (int i = 0; i < board.numEntries(); i++)
			count[board.get(i)]++;
		byte lastByte = in.readByte();
		turn = lastByte >>> 2;
		passes = lastByte & 3;
		assert (lastByte >>> 4) == 0;
	}

	public void set(T t) {
		assert height == t.height;
		assert width == t.width;
		turn = t.turn;
		passes = t.passes;
		board.set(t.board);
		System.arraycopy(t.count, 0, count, 0, 3);
	}

	@Override
	public int compareTo(T o) {
		return compareToOtherState(o);
	}

	public int compareToOtherState(ReversiState<?> o) {
		assert height == o.height;
		assert width == o.width;
		int diff = board.compareTo(o.board);
		if (diff != 0)
			return diff;
		diff = turn - o.turn;
		if (turn != o.turn)
			return turn - o.turn;
		if (passes != o.passes)
			return passes - o.passes;
		return 0;
	}

	private void set(int row, int col, int v) {
		count[get(row, col)]--;
		count[v]++;
		board.set(row * width + col, v);
	}

	public void set(int row, int col, char v) {
		set(row, col, pieceFor(v));
	}

	private static int pieceFor(char v) {
		switch (v) {
		case ' ':
			return 0;
		case 'X':
			return 1;
		case 'O':
			return 2;
		default:
			throw new IllegalArgumentException(Character.toString(v));
		}
	}

	public void setTurn(char turn) {
		setTurn(pieceFor(turn));
	}

	private void setTurn(int piece) {
		turn = piece;
	}

	public boolean makeMove(int row, int col) {
		if (get(row, col) != 0)
			return false;
		boolean flipFound = false;
		int opposite = flip(turn);
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -1; dx <= 1; dx++) {
				if (dx == 0 && dy == 0)
					continue;
				int nRow = row + dy, nCol = col + dx;
				if (isType(nRow, nCol, opposite)) {
					int dist;
					for (dist = 2;; dist++) {
						nRow += dy;
						nCol += dx;
						if (!isType(nRow, nCol, opposite))
							break;
					}
					if (isType(nRow, nCol, turn)) {
						flipFound = true;
						for (int d = 1; d < dist; d++) {
							nRow -= dy;
							nCol -= dx;
							flip(nRow, nCol);
						}
					}
				}
			}
		}
		if (flipFound) {
			set(row, col, turn);
			flipTurn();
			passes = 0;
			return true;
		} else
			return false;
	}

	private void flip(int row, int col) {
		set(row, col, flip(get(row, col)));
	}

	private boolean isType(int row, int col, int color) {
		return inBounds(row, col) && get(row, col) == color;
	}

	private boolean inBounds(int row, int col) {
		return row >= 0 && row < height && col >= 0 && col < width;
	}

	public void makePass() {
		flipTurn();
		passes++;
	}

	private void flipTurn() {
		turn = flip(turn);
	}

	private static int flip(int x) {
		switch (x) {
		case 1:
			return 2;
		case 2:
			return 1;
		default:
			throw new IllegalArgumentException(x + " is not flippable");
		}
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof ReversiState && tEquals((ReversiState<?>) o);
	}

	@Override
	public int hashCode() {
		return (31 + board.hashCode()) * 31 + makeLastByte();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int row = height - 1; row >= 0; row--) {
			sb.append(row + 1);
			for (int col = 0; col < width; col++) {
				sb.append(" " + getChar(row, col));
			}
			sb.append("\n");
		}
		sb.append(" ");
		for (int col = 0; col < width; col++) {
			sb.append(" " + Character.toString((char) (col + 'A')));
		}
		sb.append("\n");
		return sb.toString();
	}

	public char getChar(int row, int col) {
		return getChar(get(row, col));
	}

	private int get(int row, int col) {
		return board.get(row * width + col);
	}

	private static char getChar(int piece) {
		switch (piece) {
		case 0:
			return ' ';
		case 1:
			return 'X';
		case 2:
			return 'O';
		default:
			return '?';
		}
	}

	public boolean tEquals(ReversiState<?> o) {
		return compareToOtherState(o) == 0;
	}
}