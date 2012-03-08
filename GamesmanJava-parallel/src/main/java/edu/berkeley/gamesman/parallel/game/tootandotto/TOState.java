package edu.berkeley.gamesman.parallel.game.tootandotto;

import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.hasher.counting.CountingState;

/**
 * Similar to the C4State for connect 4, it holds the game as a list of pieces
 * in col-major order. The highest index in the array is the number of pieces on
 * the board. The second and third highest, respectively, are the number of Ts
 * and Os placed by player 1. The fourth and fifth are the number of Ts and Os
 * placed by player 2. The length of the array is gameSize + 5.
 * 
 * @author williamshen
 * 
 */
public class TOState extends CountingState {
	private final int width, height, boardSize;
	private final BitSetBoard myBoard; // TODO: does bitsetboard work here? it
										// seems to be designed for c4
	private int changePlace;

	public TOState(TOHasher myHasher, int width, int height) {
		super(myHasher, width * height);
		this.width = width;
		this.height = height;
		this.boardSize = width * height;
		changePlace = boardSize - 1;
		this.myBoard = new BitSetBoard(height, width); // TODO: does bitsetboard
														// work here? it seems
														// to be designed for c4
	}

}
