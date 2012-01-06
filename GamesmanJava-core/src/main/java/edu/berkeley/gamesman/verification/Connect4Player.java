package edu.berkeley.gamesman.verification;

/**
 * Specifies a player to keep track of whose turn it is.
 * 
 * @author adegtiar
 * @author rchengyue
 */
public enum Connect4Player implements Player {
	X, O;

	@Override
	public Connect4Player getOppositePlayer() {
		if (this == X)
			return O;
		else
			return X;
	}

	public Connect4Piece getPiece() {
		if (this == X)
			return Connect4Piece.O;
		else
			return Connect4Piece.X;
	}
}
