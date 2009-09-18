package edu.berkeley.gamesman.testing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.game.Connect4;
import edu.berkeley.gamesman.util.Pair;

public class ConnectFour implements MouseListener {
	final char[][] board;

	private int[] columnHeight = new int[7];

	private char turn = 'X';

	boolean compO;

	boolean compX;

	private boolean win = false;

	private Thread paintThread;

	private DisplayFour df;

	private Connect4 cgame;

	private Configuration conf;

	Database fd;

	private static Random r = new Random();

	final int gameWidth;

	final int gameHeight;

	public ConnectFour(Configuration conf, DisplayFour disfour) {
		this(conf, disfour, false, true);
	}

	public ConnectFour(Configuration conf, DisplayFour disfour, boolean cX,
			boolean cO) {
		int c, r;
		this.conf = conf;
		compX = cX;
		compO = cO;
		gameHeight = Integer.parseInt(conf.getProperty("gamesman.game.height"));
		gameWidth = Integer.parseInt(conf.getProperty("gamesman.game.width"));
		board = new char[gameHeight][gameWidth];
		fd = conf.db;
		df = disfour;
		cgame = new Connect4(conf);
		cgame.prepare();
		for (c = 0; c < gameWidth; c++) {
			for (r = 0; r < gameHeight; r++) {
				df.slots[r][c].addMouseListener(this);
			}
		}
		paintThread = new Thread(df);
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
		paintThread.start();
		paintThread = new Thread(df);
		cgame.setFromString(arrToString(board));
		System.out.println(fd.getRecord(cgame.stateToHash(cgame.getState())));
		if (!win())
			new Thread() {
				public void run() {
					startCompMove();
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
			cgame.setFromString(arrToString(board));
			Collection<Pair<String, ItergameState>> moves = cgame.validMoves();
			ArrayList<Pair<String, ItergameState>> listMoves = new ArrayList<Pair<String, ItergameState>>(
					moves.size());
			listMoves.addAll(moves);
			ArrayList<Record> records = new ArrayList<Record>(moves.size());
			for (Pair<String, ItergameState> move : listMoves) {
				Record rec = fd.getRecord(cgame.stateToHash(move.cdr));
				rec.previousPosition();
				records.add(rec);
			}
			Record bestRecord = cgame.combine(records);
			ArrayList<Pair<String, ItergameState>> bestMoves = new ArrayList<Pair<String, ItergameState>>(
					listMoves.size());
			for (int i = 0; i < records.size(); i++) {
				if (records.get(i).equals(bestRecord))
					bestMoves.add(listMoves.get(i));
			}
			makeMove(bestMoves.get(r.nextInt(bestMoves.size())).car.charAt(0) - '0');
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
					paintThread.start();
					paintThread = new Thread(df);
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

	public DisplayFour getDisplay() {
		return df;
	}
}
