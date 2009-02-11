package edu.berkeley.gamesman.testing;


import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigInteger;
import java.util.Collection;

import javax.swing.JFrame;

import edu.berkeley.gamesman.Gamesman;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.game.Connect4;

public class ConnectFour implements MouseListener {
	char[][] board = new char[7][6];
	private int[] columnHeight = new int[7];
	private char turn = 'O';
	private boolean compO = false;
	private boolean compX = false;
	private boolean win = false;
	private Thread paintThread;
	private DisplayFour df;
	private Connect4 cgame = new Connect4();
	private BigInteger state;
	private FileDatabase fd;
	static int WIDTH = 4;
	static int HEIGHT = 4;

	public ConnectFour(DisplayFour disfour, FileDatabase db) {
		int c, r;
		fd = db;
		df = disfour;
		for (c = 0; c < WIDTH; c++) {
			for (r = 0; r < HEIGHT; r++) {
				df.slots[c][r].addMouseListener(this);
			}
		}
		paintThread = new Thread(df);
		for (c = 0; c < WIDTH; c++) {
			for (r = 0; r < HEIGHT; r++) {
				board[c][r] = ' ';
			}
			columnHeight[c] = 0;
		}
		startCompMove();
	}

	boolean compTurn() {
		return (turn == 'O' && compO) || (turn == 'X' && compX);
	}

	void makeMove(int move) {
		if (columnHeight[move] >= 6 || win())
			return;
		board[move][columnHeight[move]] = turn;
		df.setLast(move, columnHeight[move]);
		if (turn == 'O')
			turn = 'X';
		else
			turn = 'O';
		columnHeight[move]++;
		df.setBoard(copy(board));
		paintThread.start();
		paintThread = new Thread(df);
		state = cgame.stateToHash(board);
		if (!win())
			startCompMove();
	}

	private void startCompMove() {
		if (compTurn() && !win()) {
			Collection<char[][]> moves = cgame.validMoves(board);
			fd.getValue(state);
		}
	}

	char getTurn() {
		return turn;
	}

	private boolean win() {
		int column, row, i;
		boolean up, right, upright, downright;
		if (win)
			return true;
		for (column = 0; column < WIDTH; column++) {
			for (row = 0; row < HEIGHT; row++) {
				if (board[column][row] == ' ')
					break;
				up = row <= 2;
				right = column <= 3;
				upright = up && right;
				downright = row >= 3 && right;
				for (i = 0; i < 4; i++) {
					up = up && board[column][row + i] == board[column][row];
					right = right
							&& board[column + i][row] == board[column][row];
					upright = upright
							&& board[column + i][row + i] == board[column][row];
					downright = downright
							&& board[column + i][row - i] == board[column][row];
				}
				if (up || right || upright || downright) {
					if (board[column][row] == 'O')
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
		for (column = 0; column < WIDTH; column++) {
			if (columnHeight[column] < HEIGHT)
				return false;
		}
		return true;
	}

	private char[][] copy(char[][] b) {
		int c, r;
		char[][] rBoard = new char[b.length][];
		for (c = 0; c < b.length; c++) {
			rBoard[c] = new char[b[c].length];
			for (r = 0; r < b[c].length; r++) {
				rBoard[c][r] = b[c][r];
			}
		}
		return rBoard;
	}

	public void mouseClicked(MouseEvent me) {
	}

	public void mousePressed(MouseEvent me) {
	}

	public void mouseReleased(MouseEvent me) {
		if (me.getButton() == MouseEvent.BUTTON3) {
			df.toggleBlind();
			paintThread.start();
			paintThread = new Thread(df);
		} else {
			Slot o = (Slot) me.getSource();
			if (compTurn())
				return;
			makeMove(o.getCol());
		}
	}

	public void mouseEntered(MouseEvent me) {
	}

	public void mouseExited(MouseEvent me) {
	}

	public static void main(String[] args) throws InstantiationException, IllegalAccessException {
		Gamesman.main(new String[] { "-G", "Connect4", "-H",
			"PerfectConnect4Hash", "-D", "FileDatabase", "-u",
				"file:///tmp/test.db", "-gw", "4", "-gh", "4" });
		//Gamesman g = Gamesman.tempMakeGo();
		//FileDatabase fd = new FileDatabase();
		//fd.initialize("file:///tmp/test.db",new Configuration("14Connect4|4|4|44PC4H13{Value=(0.2)}"));
		FileDatabase fd = new FileDatabase();
		fd.initialize("file:///tmp/test.db",null);
		System.out.println(fd.getValue(BigInteger.ZERO));
		DisplayFour df = new DisplayFour();
		/* ConnectFour cf= */new ConnectFour(df, fd);
		JFrame jf = new JFrame();
		Container c = jf.getContentPane();
		c.add(df);
		jf.setSize(350, 300);
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jf.setVisible(true);
	}
}
