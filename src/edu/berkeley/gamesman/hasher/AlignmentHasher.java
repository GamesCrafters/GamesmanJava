package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.game.Alignment;
import edu.berkeley.gamesman.game.util.AlignmentState;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class AlignmentHasher extends Hasher<AlignmentState> {
	private Alignment game;
	private char[] myArray = null;
	private long[][][] offsets;
	private long numHashes;
	private MMHasher mmh = new MMHasher();

	public void initializeOffsets() {
		game = (Alignment) conf.getGame();
		offsets = new long[game.piecesToWin][game.piecesToWin][game.openCells
				.size() + 1];
		long offset = 0;
		for (int xCaptured = 0; xCaptured < game.piecesToWin; xCaptured++) {
			for (int oCaptured = 0; oCaptured < game.piecesToWin; oCaptured++) {
				for (int pieces = 0; pieces <= game.openCells.size(); pieces++) {
					offsets[xCaptured][oCaptured][pieces] = offset;
					int totalPieces = pieces + xCaptured + oCaptured;
					int xPieces = (totalPieces + 1) / 2 - xCaptured;
					int oPieces = totalPieces / 2 - oCaptured;
					offset += Util.nCr(game.openCells.size(), xPieces + oPieces)
							* Util.nCr(xPieces + oPieces, oPieces);
				}
			}
		}
		numHashes = offset;
	}

	@Override
	public String describe() {
		return "Alignment Hasher";
	}

	@Override
	public long hash(AlignmentState state) {
		if (myArray == null)
			myArray = new char[game.openCells.size()];
		int numPieces = 0;
		for (int i = 0; i < myArray.length; i++) {
			Pair<Integer, Integer> openCell = game.openCells.get(i);
			int row = openCell.car;
			int col = openCell.cdr;
			myArray[i] = state.get(row, col);
			if (myArray[i] != ' ')
				numPieces++;
		}
		if (offsets == null)
			initializeOffsets();
		return offsets[state.xDead][state.oDead][numPieces] + mmh.hash(myArray);
	}

	@Override
	public long numHashes() {
		if (offsets == null)
			initializeOffsets();
		return numHashes;
	}

	@Override
	public void unhash(long hash, AlignmentState state) {
		state.xDead = guessXDead(hash);
		state.oDead = guessODead(hash, state.xDead);
		int numPieces = guessNumPieces(hash, state.xDead, state.oDead);
		int totalPieces = numPieces + state.xDead + state.oDead;
		int xPieces = (totalPieces + 1) / 2 - state.xDead;
		int oPieces = totalPieces / 2 - state.oDead;
		if (myArray == null)
			myArray = new char[game.openCells.size()];
		if (offsets == null)
			initializeOffsets();
		mmh.unhash(hash - offsets[state.xDead][state.oDead][numPieces],
				myArray, xPieces, oPieces);
		for (int i = 0; i < myArray.length; i++) {
			Pair<Integer, Integer> openCell = game.openCells.get(i);
			int row = openCell.car;
			int col = openCell.cdr;
			state.put(row, col, myArray[i]);
		}
	}

	private int guessXDead(long hash) {
		int low = 0, high = offsets.length;
		while (high - low > 1) {
			int guess = (low + high) >> 1;
			if (hash < offsets[guess][0][0])
				high = guess;
			else
				low = guess;
		}
		return low;
	}

	private int guessODead(long hash, int xDead) {
		int low = 0, high = offsets[xDead].length;
		while (high - low > 1) {
			int guess = (low + high) >> 1;
			if (hash < offsets[xDead][guess][0])
				high = guess;
			else
				low = guess;
		}
		return low;
	}

	private int guessNumPieces(long hash, int xDead, int oDead) {
		int low = 0, high = offsets[xDead][oDead].length;
		while (high - low > 1) {
			int guess = (low + high) >> 1;
			if (hash < offsets[xDead][oDead][guess])
				high = guess;
			else
				low = guess;
		}
		return low;
	}

	@Override
	public AlignmentState unhash(long hash) {
		AlignmentState as = game.newState();
		unhash(hash, as);
		return as;
	}

}
