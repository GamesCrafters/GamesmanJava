package edu.berkeley.gamesman.testing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.game.TopDownC4;
import edu.berkeley.gamesman.game.util.C4State;
import edu.berkeley.gamesman.game.util.ItergameState;
import edu.berkeley.gamesman.util.Pair;

/**
 * The internal board used for the testing class
 * 
 * @author dnspies
 */
class ConnectFour implements MouseListener {
	final char[][] board;

	private final Connect4 game;

	private final boolean topDown;

	private final TopDownC4 tdgame;

	private int[] columnHeight = new int[7];

	private char turn = 'X';

	boolean compO;

	boolean compX;

	private boolean win = false;

	private DisplayFour df;

	Database fd;

	private static Random r = new Random();

	final int gameWidth;

	final int gameHeight;

	private Record nextRecord = null;

	/**
	 * @param conf
	 *            The configuration object
	 * @param disfour
	 *            The display board
	 */
	public ConnectFour(Configuration conf, DisplayFour disfour) {
		this(conf, disfour, false, true);
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
	public ConnectFour(Configuration conf, DisplayFour disfour, boolean cX,
			boolean cO) {
		topDown = conf.getProperty("gamesman.game").contains("TopDownC4");
		if (topDown) {
			game = null;
			tdgame = (TopDownC4) conf.getGame();
		} else {
			tdgame = null;
			game = (Connect4) conf.getGame();
		}
		int c, r;
		compX = cX;
		compO = cO;
		gameHeight = Integer.parseInt(conf.getProperty("gamesman.game.height"));
		gameWidth = Integer.parseInt(conf.getProperty("gamesman.game.width"));
		board = new char[gameHeight][gameWidth];
		fd = conf.db;
		df = disfour;
		for (c = 0; c < gameWidth; c++) {
			for (r = 0; r < gameHeight; r++) {
				df.slots[r][c].addMouseListener(this);
			}
		}
		for (c = 0; c < gameWidth; c++) {
			for (r = 0; r < gameHeight; r++) {
				board[r][c] = ' ';
			}
			columnHeight[c] = 0;
		}
		df.setBoard(copy(board));
		startCompMove();
	}

	boolean compTurn() {
		return (turn == 'O' && compO) || (turn == 'X' && compX);
	}

	void makeMove(int move) {
		if (columnHeight[move] >= 6 || win())
			return;
		board[columnHeight[move]][move] = turn;
		if (turn == 'O')
			turn = 'X';
		else
			turn = 'O';
		columnHeight[move]++;
		df.setBoard(copy(board));
		df.paintBoard();
		new Thread() {
			public void run() {
				if (nextRecord != null) {
					System.out.println(nextRecord);
					nextRecord = null;
				} else if (topDown) {
					tdgame.setFromString(arrToString(board));
					System.out.println(fd.getRecord(tdgame.stateToHash(tdgame
							.getState())));
				} else {
					game.setFromString(arrToString(board));
					System.out.println(fd.getRecord(game.stateToHash(game
							.getState())));
				}
				if (!win())
					new Thread() {
						public void run() {
							startCompMove();
						}
					}.start();
			}
		}.start();

	}

	synchronized void startCompMove() {
		if (compTurn() && !win()) {
			if (compO && compX)
				try {
					wait(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			if (topDown) {
				tdgame.setFromString(arrToString(board));
				Collection<Pair<String, C4State>> moves = tdgame.validMoves();
				ArrayList<Pair<String, C4State>> listMoves = new ArrayList<Pair<String, C4State>>(
						moves.size());
				listMoves.addAll(moves);
				long[] moveHashes = new long[listMoves.size()];
				Record[] records = new Record[listMoves.size()];
				for (int i = 0; i < listMoves.size(); i++) {
					C4State state = listMoves.get(i).cdr;
					moveHashes[i] = tdgame.stateToHash(state);
					if (tdgame.getState().numPieces != state.numPieces)
						tdgame.setNumPieces(state.numPieces);
					records[i] = fd.getRecord(moveHashes[i]);
				}
				for (Record r : records)
					r.previousPosition();
				Record bestRecord = tdgame.combine(records, 0, records.length);
				ArrayList<Pair<String, C4State>> bestMoves = new ArrayList<Pair<String, C4State>>(
						listMoves.size());

				for (int i = 0; i < records.length; i++) {
					if (records[i].equals(bestRecord))
						bestMoves.add(listMoves.get(i));
				}
				Pair<String, C4State> chosenMove = bestMoves.get(r
						.nextInt(bestMoves.size()));
				nextRecord = bestRecord;
				nextRecord.nextPosition();
				makeMove(chosenMove.car.charAt(0) - '0');
			} else {
				game.setFromString(arrToString(board));
				Collection<Pair<String, ItergameState>> moves = game
						.validMoves();
				ArrayList<Pair<String, ItergameState>> listMoves = new ArrayList<Pair<String, ItergameState>>(
						moves.size());
				listMoves.addAll(moves);
				long[] moveHashes = new long[listMoves.size()];
				Record[] records = new Record[listMoves.size()];
				for (int i = 0; i < listMoves.size(); i++) {
					ItergameState state = listMoves.get(i).cdr;
					moveHashes[i] = game.stateToHash(state);
					if (game.getTier() != state.tier)
						game.setTier(state.tier);
					records[i] = fd.getRecord(moveHashes[i]);
				}
				for (Record r : records)
					r.previousPosition();
				Record bestRecord = game.combine(records, 0, records.length);
				ArrayList<Pair<String, ItergameState>> bestMoves = new ArrayList<Pair<String, ItergameState>>(
						listMoves.size());

				for (int i = 0; i < records.length; i++) {
					if (records[i].equals(bestRecord))
						bestMoves.add(listMoves.get(i));
				}
				Pair<String, ItergameState> chosenMove = bestMoves.get(r
						.nextInt(bestMoves.size()));
				nextRecord = bestRecord;
				nextRecord.nextPosition();
				makeMove(chosenMove.car.charAt(0) - '0');
			}
		}
	}

	private String arrToString(char[][] c) {
		String s = "";
		for (int i = 0; i < c.length; i++)
			s += new String(c[i]);
		return s;
	}

	char getTurn() {
		return turn;
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
					df.paintBoard();
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

	private char[][] copy(char[][] b) {
		int c, r;
		char[][] rBoard = new char[b.length][];
		for (r = 0; r < b.length; r++) {
			rBoard[r] = new char[b[r].length];
			for (c = 0; c < b[r].length; c++) {
				rBoard[r][c] = b[r][c];
			}
		}
		return rBoard;
	}

	public void mouseClicked(MouseEvent me) {
	}

	public void mousePressed(MouseEvent me) {
	}

	public void mouseReleased(MouseEvent me) {
		Slot o = (Slot) me.getSource();
		if (compTurn())
			return;
		makeMove(o.getCol());
	}

	public void mouseEntered(MouseEvent me) {
	}

	public void mouseExited(MouseEvent me) {
	}

	/**
	 * @return The displayed board
	 */
	public DisplayFour getDisplay() {
		return df;
	}
}
