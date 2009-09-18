package edu.berkeley.gamesman.testing;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JPanel;

class DisplayFour extends JPanel implements Runnable {
	private final static long serialVersionUID = 2;
	private char[][] board;
	private final int gameHeight;
	private final int gameWidth;
	Slot[][] slots;

	DisplayFour(int height, int width) {
		int col, row;
		gameHeight = height;
		gameWidth = width;
		slots = new Slot[gameHeight][gameWidth];
		setBackground(Color.YELLOW);
		setForeground(Color.YELLOW);
		setLayout(new GridLayout(gameHeight, gameWidth));
		for (row = gameHeight - 1; row >= 0; row--) {
			for (col = 0; col < gameWidth; col++) {
				slots[row][col] = new Slot(row,col);
				add(slots[row][col]);
			}
		}
	}

	void setBoard(char[][] board) {
		this.board = board;
		new Thread(this).start();
	}

	public synchronized void run() {
		int c, r;
		for (c = 0; c < gameWidth; c++) {
			for (r = 0; r < gameHeight; r++) {
				slots[r][c].setChar(board[r][c]);
				slots[r][c].repaint();
			}
		}
	}
}