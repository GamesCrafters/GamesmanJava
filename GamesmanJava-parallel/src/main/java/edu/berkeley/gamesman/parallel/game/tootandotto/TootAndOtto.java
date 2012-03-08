package edu.berkeley.gamesman.parallel.game.tootandotto;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.hadoop.conf.Configuration;

import edu.berkeley.gamesman.game.type.GameValue;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;
import edu.berkeley.gamesman.hasher.genhasher.Move;
import edu.berkeley.gamesman.parallel.ranges.RangeTree;
import edu.berkeley.gamesman.solve.reader.SolveReader;
import edu.berkeley.gamesman.util.Pair;

public class TootAndOtto extends RangeTree<TOState> implements
		SolveReader<TOState> {
	private Move[] myMoves;
	private Move[][] colMoves; // TODO: what does colMoves do?
	private TOHasher myHasher;
	private int width, height;
	private int gameSize;
	private int suffLen;

	@Override
	public void rangeTreeConfigure(Configuration conf) {
		width = conf.getInt("gamesman.game.width", 5);
		height = conf.getInt("gamesman.game.height", 4);
		gameSize = width * height;
		myHasher = new TOHasher(width, height);
		int varianceLength = conf.getInt("gamesman.game.variance.length", 10);
		suffLen = Math.max(1, gameSize + 5 - varianceLength);

		ArrayList<Move>[] columnMoveList = new ArrayList[width];
		colMoves = new Move[width][];
		for (int i = 0; i < width; i++) {
			columnMoveList[i] = new ArrayList<Move>();
		}
		// TODO: how to rewrite this block for TO
		for (int numPieces = 0; numPieces < gameSize; numPieces++) {
			int turn = getTurn(numPieces);
			for (int row = 0; row < height; row++) {
				for (int col = 0; col < width; col++) {
					int place = getPlace(row, col);
					if (isBottom(row, col)) {
						columnMoveList[col].add(new Move(place, 0, turn,
								gameSize, numPieces, numPieces + 1));
					} else {
						columnMoveList[col].add(new Move(place - 1, 1, 1,
								place, 0, turn, gameSize, numPieces,
								numPieces + 1));
						columnMoveList[col].add(new Move(place - 1, 2, 2,
								place, 0, turn, gameSize, numPieces,
								numPieces + 1));
					}
				}
			}
		}

		ArrayList<Move> allMoves = new ArrayList<Move>();
		for (int i = 0; i < width; i++) {
			colMoves[i] = columnMoveList[i].toArray(new Move[columnMoveList[i]
					.size()]);
			allMoves.addAll(columnMoveList[i]);
		}
		myMoves = allMoves.toArray(new Move[allMoves.size()]);

	}

	/**
	 * Determine if we are at the bottom of the column
	 * 
	 * @param row
	 * @param col
	 * @return true if the position (row, col) is at the bottom of a column
	 */
	private boolean isBottom(int row, int col) {
		return row == 0;
	}

	/**
	 * Translate the coordinate on the board into the nth place on the board in
	 * column major order
	 * 
	 * @param row
	 * @param col
	 * @return
	 */
	private int getPlace(int row, int col) {
		return col * height + row;
	}

	/**
	 * Determine whose turn it is, given the number of pieces on the board
	 * 
	 * @param numPieces
	 * @return 1 if it is the first player's turn, 2 if it is the second
	 *         player's turn.
	 */
	private int getTurn(int numPieces) {
		return (numPieces % 2) + 1;
	}

	@Override
	public TOState getPosition(String board) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Pair<String, TOState>> getChildren(TOState position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getString(TOState position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GameValue getValue(TOState state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<TOState> getStartingPositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GenHasher<TOState> getHasher() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Move[] getMoves() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int suffixLength() {
		// TODO Auto-generated method stub
		return 0;
	}

}
