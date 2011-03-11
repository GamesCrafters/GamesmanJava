package edu.berkeley.gamesman.hasher;

public class C4GeneratingHasher {
	private final char[][] pieces;
	private final long[][] pieceHashes;
	private final long[] colHashes;
	private final long[][][][] rearrange;
	private int tier;
	private long hash;

	public C4GeneratingHasher(int width, int height) {
		pieces = new char[width][height];
		pieceHashes = new long[width][height];
		colHashes = new long[width];
		rearrange = new long[width][height + 1][][];
		int length = 1;
		rearrange[0][0] = new long[][] { { 1 } };
		for (int col = 0; col < width; col++) {
			if (col > 0) {
				rearrange[col][0] = new long[length][];
				for (int o = 0; o < length; o++) {
					rearrange[col][0][o] = new long[length - o];
					for (int x = 0; x < length - o; x++) {
						rearrange[col][0][o][x] = 0L;
						for (int colHeight = 0; colHeight <= height; colHeight++) {
							rearrange[col][0][o][x] += rearrange(col - 1,
									colHeight, o, x);
						}
					}
				}
			}
			for (int colHeight = 1; colHeight <= height; colHeight++) {
				rearrange[col][colHeight] = new long[++length][];
				for (int o = 0; o < length; o++) {
					rearrange[col][colHeight][o] = new long[length - o];
					for (int x = 0; x < length - o; x++) {
						rearrange[col][colHeight][o][x] = rearrange(col,
								colHeight - 1, o - 1, x)
								+ rearrange(col, colHeight - 1, o, x - 1);
					}
				}
			}
		}
	}

	private long rearrange(int c, int h, int o, int x) {
		if (h < 0 || o < 0 || o >= rearrange[c][h].length || x < 0
				|| x >= rearrange[c][h][o].length)
			return 0L;
		return rearrange[c][h][o][x];
	}

	public void setTier(int tier) {
		this.tier = tier;
		int oCount = tier / 2;
		int xCount = (tier + 1) / 2;
		for (int col = 0; col < pieces.length; col++) {
			for (int row = 0; row < pieces[col].length; row++) {
				if (xCount > 0) {
					pieces[col][row] = 'X';
					xCount--;
				} else if (oCount > 0) {
					pieces[col][row] = 'O';
					oCount--;
				} else {
					pieces[col][row] = ' ';
				}
				pieceHashes[col][row] = 0L;
			}
		}
	}

	public long hash(char[] board) {
		int oCount = 0, xCount = 0;
		int piece = 0;
		hash = 0L;
		for (int col = 0; col < pieces.length; col++) {
			int row;
			for (row = 0; row < pieces[col].length; row++) {
				pieces[col][row] = board[piece++];
				if (pieces[col][row] == ' ')
					break;
				else if (pieces[col][row] == 'X') {
					pieceHashes[col][row] = rearrange(col, row, oCount - 1,
							xCount + 1);
					hash += pieceHashes[col][row];
					xCount++;
				} else if (pieces[col][row] == 'O') {
					pieceHashes[col][row] = 0L;
					oCount++;
				} else {
					throw new Error("Bad piece: " + pieces[col][row]);
				}
			}
			colHashes[col] = 0L;
			int colHeight = row;
			for (row = 0; row < colHeight; row++) {
				colHashes[col] += rearrange(col, row, oCount, xCount);
			}
			hash += colHashes[col];
			for (row++; row < pieces[col].length; row++) {
				pieces[col][row] = board[piece++];
				pieceHashes[col][row] = 0L;
			}
		}
		if (oCount != tier / 2 || xCount != (tier + 1) / 2)
			throw new Error("Wrong number of pieces!");
		return hash;
	}

	public void unhash(long hash) {
		this.hash = hash;
		int oCount = tier / 2, xCount = (tier + 1) / 2;
		for (int col = pieces.length - 1; col >= 0; col--) {
			int colHeight;
			for (colHeight = 0; colHeight < pieces[col].length; colHeight++) {
				long heightHash = rearrange(col, colHeight, oCount, xCount);
				if (hash >= heightHash && (xCount > 0 || oCount > 0))
					hash -= heightHash;
				else
					break;
			}
			for (int row = colHeight; row < pieces[col].length; row++) {
				pieces[col][row] = ' ';
				pieceHashes[col][row] = 0L;
			}
			for (int row = colHeight - 1; row >= 0; row--) {
				long xHash = rearrange(col, row, oCount - 1, xCount);
				if (hash >= xHash && xCount > 0) {
					pieces[col][row] = 'X';
					pieceHashes[col][row] = xHash;
					xCount--;
					hash -= xHash;
				} else {
					pieces[col][row] = 'O';
					pieceHashes[col][row] = 0L;
					oCount--;
				}
			}
		}
	}

	public void next(C4ChangedIterator changed) {
		if (changed != null)
			changed.reset();
		int xCount = 0, oCount = 0;
		int col, row = 0;
		for (col = 0; col < pieces.length; col++) {
			for (row = 0; row < pieces[col].length; row++) {
				if (pieces[col][row] == 'O') {
					oCount++;
					if (xCount > 0) {
						if (pieces[col][row] != 'X') {
							if (changed != null)
								changed.add(col, row);
							pieces[col][row] = 'X';
						}
						xCount--;
						break;
					}
				} else if (pieces[col][row] == 'X')
					xCount++;
				else if (pieces[col][row] == ' ') {
					if (xCount + oCount > row) {
						if (oCount > 0) {
							if (pieces[col][row] != 'O') {
								if (changed != null)
									changed.add(col, row);
								pieces[col][row] = 'O';
							}
							oCount--;
						} else if (xCount > 0) {
							if (pieces[col][row] != 'X') {
								if (changed != null)
									changed.add(col, row);
								pieces[col][row] = 'X';
							}
							xCount--;
						}
						break;
					}
				} else {
					throw new Error("Bad piece");
				}
			}
			if (row < pieces[col].length)
				break;
		}
		if (col == pieces.length)
			row = 0;
		long nextPieceHash = 0L;
		for (int col2 = 0; col2 <= col; col2++) {
			if (col2 == pieces.length)
				break;
			if (col2 < col)
				nextPieceHash += colHashes[col2];
			for (int row2 = 0; row2 < (col2 == col ? row : pieces[col2].length); row2++) {
				nextPieceHash += pieceHashes[col2][row2];
				if (xCount + oCount > row || col2 == col) {
					if (xCount > 0) {
						if (pieces[col2][row2] != 'X') {
							if (changed != null)
								changed.add(col, row);
							pieces[col2][row2] = 'X';
						}
						xCount--;
					} else if (oCount > 0) {
						if (pieces[col2][row2] != 'O') {
							if (changed != null)
								changed.add(col, row);
							pieces[col2][row2] = 'O';
						}
						oCount--;
					}
				} else {
					if (pieces[col][row] != ' ') {
						if (changed != null)
							changed.add(col, row);
						pieces[col2][row2] = ' ';
					}
				}
				pieceHashes[col2][row2] = 0;
			}
			if (col2 < col)
				colHashes[col2] = 0L;
		}
		if (col == pieces.length)
			hash = 0;
		else {
			if (pieces[col][row] == 'O')
				colHashes[col] += nextPieceHash + 1;
			else if (pieces[col][row] == 'X')
				pieceHashes[col][row] += nextPieceHash + 1;
			else
				throw new Error("Bad piece");
			hash++;
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(pieces.length * pieces[0].length);
		for (char[] col : pieces)
			sb.append(col);
		return sb.toString();
	}

	public static void main(String[] args) {
		C4GeneratingHasher gh = new C4GeneratingHasher(4, 3);
		C4GeneratingHasher gh2 = new C4GeneratingHasher(4, 3);
		gh.setTier(5);
		gh2.setTier(5);
		for (long i = 0; i < 410; i++) {
			String s = gh.toString();
			System.out.println(s + ", " + gh.hash + ","
					+ (sum(gh.colHashes) + deepSum(gh.pieceHashes)) + ","
					+ gh2.hash(s.toCharArray()));
			if (i % 13 == 0)
				gh.unhash(i % 400);
			gh.next(null);
		}
	}

	private static long sum(long[] arr) {
		long total = 0L;
		for (long l : arr)
			total += l;
		return total;
	}

	private static long deepSum(long[][] arr) {
		long total = 0L;
		for (long[] arr2 : arr)
			for (long l : arr2)
				total += l;
		return total;
	}
}
