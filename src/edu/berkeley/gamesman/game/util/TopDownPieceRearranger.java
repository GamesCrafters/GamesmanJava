package edu.berkeley.gamesman.game.util;

import edu.berkeley.gamesman.util.QuickLinkedList;
import edu.berkeley.gamesman.util.Util;

public class TopDownPieceRearranger {
	private static class Piece {
		long hash;

		int index, numOs;

		char player;
	}

	private long hash;

	private final Piece afterPiece;

	private final QuickLinkedList<Piece> pieces;

	private final QuickLinkedList<Piece>.QuickLinkedIterator pieceIterator;

	public TopDownPieceRearranger(int numSpaces) {
		Piece[] pieceArray = new Piece[numSpaces];
		QuickLinkedList<Piece> myPieces = null;
		try {
			myPieces = new QuickLinkedList<Piece>(pieceArray, Piece.class
					.getConstructor());
		} catch (SecurityException e) {
			Util.fatalError("Cannot access constructor", e);
		} catch (NoSuchMethodException e) {
			Util.fatalError("No empty constructor", e);
		}
		pieces = myPieces;
		pieceIterator = pieces.listIterator();
		hash = 0;
		afterPiece = new Piece();
		afterPiece.index = 0;
		afterPiece.numOs = 0;
		afterPiece.hash = 1;
		afterPiece.player = 'X';
	}

	public int changeMove(int serial, int stepsBack) {
		pieceIterator.jumpSerial(serial);
		Piece move = pieceIterator.next();
		pieceIterator.remove();
		Piece p = null;
		for (int i = 0; i < stepsBack; i++) {
			p = pieceIterator.previous();
			++p.index;
			long hashDiff;
			if (move.player == 'O') {
				++p.numOs;
				hashDiff = p.hash * p.index / p.numOs - p.hash;
				hash += hashDiff;
			} else
				hashDiff = p.hash * p.index / (p.index - p.numOs) - p.hash;
			p.hash += hashDiff;
		}
		if (p == null) {
			if (pieceIterator.hasNext()) {
				p = pieceIterator.next();
				pieceIterator.previous();
			} else
				p = afterPiece;
		}
		Piece newMove = pieceIterator.add();
		newMove.player = move.player;
		newMove.index = p.index - 1;
		if (p.player == 'O') {
			newMove.numOs = p.numOs - 1;
			newMove.hash = p.hash * p.numOs / p.index;
		} else {
			newMove.numOs = p.numOs;
			newMove.hash = p.hash * (p.index - p.numOs) / p.index;
		}
		if (move.player == 'O')
			hash += newMove.hash - move.hash;
		pieceIterator.previous();
		return pieceIterator.nextSerial();
	}

	public int makeMove(char player, int stepsBack) {
		pieceIterator.reset(true);
		Piece p = afterPiece;
		for (int i = 0; i <= stepsBack; i++) {
			++p.index;
			long hashDiff;
			if (player == 'O') {
				++p.numOs;
				hashDiff = p.hash * p.index / p.numOs - p.hash;
				hash += hashDiff;
			} else
				hashDiff = p.hash * p.index / (p.index - p.numOs) - p.hash;
			p.hash += hashDiff;
			if (i < stepsBack)
				p = pieceIterator.previous();
		}
		Piece newMove = pieceIterator.add();
		newMove.player = player;
		newMove.index = p.index - 1;
		if (p.player == 'O') {
			newMove.numOs = p.numOs - 1;
			newMove.hash = p.hash * p.numOs / p.index;
		} else {
			newMove.numOs = p.numOs;
			newMove.hash = p.hash * (p.index - p.numOs) / p.index;
		}
		if (player == 'O')
			hash += newMove.hash;
		pieceIterator.previous();
		return pieceIterator.nextSerial();
	}

	public void undoMove(int serial) {
		pieceIterator.jumpSerial(serial);
		Piece move = pieceIterator.next();
		pieceIterator.remove();
		if (move.player == 'O')
			hash -= move.hash;
		Piece p = null;
		while (pieceIterator.hasNext()) {
			p = pieceIterator.next();
			long hashDiff;
			if (move.player == 'O') {
				hashDiff = p.hash - p.hash * p.numOs / p.index;
				--p.numOs;
				hash -= hashDiff;
			} else
				hashDiff = p.hash - p.hash * (p.index - p.numOs) / p.index;
			--p.index;
			p.hash -= hashDiff;
		}
	}
}
