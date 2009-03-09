package edu.berkeley.gamesman.testing;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JPanel;

public class DisplayFour extends JPanel implements Runnable {
	private final static long serialVersionUID = 2;
	private char[][] board;
	Slot[][] slots = new Slot[ConnectFour.HEIGHT][ConnectFour.WIDTH];

	public DisplayFour() {
		int col, row;
		setBackground(Color.YELLOW);
		setForeground(Color.YELLOW);
		setLayout(new GridLayout(ConnectFour.HEIGHT, ConnectFour.WIDTH));
		for (row = ConnectFour.HEIGHT - 1; row >= 0; row--) {
			for (col = 0; col < ConnectFour.WIDTH; col++) {
				slots[row][col] = new Slot(row,col);
				add(slots[row][col]);
			}
		}
	}

	void setBoard(char[][] board) {
		this.board = board;
	}

	public synchronized void run() {
		int c, r;
		for (c = 0; c < ConnectFour.WIDTH; c++) {
			for (r = 0; r < ConnectFour.HEIGHT; r++) {
				slots[r][c].setChar(board[r][c]);
				slots[r][c].repaint();
			}
		}
	}
}