package edu.berkeley.gamesman.game.util;

public class Bullet {

	public Bullet(int row, int col, int dir_num, char owner) {
		this.row = row;
		this.col = col;
		this.owner = owner;
		switch (dir_num) {
		case (0):
			dir = 'n';
			break;
		case (1):
			dir = 'w';
			break;
		case (2):
			dir = 'e';
			break;
		case (3):
			dir = 's';
			break;
		}
	}

	public Bullet set(int row, int col, int dir_num, char owner) {
		this.row = row;
		this.col = col;
		this.owner = owner;
		switch (dir_num) {
		case (0):
			dir = 'n';
			break;
		case (1):
			dir = 'w';
			break;
		case (2):
			dir = 'e';
			break;
		case (3):
			dir = 's';
			break;
		}
		return this;
	}

	public char owner() {
		return owner;
	}

	public int row() {
		return row;
	}

	public int col() {
		return col;
	}

	public int drow() {
		switch (dir) {
		case ('n'):
			return -1;
		case ('w'):
			return 0;
		case ('e'):
			return 0;
		case ('s'):
			return 1;
		default:
			throw new IllegalArgumentException("bad direction " + dir);
		}
	}

	public int dcol() {
		switch (dir) {
		case ('n'):
			return 0;
		case ('w'):
			return -1;
		case ('e'):
			return 1;
		case ('s'):
			return 0;
		default:
			throw new IllegalArgumentException("bad direction " + dir);
		}
	}

	private int row;
	private int col;
	private char dir; //
	private char owner;
}
