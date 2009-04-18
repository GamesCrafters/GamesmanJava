package edu.berkeley.gamesman.testing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import javax.swing.JFrame;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.ItergameState;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.game.RConnect4;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class ConnectFour implements MouseListener {
	final char[][] board;

	private int[] columnHeight = new int[7];

	private char turn = 'X';

	boolean compO;

	boolean compX;

	private boolean win = false;

	private Thread paintThread;

	private DisplayFour df;

	private RConnect4 cgame;

	Database fd;

	private static Random r = new Random();

	final int gameWidth;

	final int gameHeight;

	public ConnectFour(int height, int width, DisplayFour disfour, Database db) {
		this(height,width,disfour,db,false,true);
	}
	
	public ConnectFour(int height, int width, DisplayFour disfour, Database db, boolean cX, boolean cO) {
		int c, r;
		compX = cX;
		compO = cO;
		gameHeight = height;
		gameWidth = width;
		board = new char[gameHeight][gameWidth];
		fd = db;
		df = disfour;
		cgame = new RConnect4(db.getConfiguration());
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
		if (!win())
			startCompMove();
	}

	void startCompMove() {
		if (compTurn() && !win()) {
			ArrayList<Pair<String, ItergameState>> bests = new ArrayList<Pair<String, ItergameState>>(
					gameWidth);
			cgame.setToString(arrToString(board));
			Collection<Pair<String, ItergameState>> moves = cgame.validMoves();
			Pair<String, ItergameState> s;
			PrimitiveValue bestOutcome = null;
			PrimitiveValue thisOutcome;
			for (Pair<String, ItergameState> move : moves) {
				s = move;
				thisOutcome = fd.getRecord(cgame.stateToHash(s.cdr)).get()
						.previousMovesValue();
				System.out.println("Next possible move " + move.car
						+ " for state " + s.car + "," + s.cdr + " has value "
						+ thisOutcome);
				if (bests.size() == 0
						|| thisOutcome.isPreferableTo(bestOutcome)) {
					bestOutcome = thisOutcome;
					bests.clear();
					bests.add(s);
				} else if (!bestOutcome.isPreferableTo(thisOutcome))
					bests.add(s);
			}
			Pair<String, ItergameState> best = bests.get(r
					.nextInt(bests.size()));
			makeMove(best.car.charAt(0) - '0');
			System.out.println("Done with startCompMove");
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

	public static void main(String[] args) throws InstantiationException,
			IllegalAccessException {
		if (args.length != 1) {
			Util.fatalError("Please specify a jobfile as the only argument");
		}
		Configuration conf;
		try {
			conf = new Configuration(args[0]);
		} catch (ClassNotFoundException e) {
			Util.fatalError("failed to load class", e);
			return;
		}
		Database fd;
		try {
			fd = conf.openDatabase();
			int width = conf.getInteger("gamesman.game.width", 7);
			int height = conf.getInteger("gamesman.game.height", 6);
			System.out.println(fd.getRecord(BigInteger.ZERO));
			DisplayFour df = new DisplayFour(height, width);
			/* ConnectFour cf= */new ConnectFour(height, width, df, fd);
			JFrame jf = new JFrame();
			Container c = jf.getContentPane();
			c.add(df);
			jf.setSize(350, 300);
			jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			jf.setVisible(true);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public DisplayFour getDisplay() {
		return df;
	}
}
