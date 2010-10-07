package edu.berkeley.gamesman.hasher;

import java.util.ArrayList;
import java.util.Arrays;

import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.Factory;

public class DartboardHasher {
	private final int[] pieces;
	private final int[] numType;
	private final long[] pieceHashes;
	private final char[] digits;
	private ArrayList<int[]> replacements = new ArrayList<int[]>();
	private ArrayList<long[]> difs = new ArrayList<long[]>();
	private final RearrangeFunction[] rearrange;
	private long hash;
	private Pool<int[]> countPool;

	private static class RearrangeFunction {
		final long value;
		final RearrangeFunction[] innerValues;

		public RearrangeFunction(int pieceTypes, int numPieces) {
			this(1L, pieceTypes, numPieces);
		}

		private RearrangeFunction(long current, int pieceTypes,
				int remainingPieces) {
			value = current;
			if (pieceTypes == 1) {
				innerValues = null;
			} else if (pieceTypes < 1) {
				throw new Error(
						"There should always be at least one piece type");
			} else {
				innerValues = new RearrangeFunction[remainingPieces + 1];
				for (int i = 0; i <= remainingPieces; i++) {
					innerValues[i] = new RearrangeFunction(current,
							pieceTypes - 1, remainingPieces - i);
					current = current * (remainingPieces - i) / (i + 1);
				}
			}
		}

		private long get(int[] k, int off) {
			if (innerValues == null) {
				if (off < k.length - 1)
					throw new Error("k too long");
				else
					return value;
			} else if (k[off] < innerValues.length && k[off] >= 0)
				return innerValues[k[off]].get(k, off + 1);
			else
				return 0L;
		}

		public long get(int... k) {
			return get(k, 0);
		}
	}

	public DartboardHasher(final char[] digits, int len) {
		pieces = new int[len];
		pieceHashes = new long[len];
		this.digits = new char[digits.length];
		countPool = new Pool<int[]>(new Factory<int[]>() {

			public int[] newObject() {
				return new int[digits.length];
			}

			public void reset(int[] t) {
				Arrays.fill(t, 0);
			}

		});
		numType = new int[digits.length];
		System.arraycopy(digits, 0, this.digits, 0, digits.length);
		rearrange = new RearrangeFunction[len + 1];
		for (int i = 0; i <= len; i++) {
			rearrange[i] = new RearrangeFunction(digits.length, i);
		}
	}

	public void setNums(int... numType) {
		if (numType.length != digits.length)
			throw new Error("Wrong number of arguments");
		System.arraycopy(numType, 0, this.numType, 0, numType.length);
		int[] count = countPool.get();
		System.arraycopy(numType, 0, count, 0, numType.length);
		reset(count);
		countPool.release(count);
		hash = 0L;
	}

	public void setReplacements(char... replacements) {
		this.replacements.clear();
		difs.clear();
		for (int replacement = 0; replacement < replacements.length; replacement += 2) {
			this.replacements.add(new int[] {
					findDigit(replacements[replacement]),
					findDigit(replacements[replacement + 1]) });
			difs.add(new long[pieces.length]);
		}
		calculateReplacements();
	}

	public void next() {
		next(null);
	}

	public void next(ChangedIterator changed) {
		if (changed != null)
			changed.reset();
		int[] count = countPool.get();
		int highest = 0;
		int piece;
		for (piece = 0; piece < pieces.length; piece++) {
			int digit = pieces[piece];
			count[digit]++;
			if (digit < highest)
				break;
			else
				highest = digit;
		}
		if (piece < pieces.length) {
			int digit = pieces[piece];
			do {
				digit++;
			} while (count[digit] == 0);
			pieces[piece] = digit;
			count[digit]--;
			pieceHashes[piece] += reset(count, changed) + 1;
			count[digit]++;
			for (int i = 0; i < replacements.size(); i++) {
				int[] replacement = replacements.get(i);
				count[replacement[0]]--;
				count[replacement[1]]++;
				difs.get(i)[piece] = -pieceHashes[piece];
				for (int j = 0; j < digit; j++) {
					count[j]--;
					difs.get(i)[piece] += rearrange(piece, count);
					count[j]++;
				}
				count[replacement[1]]--;
				count[replacement[0]]++;
			}
			hash++;
			if (changed != null)
				changed.add(piece);
		} else {
			reset(count, changed);
			hash = 0L;
		}
		countPool.release(count);
	}

	private long reset(int[] count) {
		return reset(count, null);
	}

	private long reset(int[] count, ChangedIterator changed) {
		long oldHashes = 0L;
		int piece = 0;
		int[] secondCount = countPool.get();
		for (int digit = digits.length - 1; digit >= 0; digit--) {
			int digitCount = count[digit];
			for (int i = 0; i < digitCount; i++) {
				if (pieces[piece] != digit) {
					pieces[piece] = digit;
					if (changed != null)
						changed.add(piece);
				}
				secondCount[digit]++;
				oldHashes += pieceHashes[piece];
				pieceHashes[piece] = 0L;
				for (int j = 0; j < replacements.size(); j++) {
					int[] replacement = replacements.get(j);
					long[] dif = difs.get(j);
					dif[piece] = 0L;
					secondCount[replacement[0]]--;
					secondCount[replacement[1]]++;
					for (int k = 0; k < digit; k++) {
						secondCount[k]--;
						dif[piece] += rearrange(piece, secondCount);
						secondCount[k]++;
					}
					secondCount[replacement[1]]--;
					secondCount[replacement[0]]++;
				}
				piece++;
			}
		}
		countPool.release(secondCount);
		return oldHashes;
	}

	private long rearrange(int n, int... k) {
		return rearrange[n].get(k);
	}

	public long hash(char[] pieces) {
		if (pieces.length != this.pieces.length)
			throw new Error("Wrong number of pieces");
		long totalHash = 0L;
		int[] count = countPool.get();
		for (int piece = 0; piece < pieces.length; piece++) {
			long hash = 0L;
			int digit = findDigit(pieces[piece]);
			this.pieces[piece] = digit;
			count[digit]++;
			for (int i = 0; i < digit; i++) {
				if (count[i] > 0) {
					count[i]--;
					hash += rearrange(piece, count);
					count[i]++;
				}
			}
			pieceHashes[piece] = hash;
			totalHash += hash;
		}
		if (!Arrays.equals(numType, count))
			throw new Error("Wrong number of pieces");
		countPool.release(count);
		this.hash = totalHash;
		calculateReplacements();
		return totalHash;
	}

	public void unhash(long hash) {
		this.hash = hash;
		int digit;
		int[] count = countPool.get();
		System.arraycopy(numType, 0, count, 0, numType.length);
		for (int piece = pieces.length - 1; piece >= 0; piece--) {
			long digitHash = 0L;
			long pieceHash = 0L;
			if (hash > 0) {
				for (digit = 0; true; digit++) {
					count[digit]--;
					digitHash = rearrange(piece, count);
					count[digit]++;
					if (hash >= digitHash) {
						hash -= digitHash;
						pieceHash += digitHash;
					} else
						break;
				}
				pieceHashes[piece] = pieceHash;
			} else {
				for (digit = 0; count[digit] == 0; digit++)
					;
				pieceHashes[piece] = 0L;
			}
			count[digit]--;
			pieces[piece] = digit;
		}
		countPool.release(count);
		calculateReplacements();
	}

	private void calculateReplacements() {
		for (int replacement = 0; replacement < replacements.size(); replacement++) {
			int[] rPair = replacements.get(replacement);
			long[] rDifs = difs.get(replacement);
			int[] count = countPool.get();
			count[rPair[0]]--;
			count[rPair[1]]++;
			for (int piece = 0; piece < pieces.length; piece++) {
				count[pieces[piece]]++;
				for (int digit = 0; digit < pieces[piece]; digit++) {
					count[digit]--;
					rDifs[piece] += rearrange(piece, count)
							- pieceHashes[piece];
					count[digit]++;
				}
			}
			countPool.release(count);
		}
	}

	private int findDigit(char c) {
		for (int digit = 0; digit < digits.length; digit++) {
			if (c == digits[digit])
				return digit;
		}
		return -1;
	}

	public char get(int piece) {
		return digits[pieces[piece]];
	}

	public long numHashes() {
		return rearrange(pieces.length, numType);
	}

	public void getChildren(char old, char replace, long[] childArray) {
		int[] replacement = null;
		long[] dif = null;
		int i;
		for (i = 0; i < replacements.size(); i++) {
			replacement = replacements.get(i);
			if (digits[replacement[0]] == old
					&& digits[replacement[1]] == replace) {
				dif = difs.get(i);
				break;
			}
		}
		if (i == replacements.size())
			throw new Error("No such replacement set");
		long hashDif = 0L;
		int[] count = countPool.get();
		System.arraycopy(numType, 0, count, 0, numType.length);
		count[replacement[1]]++;
		for (int piece = pieces.length - 1; piece >= 0; piece--) {
			count[pieces[piece]]--;
			if (pieces[piece] == replacement[0]) {
				long pDif = 0L;
				for (int digit = 0; digit < replacement[1]; digit++) {
					count[digit]--;
					pDif += rearrange(piece, count);
					count[digit]++;
				}
				childArray[piece] = hash + hashDif + pDif - pieceHashes[piece];
			} else
				childArray[piece] = -1;
			hashDif += dif[piece];
		}
		countPool.release(count);
	}

	public String toString() {
		char[] stringArr = new char[pieces.length];
		for (int i = 0; i < pieces.length; i++) {
			stringArr[i] = get(i);
		}
		return new String(stringArr);
	}

	public static void main(String[] args) {
		DartboardHasher dh = new DartboardHasher(new char[] { ' ', 'O', 'X' },
				9);
		dh.setNums(4, 2, 3);
		dh.setReplacements(' ', 'O');
		long[] children = new long[9];
		long start = System.nanoTime();
		for (int i = 0; i < 1260; i++) {
			dh.getChildren(' ', 'O', children);
			dh.next();
		}
		long end = System.nanoTime();
		System.out.println(end - start);
	}
}