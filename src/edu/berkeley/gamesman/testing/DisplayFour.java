package edu.berkeley.gamesman.testing;

import javax.swing.*;
import java.awt.*;

public class DisplayFour extends JPanel implements Runnable {
	private final static long serialVersionUID = 2;
	private char[][] board;
	Slot[][] slots = new Slot[ConnectFour.WIDTH][ConnectFour.HEIGHT];

	public DisplayFour() {
		int columns, rows;
		setBackground(Color.YELLOW);
		setForeground(Color.YELLOW);
		setLayout(new GridLayout(ConnectFour.WIDTH, ConnectFour.HEIGHT));
		for (rows = ConnectFour.HEIGHT - 1; rows >= 0; rows--) {
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
		for (c = 0; c < ConnectFour.WIDTH; c++) {
			for (r = 0; r < ConnectFour.HEIGHT; r++) {
				slots[c][r].setChar(board[c][r]);
				slots[c][r].repaint();
			}
		}
	}
}