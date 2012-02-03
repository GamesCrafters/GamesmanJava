package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.util.BitSetBoard;
import edu.berkeley.gamesman.game.util.TierState;
import edu.berkeley.gamesman.hasher.ChangedIterator;

public class TicTacToe extends RectangularDartboardGame {
	private final BitSetBoard bsb;
	private final int piecesToWin;
	private final ChangedIterator myIter = new ChangedIterator();

	public TicTacToe(Configuration conf) {
		super(conf, LAST_MOVE_TIE);
		bsb = new BitSetBoard(gameHeight, gameWidth);
		piecesToWin = conf.getInteger("gamesman.game.pieces", 3);
	}

	@Override
	public Value primitiveValue() {
		return bsb.xInALine(piecesToWin, oppositeTurn()) ? Value.LOSE
				: (getTier() == gameSize ? Value.TIE : Value.UNDECIDED);
	}

	private char oppositeTurn() {
		switch (getTurn()) {
		case 'X':
			return 'O';
		case 'O':
			return 'X';
		default:
			throw new Error("This should not happen");
		}
	}

	private void setBSB() {
		bsb.clear();
		for (int row = 0; row < gameHeight; row++) {
			for (int col = 0; col < gameWidth; col++) {
				if (get(row, col) != ' ')
					bsb.addPiece(row, col, get(row, col));
			}
		}
	}

	@Override
	public String describe() {
		return "Tic Tac Toe " + gameWidth + "x" + gameHeight;
	}

	@Override
	public void setState(TierState pos) {
		super.setState(pos);
		setBSB();
	}

	@Override
	public void setFromString(String pos) {
		super.setFromString(pos);
		setBSB();
	}

	@Override
	protected void setTier(int tier) {
		super.setTier(tier);
		setBSB();
	}

	@Override
	public void setStartingPosition(int n) {
		super.setStartingPosition(n);
		setBSB();
	}

	@Override
	public void nextHashInTier() {
		myHasher.next(myIter);
		while (myIter.hasNext()) {
			int piece = myIter.next();
			int row = piece / gameWidth;
			int col = piece % gameWidth;
			bsb.removePiece(row, col);
			if (myHasher.get(piece) != ' ') {
				bsb.addPiece(row, col, myHasher.get(piece));
			}
		}
	}
}
