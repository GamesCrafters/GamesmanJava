package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.game.TopDownC4;
import edu.berkeley.gamesman.game.util.C4State;
import edu.berkeley.gamesman.util.ExpCoefs;

public class TDC4Hasher extends Hasher<C4State> {

	private long[] offsets;

	private long[] arrangeLengths;

	public TDC4Hasher(Configuration conf) {
		super(conf);
	}

	@Override
	public String describe() {
		return "Top-down Connect 4 hasher";
	}

	private void setOffsets() {
		TopDownC4 game = (TopDownC4) conf.getGame();
		offsets = new long[game.gameSize + 2];
		arrangeLengths = new long[game.gameSize + 2];
		ExpCoefs ec = game.ec;
		offsets[0] = 0L;
		arrangeLengths[0] = 1L;
		for (int i = 1; i < offsets.length; i++) {
			offsets[i] = offsets[i - 1] + ec.getCoef(game.gameWidth, i - 1)
					* arrangeLengths[i - 1];
			arrangeLengths[i] = arrangeLengths[i - 1] * i / ((i + 1) / 2);
		}
	}

	@Override
	public long hash(C4State board) {
		if (offsets == null)
			setOffsets();
		return offsets[board.numPieces] + arrangeLengths[board.numPieces]
				* board.spaceArrangement + board.pieceArrangement;
	}

	@Override
	public long numHashes() {
		if (offsets == null)
			setOffsets();
		return offsets[offsets.length - 1];
	}

	@Override
	public C4State unhash(long hash) {
		int numPieces = getNumPieces(hash);
		long newHash = hash - offsets[numPieces];
		long spaceArrangement = newHash / arrangeLengths[numPieces];
		long pieceArrangement = newHash % arrangeLengths[numPieces];
		return new C4State(numPieces, spaceArrangement, pieceArrangement);
	}

	private int getNumPieces(long hash) {
		if (offsets == null)
			setOffsets();
		int smallest = 0, largest = offsets.length - 1;
		int guess = (smallest + largest) / 2;
		while (guess != smallest) {
			if (offsets[guess] > hash)
				largest = guess;
			else
				smallest = guess;
			guess = (smallest + largest) / 2;
		}
		return guess;
	}

}
