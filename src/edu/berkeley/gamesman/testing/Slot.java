package edu.berkeley.gamesman.testing;

import javax.swing.*;
import java.awt.*;

class Slot extends JComponent {
	private final static long serialVersionUID = 1;
	private char myChar = ' ';
	private int myCol, myRow;

	Slot(int c, int r) {
		myCol = c;
		myRow = r;
	}

	void setChar(char c) {
		myChar = c;
		repaint();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		int pieceWidth = getWidth() * 7 / 8;
		int pieceHeight = getHeight() * 7 / 8;
		g2.fillRect(0, 0, getWidth(), getHeight());
		switch (myChar) {
		case ' ':
			g2.setColor(Color.BLUE);
			break;
		case 'O':
			g2.setColor(Color.BLACK);
			break;
		case 'X':
			g2.setColor(Color.RED);
			break;
		}
		g2.fillOval(getWidth() / 2 - pieceWidth / 2, getHeight() / 2
				- pieceHeight / 2, pieceWidth, pieceHeight);
	}

	int getCol() {
		return myCol;
	}

	int getRow() {
		return myRow;
	}
}