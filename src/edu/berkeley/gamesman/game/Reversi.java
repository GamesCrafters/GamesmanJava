package edu.berkeley.gamesman.game;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.DartboardHasher;
import edu.berkeley.gamesman.util.Pair;

public class Reversi extends TierGame {
	private final int width;
	private final int height;
	private final int boardSize;
	private final Cell[][] board;
	private final DartboardHasher dbh;
	private int turn; // 1 for black, 0 for white.
	private final static int BLACK = 1;
	private final static int WHITE = 0;
	private TierState[] children;
	private boolean isChildrenValid;
	private final long[][][] offsetTable;
	// offsetTable is a table of offsets based on the tier, or number of pieces
	// and each element is an array of all possible ways to order the board with
	// increasing number of white vs black pieces(first element is ways to order
	// the board with 0 white pieces, second is with 1 white piece, etc...)
	// tier
	// 0 [ [1]
	// 1 [64,128]
	// 2 [4032,...]
	// ...
	// 63 [64,...] ]

	private String[] stringMoves; // only for testing.
	private final int[] numPieces = new int[2];

	private class Cell {
		final int row, col;
		final int boardNum;

		public Cell(int row, int col, int boardNum) {
			this.row = row;
			this.col = col;
			this.boardNum = boardNum;
			dbh.set(boardNum, ' ');
		}

		public char getPiece() {
			return dbh.get(boardNum);
		}

		public void setPiece(char p) {
			if (dbh.get(boardNum) != ' ' && p == ' ')
				if (dbh.get(boardNum) == 'X')
					numPieces[BLACK]--;
				else
					numPieces[WHITE]--;
			else if (dbh.get(boardNum) == ' ' && p != ' ')
				if (p == 'X')
					numPieces[BLACK]++;
				else
					numPieces[WHITE]++;
			if (!(p == ' ' || p == 'X' || p == 'O'))
				throw new Error("Bad piece: " + p);
			dbh.set(boardNum, p);
		}
	}

	public Reversi(Configuration conf) {
		super(conf);
		width = conf.getInteger("gamesman.game.width", 8);
		height = conf.getInteger("gamesman.game.height", 8);
		boardSize = width * height;
		board = new Cell[height][width];
		dbh = new DartboardHasher(boardSize, ' ', 'O', 'X');
		offsetTable = new long[boardSize + 1][2][];
		// initialize offset table
		for (int tier = 0; tier < boardSize; tier++) {
			long total = 0;
			for (int turn = 0; turn < 2; turn++) {
				offsetTable[tier][turn] = new long[tier + 1];
				for (int offset = 0; offset <= tier; offset++) {
					dbh.setNums(boardSize - tier, offset, tier - offset);
					offsetTable[tier][turn][offset] = total;
					total += dbh.numHashes();
				}
			}
		}
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				board[row][col] = new Cell(row, col, row * width + col);
			}
		}
		turn = BLACK;
		board[height / 2 - 1][width / 2 - 1].setPiece('X');
		board[height / 2][width / 2 - 1].setPiece('O');
		board[height / 2 - 1][width / 2].setPiece('O');
		board[height / 2][width / 2].setPiece('X');
		isChildrenValid = false;
		children = new TierState[0];
		stringMoves = new String[0]; // only for testing.
	}

	@Override
	public void setState(TierState pos) {
		unhash(pos.tier, pos.hash);
		isChildrenValid = false;
	}

	private void unhash(int tier, long hash) {
		if (hash > offsetTable[tier][1][0]) {
			turn = 1;
			hash -= offsetTable[tier][1][0];
		} else {
			turn = 0;
		}
		int offset = Arrays.binarySearch(offsetTable[tier][turn], hash);
		if (offset < 0)
			offset = -offset - 2;
		hash -= offsetTable[tier][turn][offset];
		dbh.unhash(hash);
	}

	@Override
	public Value primitiveValue() {
		if (!(isChildrenValid))
			getChildren();
		if (children.length == 0) {
			return numPieces[turn] > numPieces[Math.abs(turn - 1)] ? Value.WIN
					: Value.LOSE;
		} else {
			return Value.UNDECIDED;
		}
	}

	@Override
	public Collection<Pair<String, TierState>> validMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getTier() {
		return numPieces[BLACK] + numPieces[WHITE];
	}

	@Override
	public String stateToString() {
		String answer = "";
		answer += (this.turn == BLACK ? 'X' : 'O');
		for (int boardNumber = 0; boardNumber < boardSize; boardNumber++) {
			answer += dbh.get(boardNumber);
		}
		return answer;
	}

	@Override
	public void setFromString(String pos) {
		if (pos.length() != boardSize + 1)
			throw new Error("Bad String - wrong length: " + pos.length()
					+ " != " + boardSize);
		char[] posArray = pos.toCharArray();
		char turn = posArray[0];
		if (turn == 'X')
			this.turn = BLACK;
		else if (turn == 'O')
			this.turn = WHITE;
		else
			throw new Error("Bad turn");
		char[] woTurn = Arrays.copyOfRange(posArray, 1, posArray.length - 1);
		dbh.setNumsAndHash(woTurn);
	}

	@Override
	public void getState(TierState state) {
		state.tier = getTier();
		state.hash = getHash();
	}

	private long getHash() {
		return offsetTable[getTier()][turn][numPieces[WHITE]] + dbh.getHash();
	}

	@Override
	public long numHashesForTier(int tier) {
		return offsetTable[tier][1][0] * 2;
	}

	@Override
	public String displayState() {
		String s = stateToString();
		StringBuilder str = new StringBuilder((width + 3) * height);
		str.append((s.charAt(0) == 'O' ? "White to Move" : "Black to Move") + "\n");
		s = s.substring(1);
		for (int row = 0; row < height; row++)
			str.append("|" + s.substring(row * width, row * width + width)
					+ "|\n");
		return str.toString();
	}

	@Override
	public void setStartingPosition(int n) {
		dbh.setNums(boardSize, 0, 0);
		board[height / 2 - 1][width / 2 - 1].setPiece('X');
		board[height / 2][width / 2 - 1].setPiece('O');
		board[height / 2 - 1][width / 2].setPiece('O');
		board[height / 2][width / 2].setPiece('X');

		turn = BLACK;
		isChildrenValid = false;
	}

	@Override
	public int numStartingPositions() {
		return 1;
	}

	@Override
	public boolean hasNextHashInTier() {
		long currentHash = dbh.getHash();
		return (currentHash != numHashesForTier(getTier()) - 1);
	}

	@Override
	public void nextHashInTier() {
		if (dbh.getHash() < dbh.numHashes() - 1)
			dbh.next();
		else {
			dbh.setNums(boardSize - getTier(), ++numPieces[WHITE],
					--numPieces[BLACK]);
		}
	}

	@Override
	public int numberOfTiers() {
		return boardSize + 1;
	}

	@Override
	public int maxChildren() {
		return boardSize;
	}

	@Override
	public int validMoves(TierState[] moves) {
		if (!(isChildrenValid))
			getChildren(moves);
		else
			System.arraycopy(children, 0, moves, 0, children.length);
		return children.length;
	}
	
	private void getChildren() {
		this.getChildren(new TierState[maxChildren()]);
		int counter = 0;
		for (int x = 0; x < children.length; x++) {
			if (children[x] != null)
				counter++;
		}
		TierState[] tempChildren = new TierState[counter];
		for (int y = 0; y < counter; y++) {
			tempChildren[y] = children[y];
		}
		children = tempChildren;
	}

	private void getChildren(TierState[] moves) {
		children = moves;
		String originalString = stateToString();
		int counter = 0;
		stringMoves = new String[0]; // only for testing.
		// looks at every spot on the board
		for (int boardNumber = 0; boardNumber < boardSize; boardNumber++) {
			// only a valid child if there is an empty space there
			if (dbh.get(boardNumber) == ' ') {
				int[] childrenNumbers = { boardNumber - width - 1,
						boardNumber - width, boardNumber - width + 1,
						boardNumber - 1, boardNumber + 1,
						boardNumber + width - 1, boardNumber + width,
						boardNumber + width + 1 };
				// looks at each spot around the given spot
				for (int index = 0; index < childrenNumbers.length; index++) {
					// makes sure the spot is within bounds, then checks if
					// there is an opposing piece next to it
					if (((childrenNumbers[index] >= 0 && childrenNumbers[index] < boardSize))
							&& (((turn == WHITE && dbh
									.get(childrenNumbers[index]) == 'X') || (turn == BLACK && dbh
									.get(childrenNumbers[index]) == 'O')) && isFlippable(
									boardNumber, index, false, null))) {
						String[] newStringMoves = new String[stringMoves.length + 1]; // only
																						// for
																						// testing.
						System.arraycopy(stringMoves, 0, newStringMoves, 0,
								stringMoves.length); // only for testing.
						newStringMoves[newStringMoves.length - 1] = ""
								+ (boardNumber / width) + ""
								+ (boardNumber % height); // only for testing.
						stringMoves = newStringMoves; // only for testing.
						String[] stringState = { stateToString().substring(1) };
						boolean x = isFlippable(boardNumber, index, true,
								stringState);
						System.out.println(children[counter]);
						children[counter].tier = getTier() + 1;
						children[counter].hash = dbh.hash(stringState[0].toCharArray());
						counter++;
						break;
					}
				}
			}
		}
		isChildrenValid = true;
		dbh.hash(originalString.toCharArray());
	}

	private boolean isFlippable(int boardNumber, int direction, boolean flip,
			String[] state) {
		int addRow = 0;
		int addCol = 0;
		switch (direction) {
		case 0:
			addRow = -1;
			addCol = -1;
			break;
		case 1:
			addRow = -1;
			addCol = 0;
			break;
		case 2:
			addRow = -1;
			addCol = 1;
			break;
		case 3:
			addRow = 0;
			addCol = -1;
			break;
		case 4:
			addRow = 0;
			addCol = 1;
			break;
		case 5:
			addRow = 1;
			addCol = -1;
			break;
		case 6:
			addRow = 1;
			addCol = 0;
			break;
		case 7:
			addRow = 1;
			addCol = 1;
			break;
		default:
			throw new Error("Bad direction");
		}
		int row = boardNumber / width;
		int col = boardNumber % height;
		if (flip) {
			state[0] = ""
					+ state[0].substring(0, row * width + col)
					+ (turn == BLACK ? 'X' : 'O')
					+ state[0].substring(row * width + col + 1,
							state[0].length());
		}
		char piece = 'P';
		row += addRow;
		col += addCol;
		int pieceCounter = 0;
		while (col >= 0 && col < width && row >= 0 && row < height) {
			piece = (flip ? state[0].charAt(row * width + col)
					: board[row][col].getPiece());
			if (piece == ' '
					|| (pieceCounter == 0 && piece == (turn == BLACK ? 'X'
							: 'O')))
				break;
			else {
				if (flip)
					state[0] = ""
							+ state[0].substring(0, row * width + col)
							+ (turn == BLACK ? 'X' : 'O')
							+ state[0].substring(row * width + col + 1,
									state[0].length());
				if (pieceCounter > 0 && piece == (turn == BLACK ? 'X' : 'O')) {
					if (flip) {
						for (int direct = 0; direct < 8; direct++) {
							if (direct != direction
									&& isFlippable(boardNumber, direct, false,
											null))
								isFlippable(boardNumber, direct, true, state);
						}
					}
					return true;
				}
				row += addRow;
				col += addCol;
				if (piece == (turn == BLACK ? 'O' : 'X'))
					pieceCounter++;
			}
		}
		return false;
	}

	@Override
	public String describe() {
		return width + "x" + height + " Reversi";
	}

	@Override
	public long recordStates() {
		if (conf.hasRemoteness) {
			return (boardSize - 3) * 3;
		} else
			return 3;
	}

	@Override
	public void longToRecord(TierState recordState, long record, Record toStore) {
		toStore.remoteness = (int) (record / 3);
		switch ((int) (record % 3)) {
		case 0:
			toStore.value = Value.LOSE;
			break;
		case 1:
			toStore.value = Value.WIN;
			break;
		case 2:
			toStore.value = Value.TIE;
			break;
		}
	}

	@Override
	public long recordToLong(TierState recordState, Record fromRecord) {
		int value;
		switch (fromRecord.value) {
		case LOSE:
			value = 0;
			break;
		case WIN:
			value = 1;
			break;
		case TIE:
			value = 2;
			break;
		default:
			throw new Error("Invalid value");
		}
		return fromRecord.remoteness * 3 + value;
	}

	public void changeTurn() {
		if (this.turn == BLACK)
			this.turn = WHITE;
		else if (this.turn == WHITE)
			this.turn = BLACK;
	}

	public String[] getStringMoves() {
		return stringMoves;
	}

	public static void main(String[] args) {
		Reversi reversiGame = null;
		try {
			reversiGame = new Reversi(new Configuration(args[0]));
		} catch (ClassNotFoundException c) {
			throw new Error(c);
		}
		String input;
		while (true) {
			System.out.println(reversiGame.displayState());
			System.out.println(reversiGame.primitiveValue().toString());
			TierState[] moves = new TierState[16];
			int y = reversiGame.validMoves(moves);
			System.out.print("Valid Moves: ");
			for (int x = 0; x < y; x++) {
				System.out.print(reversiGame.getStringMoves()[x] + ",");
			}
			System.out.println();
			System.out.print("Hashes: ");
			for (int x = 0; x < y; x++) {
				System.out.print(moves[x].hash + ",");
			}
			System.out.println();
			// System.out.println(reversiGame.turn);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			try {
				input = br.readLine();
			} catch (IOException e) {
				throw new Error(e);
			}
			int index = 0;
			for (index = 0; index < reversiGame.getStringMoves().length; index++)
				if (reversiGame.getStringMoves()[index].equals(input))
					break;
			if (index == reversiGame.getStringMoves().length) {
				System.out.println("Invalid Move");
				break;
			}
			reversiGame.setState(new TierState(reversiGame.getTier(), new Long(
					moves[index].hash)));
			reversiGame.changeTurn();
		}
	}

}
