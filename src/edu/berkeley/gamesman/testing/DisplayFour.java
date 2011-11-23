package edu.berkeley.gamesman.testing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

@SuppressWarnings("javadoc")
class DisplayFour extends JPanel {
	private final static long serialVersionUID = 2;
	private final int gameHeight;
	private final int gameWidth;
	private final Slot[][] slots;
	private final ConnectFour c4;

	DisplayFour(ConnectFour c4) {
		int col, row;
		gameHeight = c4.gameHeight;
		gameWidth = c4.gameWidth;
		this.c4 = c4;
		slots = new Slot[gameHeight][gameWidth];
		setBackground(Color.YELLOW);
		setForeground(Color.YELLOW);
		setLayout(new GridLayout(gameHeight, gameWidth));
		for (row = gameHeight - 1; row >= 0; row--) {
			for (col = 0; col < gameWidth; col++) {
				slots[row][col] = new Slot(row, col);
				add(slots[row][col]);
			}
		}
	}

	class Slot extends JComponent implements MouseListener {
		private final static long serialVersionUID = 1;
		private int myCol, myRow;

		Slot(int r, int c) {
			myCol = c;
			myRow = r;
			addMouseListener(this);
		}

		public void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			int pieceWidth = getWidth() * 7 / 8;
			int pieceHeight = getHeight() * 7 / 8;
			g2.fillRect(0, 0, getWidth(), getHeight());
			switch (c4.get(myRow, myCol)) {
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

		@Override
		public void mouseClicked(MouseEvent e) {
			c4.makeMove(myCol);
			DisplayFour.this.repaint();
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}
	}
}