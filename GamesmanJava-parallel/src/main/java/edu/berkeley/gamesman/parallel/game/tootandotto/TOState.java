package edu.berkeley.gamesman.parallel.game.tootandotto;

import edu.berkeley.gamesman.hasher.counting.CountingState;

/**
 * Similar to the C4State for connect 4, it holds the game as a list of pieces
 * in col-major order. The highest index in the array is the number of pieces on
 * the board. The second and third highest, respectively, are the number of Ts
 * and Os placed by player 1. The fourth and fifth are the number of Ts and Os
 * placed by player 2. The length of the array is gameSize + 5.
 * 
 * @author williamshen need to allow players to be associated with a name, eg
 *         player one is toot, or otto choice
 */
public class TOState extends CountingState {
	private final int width, height, boardSize;
	private int player1T, player2T, player1O, player2O; // remaining T's and O's
														// for the players
	private int piecesOnBoard;

	private int[][] boardRep;// col major order

	/*
	 * Initial state constructor
	 */
	public TOState(TOHasher myHasher, int width, int height) {
		super(myHasher, width * height);
		this.width = width;
		this.height = height;
		this.boardSize = width * height;
		this.boardRep = new int[height][width];


		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				this.boardRep[i][j] = 0;

	}

	/*
	 * Copies board from parent
	 */
	public TOState(TOHasher myHasher, int width, int height, TOState parentState) {
		super(myHasher, width * height);
		this.width = width;
		this.height = height;
		this.boardSize = width * height;
		this.boardRep = new int[height][width];

		// could probably do clone on parent...
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++)
				this.boardRep[i][j] = parentState.boardRep[i][j];

	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(width * height);
		for (int row = height - 1; row >= 0; row--) {
			sb.append('|');
			for (int col = 0; col < width; col++) {
				sb.append(getChar(row, col));
				sb.append('|');
			}
			sb.append('\n');
		}
		return sb.toString();
	}
	
	private char getChar(int row, int col) {
		int place = col * height + row;
		return place < getStart() ? '*' : getChar(place);
	}
	
	private char getChar(int place) {
		return TootAndOtto.charFor(get(place));
	}
}
