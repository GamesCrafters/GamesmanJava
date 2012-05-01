package edu.berkeley.gamesman.parallel.game.tootandotto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.game.type.GameRecord;
import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.counting.CountingState;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.parallel.GameRecordCombiner;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.parallel.ranges.Suffix;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.qll.QuickLinkedList;

/**
 * Similar to the C4State for connect 4, the CountingState for TootAndOtto holds
 * the game as a list of pieces in col-major order. The highest index in the
 * array is the number of pieces on the board. The second and third highest,
 * respectively, are the number of Ts and Os placed by player 1. The fourth and
 * fifth are the number of Ts and Os placed by player 2. The length of the array
 * is gameSize + 5.
 * 
 * @author williamshen
 */
public class TootAndOtto extends RangeTree<CountingState, GameRecord> implements
		SolveReader<CountingState, GameRecord> {
	private Move[] myMoves;
	private Move[][] colMoves;
	private TOHasher myHasher;
	private int width, height;
	private int gameSize;
	// maxPieces is the max number of T or Os a single player can play
	private int maxPieces;
	private int tootPlayer;
	private boolean misere;


	

	@Override
	public void rangeTreeConfigure(Configuration conf) {
		// Defaulting game size to the standard 6x4 unless set by conf file
		width = conf.getInt("gamesman.game.width", 6);
		height = conf.getInt("gamesman.game.height", 4);
		gameSize = width * height;

		// The number of pieces of each kind that each player gets (each gets 6
		// Ts and 6 Os by default)
		maxPieces = conf.getInt("gamesman.game.maxPieces", 6);
		myHasher = new TOHasher(width, height, maxPieces);

		int varianceLength = conf.getInt("gamesman.game.variance.length", 10);
		// gameSize + 5 is the length of the entire sequence (suffLen +
		// varianceLength)
		assert varianceLength <= gameSize;

		// defaults player one to be TOOT
		tootPlayer = conf.getInt("gamesman.game.tootPlayer", 1);

		ArrayList<Move>[] columnMoveList = new ArrayList[width];
		colMoves = new Move[width][];
		for (int i = 0; i < width; i++) {
			columnMoveList[i] = new ArrayList<Move>();
		}
		generateMoves(0, 0, 0, 0, 0, columnMoveList);
		ArrayList<Move> allMoves = new ArrayList<Move>();
		for (int i = 0; i < width; i++) {
			colMoves[i] = columnMoveList[i].toArray(new Move[columnMoveList[i]
					.size()]);
			allMoves.addAll(columnMoveList[i]);
		}
		myMoves = allMoves.toArray(new Move[allMoves.size()]);
		misere = conf.getBoolean("gamesman.game.misere", false);
	}

	/**
	 * generates moves that can be made from the current board configuration
	 * 
	 * @param numPieces
	 *            number of pieces on the board
	 * @param player1T
	 *            number of T for player 1
	 * @param player1O
	 *            number of O for player 1
	 * @param player2T
	 *            number of T for player 2
	 * @param player2O
	 *            number of O for player 2
	 * @param columnMoveList
	 *            the list of moves generated so far that we will append to
	 */
	private void generateMoves(int numPieces, int player1T, int player1O,
			int player2T, int player2O, ArrayList<Move>[] columnMoveList) {
		// figure out who is playing
		int turn = getTurn(numPieces);

		// Assume it is the first player's turn
		int TIndex = gameSize + 3;
		int OIndex = gameSize + 2;
		int numT = player1T;
		int numO = player1O;
		int numPiecesIndex = gameSize + 4;

		// If it is the second player's turn, set the info accordingly
		if (turn == 2) {
			TIndex -= 2;
			OIndex -= 2;
			numT = player2T;
			numO = player2O;
		}

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				int place = getPlace(row, col);
				if (numT < maxPieces) {
					if (isBottom(row, col)) {
						columnMoveList[col].add(new Move(place, 0, 1, TIndex,
								numT, numT + 1, numPiecesIndex, numPieces,
								numPieces + 1));
					} else {
						columnMoveList[col].add(new Move(place - 1, 1, 1,
								place, 0, 1, TIndex, numT, numT + 1,
								numPiecesIndex, numPieces, numPieces + 1));
						columnMoveList[col].add(new Move(place - 1, 2, 2,
								place, 0, 1, TIndex, numT, numT + 1,
								numPiecesIndex, numPieces, numPieces + 1));
					}
					if (turn == 1) {
						generateMoves(numPieces + 1, player1T + 1, player1O,
								player2T, player2O, columnMoveList);
					} else {
						generateMoves(numPieces + 1, player1T, player1O,
								player2T + 1, player2O, columnMoveList);
					}
				}
				if (numO < maxPieces) {
					if (isBottom(row, col)) {
						columnMoveList[col].add(new Move(place, 0, 2, OIndex,
								numO, numO + 1, numPiecesIndex, numPieces,
								numPieces + 1));
					} else {
						columnMoveList[col].add(new Move(place - 1, 1, 1,
								place, 0, 2, OIndex, numO, numO + 1,
								numPiecesIndex, numPieces, numPieces + 1));
						columnMoveList[col].add(new Move(place - 1, 2, 2,
								place, 0, 2, OIndex, numO, numO + 1,
								numPiecesIndex, numPieces, numPieces + 1));
					}
				}
				if (turn == 1) {
					generateMoves(numPieces + 1, player1T, player1O + 1,
							player2T, player2O, columnMoveList);
				} else {
					generateMoves(numPieces + 1, player1T, player1O, player2T,
							player2O + 1, columnMoveList);
				}
			}
		}
	}

	/**
	 * Determine if we are at the bottom of the column
	 * 
	 * @param row
	 * @param col
	 * @return true if the position (row, col) is at the bottom of a column
	 */
	private boolean isBottom(int row, int col) {
		return row == 0;
	}

	/**
	 * Translate the coordinate on the board into the nth place on the board in
	 * column major order
	 * 
	 * @param row
	 * @param col
	 * @return
	 */
	private int getPlace(int row, int col) {
		return col * height + row;
	}

	/**
	 * Determine whose turn it is, given the number of pieces on the board
	 * 
	 * @param numPieces
	 * @return 1 if it is the first player's turn, 2 if it is the second
	 *         player's turn.
	 */
	private int getTurn(int numPieces) {
		return (numPieces % 2) + 1;
	}

	/**
	 * returns the T index for specified player
	 * 
	 * @param player
	 * @return
	 */
	int getPlayerTIndex(int player) {
		switch (player) {
		case 1:
			return gameSize + 3;
		case 2:
			return gameSize + 1;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * returns the O index for specified player
	 * 
	 * @param player
	 * @return
	 */
	int getPlayerOIndex(int player) {
		switch (player) {
		case 1:
			return gameSize + 2;
		case 2:
			return gameSize;
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public CountingState getPosition(String board) {
		assert board.length() == gameSize + 4;
		int[] pos = new int[gameSize + 5];
		int pieceCount = 0;
		int i = 0;
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				char c = board.charAt(i++);
				pos[col * height + row] = pieceFor(c);
				if (c != ' ')
					pieceCount++;
			}
		}
		// Reading player's T and O information
		pos[gameSize] = board.charAt(gameSize);
		pos[gameSize + 1] = board.charAt(gameSize + 1);
		pos[gameSize + 2] = board.charAt(gameSize + 2);
		pos[gameSize + 3] = board.charAt(gameSize + 3);
		pos[gameSize + 4] = pieceCount;
		CountingState s = newState();
		getHasher().set(s, pos);
		return s;
	}

	private static int pieceFor(char c) {
		switch (c) {
		case ' ':
			return 0;
		case 'T':
			return 1;
		case 'O':
			return 2;
		default:
			throw new IllegalArgumentException();
		}
	}

	public CountingState newState() {
		return myHasher.newState();
	}

	@Override
	public Collection<Pair<String, CountingState>> getChildren(
			CountingState position) {
		ArrayList<Pair<String, CountingState>> children = new ArrayList<Pair<String, CountingState>>();
		int playerInTurn = getTurn(position);
		for (int col = 0; col < width; col++) {
			if (!isColumnFull(position, col)) {
				for (int row = 0; row < height; row++) {
					if (get(position, row, col) == 0) {
						int place = getPlace(row, col);
						int TIndex = getPlayerTIndex(playerInTurn);
						int OIndex = getPlayerOIndex(playerInTurn);
						int numT = position.get(TIndex);
						int numO = position.get(OIndex);
						int numPiecesIndex = gameSize + 4;
						int numPieces = position.get(numPiecesIndex);

						if (numT < maxPieces) {
							CountingState s = newState();
							getHasher().set(s, position);
							myHasher.makeMove(s, new Move(place, 0, 1, TIndex,
									numT, numT + 1, numPiecesIndex, numPieces,
									numPieces + 1));
							children.add(new Pair<String, CountingState>(
									Integer.toString(col), s));
						}
						if (numO < maxPieces) {
							CountingState s = newState();
							getHasher().set(s, position);
							myHasher.makeMove(s, new Move(place, 0, 1, OIndex,
									numO, numO + 1, numPiecesIndex, numPieces,
									numPieces + 1));
							children.add(new Pair<String, CountingState>(
									Integer.toString(col), s));
						}
						break;
					}
				}
			}
		}
		return children;
	}

	boolean isColumnFull(CountingState state, int col) {
		return get(state, col, height - 1) != 0;
	}

	@Override
	public String getString(CountingState position) {
		StringBuilder sb = new StringBuilder(gameSize + 4);
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				sb.append(charFor(position.get(col * height + row)));
			}
		}
		// Appending player's T and O count
		sb.append(position.get(gameSize));
		sb.append(position.get(gameSize + 1));
		sb.append(position.get(gameSize + 2));
		sb.append(position.get(gameSize + 3));

		return sb.toString();
	}

	/**
	 * Return the W/L/T value of the game for a given primitive state. If the
	 * state is not primitive, return null.
	 * 
	 * @param state
	 *            The (possibly) primitive position
	 * @return W/L/T or null (if not primitive)
	 */
	@Override
	public GameValue getValue(CountingState state) {
		int numPieces = state.get(gameSize + 4);
		int lastPlayed = getTurn(numPieces);
		TOEnum pattern = checkPattern(state);

		switch (pattern) {
		case NONE:
			if (numPieces == gameSize) {
				return GameValue.TIE;
			} else {
				return null;
			}
		case BOTH:
			return GameValue.TIE;
		default:
			if (pattern == getPattern(lastPlayed)) {
				return GameValue.LOSE;
			} else {
				return GameValue.WIN;
			}
		}
	}

	/**
	 * return the player's pattern
	 * 
	 * @param player
	 * @return
	 */
	private TOEnum getPattern(int player) {
		if (tootPlayer == player) {
			return TOEnum.TOOT;
		} else {
			return TOEnum.OTTO;
		}
	}

	/**
	 * given a state, return the pattern(s) detected in state
	 * 
	 * @param state
	 * @return
	 */
	protected TOEnum checkPattern(CountingState state) {
		boolean tootFound, ottoFound, horizontalPossible, downPossible;
		boolean verticalPossible;
		tootFound = false;
		ottoFound = false;
		horizontalPossible = false;// don't bother checking if it would run off
									// the board
		verticalPossible = false;// don't bother checking if it would run off
									// the board
		downPossible = false; // if either is false don't need to check diagonal
		// TODO Auto-generated method stub Will loop over stuff here
		for (int i = 0; i < gameSize; i++) {
			if (state.get(i) != 0)// if i isn't blank
			{
				verticalPossible = ((height - (i % height)) >= 3);
				horizontalPossible = ((width - (i / height)) >= 3);
				downPossible = ((i % height) >= 3);

			}
			if (state.get(i) == 2) {
				if (verticalPossible) {
					if ((state.get(i + 1)) == 1 && (state.get(i + 2)) == 1
							&& (state.get(i + 3)) == 2)
						ottoFound = true;
				}
				if (horizontalPossible) {
					if ((state.get(i + height)) == 1
							&& (state.get(i + 2 * height)) == 1
							&& (state.get(i + 3 * height)) == 2)
						ottoFound = true;
				}
				if (horizontalPossible && verticalPossible) {
					if ((state.get(i + 1 + height)) == 1
							&& (state.get(i + 2 * height + 2)) == 1
							&& (state.get(i + 3 * height + 3)) == 2)
						ottoFound = true;
				}
				if (horizontalPossible && downPossible) {
					if ((state.get(i + height - 1)) == 1
							&& (state.get(i + 2 * height - 2)) == 1
							&& (state.get(i + 3 * height - 3)) == 2)
						ottoFound = true;
				}
			} else if(state.get(i)==1){// starting with a 1 looking for toot
				if (verticalPossible) {
					if ((state.get(i + 1)) == 2 && (state.get(i + 2)) == 2
							&& (state.get(i + 3)) == 1)
						tootFound = true;
				}
				if (horizontalPossible) {
					if ((state.get(i + height)) == 2
							&& (state.get(i + 2 * height)) == 2
							&& (state.get(i + 3 * height)) == 1)
						tootFound = true;
				}
				if (horizontalPossible && verticalPossible) {
					if ((state.get(i + 1 + height)) == 2
							&& (state.get(i + 2 * height + 2)) == 2
							&& (state.get(i + 3 * height + 3)) == 1)
						tootFound = true;
				}
				if (horizontalPossible && downPossible) {
					if ((state.get(i + height - 1)) == 2
							&& (state.get(i + 2 * height - 2)) == 2
							&& (state.get(i + 3 * height - 3)) == 1)
						tootFound = true;
				}

			}
		}
		if (tootFound && ottoFound)
			return TOEnum.BOTH;
		if (tootFound)
			return TOEnum.TOOT;
		if (ottoFound)
			return TOEnum.OTTO;
		return TOEnum.NONE;
	}

	@Override
	public Collection<CountingState> getStartingPositions() {
		CountingState result = myHasher.newState();
		return Collections.singleton(result);
	}

	@Override
	public TOHasher getHasher() {
		return myHasher;
	}

	@Override
	protected Move[] getMoves() {
		return myMoves;
	}

	@Override
	protected boolean setNewRecordAndHasChildren(CountingState state,
			GameRecord rec) {
		GameValue val = getValue(state);
		if (val == null) {
			rec.set(GameValue.DRAW);
			return true;
		} else {
			if (val == GameValue.TIE)
				rec.set(GameValue.TIE, 0);
			else if (val == GameValue.LOSE)
				rec.set(GameValue.LOSE, 0);
			else if (val == GameValue.WIN)
				rec.set(GameValue.WIN, 0);
			else
				throw new RuntimeException("No other primitives");
			return false;
		}
	}

	@Override
	protected boolean combineValues(QuickLinkedList<GameRecord> grList,
			GameRecord gr) {
		return GameRecordCombiner.combineValues(grList, gr);
	}

	@Override
	protected void previousPosition(GameRecord gr, GameRecord toFill) {
		toFill.previousPosition(gr);
	}

	@Override
	protected Class<GameRecord> getGameRecordClass() {
		return GameRecord.class;
	}

	/**
	 * Returns the tier which is just the last element of the sequence.
	 */
	@Override
	public int getDivision(Suffix<CountingState> suff) {
		assert suff.length() == suffLen();
		return suff.get(suffLen() - 1);
	}

	static char charFor(int piece) {
		switch (piece) {
		case 0:
			return ' ';
		case 1:
			return 'T';
		case 2:
			return 'O';
		default:
			return '?';
		}
	}

	@Override
	public GameRecord getRecord(CountingState position, GameRecord fetchedRec) {
		return fetchedRec;
	}

	/**
	 * Returns expected number of pieces for a state (the high-index element of
	 * the state array)
	 * 
	 * @param state
	 *            The state
	 * @return The number of pieces/tier for this state
	 */
	int numPieces(CountingState state) {
		return state.get(gameSize + 4);
	}

	protected int maxVarianceLength() {
		return gameSize;
	}

	/**
	 * Returns whose turn it is for this state (for testing)
	 * 
	 * @param state
	 * @return
	 */
	int getTurn(CountingState state) {
		return getTurn(numPieces(state));
	}

	/**
	 * Given a state, fetches the piece at row-col (0 empty, 1 T, 2 O) (for
	 * testing)
	 * 
	 * @param state
	 * @param row
	 * @param col
	 * @return
	 */
	int get(CountingState state, int row, int col) {
		return state.get(col * height + row);
	}
}
