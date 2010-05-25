package edu.berkeley.gamesman.game.util;

import edu.berkeley.gamesman.util.*;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Node;
import edu.berkeley.gamesman.util.qll.RecycleLinkedList;

/**
 * A binary rearranger which is structured to match the format of top-down
 * solves (No next method and allowing multiple insertions and removals)
 * 
 * @author dnspies
 */
public final class TopDownPieceRearranger {
	public final static class Piece {
		private long hash;

		private int index, numOs;

		private char player;

		public String toString() {
			return String.valueOf(player);
		}
	}

	private long hash;

	private final Piece afterPiece;

	private final Piece beforePiece;

	private final RecycleLinkedList<Piece> pieces;

	private final RecycleLinkedList<Piece>.RLLIterator pieceIterator;

	private final TDPRIter myCharIter;

	private final class TDPRIter implements CharIterator {

		private final RecycleLinkedList<Piece>.RLLIterator myIterator = pieces
				.listIterator();

		public boolean hasNext() {
			return myIterator.hasNext();
		}

		public char next() {
			return myIterator.next().player;
		}

		public void reset() {
			myIterator.toIndex(0);
		}

	}

	/**
	 * @param numSpaces
	 *            The number of empty spaces on the board (it is presumed the
	 *            game begins with no pieces on the board)
	 */
	public TopDownPieceRearranger(int numSpaces) {
		RecycleLinkedList<Piece> myPieces = null;
		myPieces = new RecycleLinkedList<Piece>(new Factory<Piece>() {
			public Piece newObject() {
				return new Piece();
			}

			public void reset(Piece t) {
			}
		});
		pieces = myPieces;
		pieceIterator = pieces.listIterator();
		hash = 0L;
		beforePiece = new Piece();
		beforePiece.player = 'O';
		beforePiece.index = -1;
		beforePiece.numOs = 0;
		beforePiece.hash = 0;
		afterPiece = new Piece();
		afterPiece.index = 0;
		afterPiece.numOs = 0;
		afterPiece.hash = 1;
		afterPiece.player = 'X';
		myCharIter = new TDPRIter();
	}

	/**
	 * Moves a piece at the given serial back a certain number of steps
	 * 
	 * @param serial
	 *            The serial of the piece to be moved
	 * @param stepsBack
	 *            The number of steps back you wish to move it
	 * @return The new serial of the piece being moved
	 */
	public Node<Piece> changeMove(Node<Piece> serial, int stepsBack) {
		pieceIterator.toNode(serial);
		Piece move = pieceIterator.next();
		char player = move.player;
		if (player == 'O')
			hash -= move.hash;
		pieceIterator.remove();
		Piece p = null;
		for (int i = 0; i < stepsBack; i++) {
			p = pieceIterator.previous();
			++p.index;
			long hashDiff;
			if (player == 'O') {
				++p.numOs;
				if (p.hash == 0L)
					hashDiff = 0L;
				else
					hashDiff = p.hash * p.index / p.numOs - p.hash;
			} else {
				if (p.hash == 0L)
					hashDiff = 1L;
				else
					hashDiff = p.hash * p.index / (p.index - p.numOs) - p.hash;
			}
			p.hash += hashDiff;
			if (p.player == 'O')
				hash += hashDiff;
		}
		if (p == null) {
			if (pieceIterator.hasNext()) {
				p = pieceIterator.next();
				pieceIterator.previous();
			} else
				p = afterPiece;
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

	// private void checkHash() {
	// pieceIterator.reset(false);
	// int countPieces = 0, numOs = 0;
	// long sumHash = 0L;
	// while (pieceIterator.hasNext()) {
	// Piece p = pieceIterator.next();
	// if (p.player == 'O') {
	// ++numOs;
	// sumHash += p.hash;
	// }
	// if (p.hash != Util.nCr(countPieces, numOs))
	// throw new RuntimeException("Hash doesn't match");
	// ++countPieces;
	// }
	// if (beforePiece.hash != 0
	// || afterPiece.hash != Util.nCr(countPieces, numOs))
	// throw new RuntimeException("Before or After hash doesn't match");
	// if (sumHash != hash)
	// throw new RuntimeException("Hash doesn't match");
	// }

	/**
	 * Adds a piece to the arrangement stepsBack steps from the end of the
	 * arrangement
	 * 
	 * @param player
	 *            The player of the added piece
	 * @param stepsBack
	 *            The number of steps from the end
	 * @return The serial of the added piece
	 */
	public Node<Piece> makeMove(char player, int stepsBack) {

		pieceIterator.toIndex(pieces.size());
		Piece p = afterPiece;
		for (int i = 0; i <= stepsBack; i++) {
			++p.index;
			long hashDiff;
			if (player == 'O') {
				++p.numOs;
				if (p.hash == 0L)
					hashDiff = 0L;
				else
					hashDiff = p.hash * p.index / p.numOs - p.hash;
			} else {
				if (p.hash == 0L)
					hashDiff = 1L;
				else
					hashDiff = p.hash * p.index / (p.index - p.numOs) - p.hash;
			}
			p.hash += hashDiff;
			if (p.player == 'O')
				hash += hashDiff;
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

		Node<Piece> serial = pieceIterator.nextSerial();
		return serial;
	}

	/**
	 * Removes the passed piece from the arrangement
	 * 
	 * @param serial
	 *            The serial of the piece to remove
	 */
	public void undoMove(Node<Piece> serial) {

		pieceIterator.toNode(serial);
		Piece move = pieceIterator.next();
		char player = move.player;
		if (player == 'O')
			hash -= move.hash;
		pieceIterator.remove();
		Piece p;
		while (pieceIterator.hasNext()) {
			p = pieceIterator.next();
			long hashDiff;
			if (player == 'O') {
				hashDiff = p.hash - p.hash * p.numOs / p.index;
				--p.numOs;
			} else
				hashDiff = p.hash - p.hash * (p.index - p.numOs) / p.index;
			--p.index;
			p.hash -= hashDiff;
			if (p.player == 'O')
				hash -= hashDiff;
		}
		if (player == 'O') {
			afterPiece.hash = afterPiece.hash * afterPiece.numOs
					/ afterPiece.index;
			--afterPiece.numOs;
		} else
			afterPiece.hash = afterPiece.hash
					* (afterPiece.index - afterPiece.numOs) / afterPiece.index;
		--afterPiece.index;

	}

	/**
	 * @return The hash of this arrangement
	 */
	public long getHash() {
		return hash;
	}

	public String toString() {
		return pieces.toString();
	}

	/**
	 * @param numPieces
	 *            The number of pieces present
	 * @param numOs
	 *            The number of those pieces which are O's
	 * @param pieceArrangement
	 *            The arrangement hash of the pieces
	 */
	public void setArrangement(int numPieces, int numOs, long pieceArrangement) {
		pieces.clear();
		hash = pieceArrangement;
		Piece lastPiece;
		Piece p = afterPiece;
		int remainingOs = numOs;
		p.index = numPieces;
		p.numOs = remainingOs;
		p.hash = Util.nCr(numPieces, numOs);
		for (int i = numPieces - 1; i >= 0; i--) {
			lastPiece = p;
			p = pieces.addFirst();
			if (lastPiece.player == 'O') {
				p.hash = lastPiece.hash * lastPiece.numOs / lastPiece.index;
			} else {
				p.hash = lastPiece.hash * (lastPiece.index - lastPiece.numOs)
						/ lastPiece.index;
			}
			p.index = i;
			p.numOs = remainingOs;
			if (remainingOs > 0 && pieceArrangement >= p.hash) {
				p.player = 'O';
				--remainingOs;
				pieceArrangement -= p.hash;
			} else
				p.player = 'X';
		}
		pieceIterator.toIndex(0);
	}

	/**
	 * @return An iterator over the players of all the pieces from left to right
	 */
	public CharIterator getCharIterator() {
		myCharIter.reset();
		return myCharIter;
	}

	/**
	 * Sets this rearranger to match the passed arrangement
	 * 
	 * @param arrangement
	 *            A string representing the arrangement of pieces
	 */
	public void setArrangement(String arrangement) {
		pieces.clear();
		char[] arrangePos = arrangement.toCharArray();
		Piece p = beforePiece, lastPiece;
		int numOs = 0;
		hash = 0L;
		int numPieces = arrangePos.length;
		for (int i = 0; i < numPieces; i++) {
			lastPiece = p;
			p = pieces.addLast();
			p.player = arrangePos[i];
			p.index = i;
			if (p.player == 'O') {
				++numOs;
				if (lastPiece.hash == 0)
					p.hash = 0;
				else {
					p.hash = lastPiece.hash * i / numOs;
					hash += p.hash;
				}
			} else {
				if (lastPiece.hash == 0)
					p.hash = 1;
				else
					p.hash = lastPiece.hash * i / (i - numOs);
			}
			p.numOs = numOs;
		}
		lastPiece = p;
		p = afterPiece;
		p.player = 'X';
		p.index = arrangePos.length;
		p.numOs = numOs;
		if (lastPiece.hash == 0)
			p.hash = 1;
		else
			p.hash = lastPiece.hash * numPieces / (numPieces - numOs);
		pieceIterator.toIndex(0);
	}
}
