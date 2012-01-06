package edu.berkeley.gamesman.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.util.Pair;

/**
 * The internal board used for the testing class
 * 
 * @author dnspies
 */
@SuppressWarnings("javadoc")
class ConnectFour {
	private final char[][] board;

	private final TierGame game;

	private int[] columnHeight = new int[7];

	private char turn = 'X';

	private boolean compO;

	private boolean compX;

	private boolean win = false;

	private Database db;

	private static Random r = new Random();

	final int gameWidth;

	final int gameHeight;

	private Record nextRecord = null;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public ConnectFour(Configuration conf, Database db) {
		this(conf, db, false, true);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param disfour
	 *            The displayBoard
	 * @param cX
	 *            Does the computer play as Red?
	 * @param cO
	 *            Does the computer play as Black?
	 */
	public ConnectFour(Configuration conf, Database db, boolean cX, boolean cO) {
		game = (TierGame) conf.getGame();
		int c, r;
		compX = cX;
		compO = cO;
		gameHeight = Integer.parseInt(conf.getProperty("gamesman.game.height"));
		gameWidth = Integer.parseInt(conf.getProperty("gamesman.game.width"));
		board = new char[gameHeight][gameWidth];
		this.db = db;
		for (c = 0; c < gameWidth; c++) {
			for (r = 0; r < gameHeight; r++) {
				board[r][c] = ' ';
			}
			columnHeight[c] = 0;
		}
		startCompMove();
	}

	private boolean compTurn() {
		return (turn == 'O' && compO) || (turn == 'X' && compX);
	}

	void makeMove(int move) {
		if (columnHeight[move] >= gameHeight || win())
			return;
		board[columnHeight[move]][move] = turn;
		if (turn == 'O')
			turn = 'X';
		else
			turn = 'O';
		columnHeight[move]++;
		if (nextRecord != null) {
			System.out.println(nextRecord);
			nextRecord = null;
		} else {
			TierState state = game.stringToState(arrToString(board));
			Record r = game.newRecord();
			DatabaseHandle fdHandle = db.getHandle(true);
			try {
				game.longToRecord(state,
						db.readRecord(fdHandle, game.stateToHash(state)), r);
			} catch (IOException e) {
				throw new Error(e);
			}
			System.out.println(r);
		}
		if (!win())
			startCompMove();
	}

	void startCompMove() {
		if (compTurn() && !win()) {
			game.setFromString(arrToString(board));
			Collection<Pair<String, TierState>> moves = game.validMoves();
			ArrayList<Pair<String, TierState>> listMoves = new ArrayList<Pair<String, TierState>>(
					moves.size());
			listMoves.addAll(moves);
			long[] moveHashes = new long[listMoves.size()];
			Record[] records = new Record[listMoves.size()];
			DatabaseHandle fdHandle = db.getHandle(true);
			for (int i = 0; i < listMoves.size(); i++) {
				TierState state = listMoves.get(i).cdr;
				moveHashes[i] = game.stateToHash(state);
				records[i] = game.newRecord();
				try {
					game.longToRecord(state,
							db.readRecord(fdHandle, moveHashes[i]), records[i]);
				} catch (IOException e) {
					throw new Error(e);
				}
			}
			for (Record r : records)
				r.previousPosition();
			Record bestRecord = game.combine(records);
			ArrayList<Pair<String, TierState>> bestMoves = new ArrayList<Pair<String, TierState>>(
					listMoves.size());

			for (int i = 0; i < records.length; i++) {
				if (records[i].equals(bestRecord))
					bestMoves.add(listMoves.get(i));
			}
			Pair<String, TierState> chosenMove = bestMoves.get(r
					.nextInt(bestMoves.size()));
			nextRecord = bestRecord;
			nextRecord.nextPosition();
			makeMove(chosenMove.car.charAt(0) - '0');
		}
	}

	private String arrToString(char[][] c) {
		String s = "";
		for (int i = 0; i < c.length; i++)
			s += new String(c[i]);
		return s;
	}

	private boolean win() {
		int col, row, i;
		boolean up, right, upright, downright;
		if (win)
			return true;
		for (col = 0; col < gameWidth; col++) {
			for (row = 0; row < gameHeight; row++) {
				if (board[row][col] == ' ')
					break;
				up = row <= gameHeight - 4;
				right = col <= gameWidth - 4;
				upright = up && right;
				downright = row >= 3 && right;
				for (i = 0; i < 4; i++) {
					up = up && board[row + i][col] == board[row][col];
					right = right && board[row][col + i] == board[row][col];
					upright = upright
							&& board[row + i][col + i] == board[row][col];
					downright = downright
							&& board[row - i][col + i] == board[row][col];
				}
				if (up || right || upright || downright) {
					if (board[row][col] == 'O')
						System.out.println("Black wins");
					else
						System.out.println("Red wins");
					win = true;
					return true;
				}
			}
		}
		for (col = 0; col < gameWidth; col++) {
			if (columnHeight[col] < gameHeight)
				return false;
		}
		return true;
	}

	public void reset() {
		for (int col = 0; col < gameWidth; col++) {
			for (int row = 0; row < gameHeight; row++) {
				board[row][col] = ' ';
			}
			columnHeight[col] = 0;
		}
		win = false;
		startCompMove();
	}

	public char get(int row, int col) {
		return board[row][col];
	}

	public void setComp(boolean compX, boolean compO) {
		this.compX = compX;
		this.compO = compO;
	}

	public void closeDb() throws IOException {
		db.close();
	}
}
