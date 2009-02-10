package edu.berkeley.gamesman.testing;

import javax.swing.*;
import java.awt.*;

public class DisplayFour extends JPanel implements Runnable {
	private final static long serialVersionUID = 2;
	static boolean BLIND = false;
	private char[][] board;
	Slot[][] slots = new Slot[ConnectFour.WIDTH][ConnectFour.HEIGHT];
	private int lastCol = -1;
	private int lastRow = -1;

	public DisplayFour() {
		int columns, rows;
		setBackground(Color.YELLOW);
		setForeground(Color.YELLOW);
		setLayout(new GridLayout(ConnectFour.WIDTH, ConnectFour.HEIGHT));
		for (rows = ConnectFour.HEIGHT-1; rows >= 0; rows--) {
			for (columns = 0; columns < ConnectFour.WIDTH; columns++) {
				slots[columns][rows] = new Slot(columns, rows);
				add(slots[columns][rows]);
			}
		}
	}

	void setBoard(char[][] board) {
		this.board = board;
	}

	public synchronized void run() {
		int c, r;
		if (BLIND) {
			c = lastCol;
			r = lastRow;
			if (!(c == -1 || r == -1)) {
				slots[c][r].setChar(board[c][r]);
				slots[c][r].repaint();
			}
			try {
				wait(500);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			if (!(c == -1 || r == -1)) {
				slots[c][r].setChar(' ');
				slots[c][r].repaint();
			}
		} else
			for (c = 0; c < ConnectFour.WIDTH; c++) {
				for (r = 0; r < ConnectFour.HEIGHT; r++) {
					slots[c][r].setChar(board[c][r]);
					slots[c][r].repaint();
				}
			}
	}

	void setLast(int x, int y) {
		lastCol = x;
		lastRow = y;
	}

	public void toggleBlind() {
		BLIND = !BLIND;
		if (BLIND == true) {
			for (int c = 0; c < 7; c++)
				for (int r = 0; r < 6; r++)
					slots[c][r].setChar(' ');
		}
	}

}