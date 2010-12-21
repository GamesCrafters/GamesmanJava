package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.Factory;

/**
 * For a given dartboard game, iterates over possible positions and provides
 * child-replacement hashes
 * 
 * @author dnspies
 */
public class DartboardHasher2 extends DartboardHasher {
	private final int[] pieces;
	private final Count numType;
	private final long[] pieceHashes;
	private final char[] digits;
	private int[][] replacements = new int[0][];
	private long[][] moveDifs;
	private long[][] difs;
	private int replacementSize = 0;
	private long hash;
	private Pool<Count> countPool;

	private class Count {
		private int pieces = 0;
		private boolean negDigit = false;
		private final int[] digitCount;
		private long hashValue = 1L;

		public Count(int numDigits) {
			digitCount = new int[numDigits];
		}

		public void incr(int type) {
			digitCount[type]++;
			if (digitCount[type] > 0) {
				pieces++;
				hashValue = hashValue * pieces / digitCount[type];
			} else if (digitCount[type] == 0)
				negDigit = false;
			else
				throw new Error("Multiple negatives not caught");
		}

		public void decr(int type) {
			if (digitCount[type] > 0) {
				hashValue = hashValue * digitCount[type] / pieces;
				pieces--;
			} else {
				if (negDigit)
					throw new Error(
							"Cannot have more than one negative at a time");
				negDigit = true;
			}
			digitCount[type]--;
		}

		public long getDecr(int type) {
			if (negDigit)
				return 0L;
			else {
				return hashValue * digitCount[type] / pieces;
			}
		}

		public void reset() {
			pieces = 0;
			Arrays.fill(digitCount, 0);
			hashValue = 1L;
			negDigit = false;
		}

		public int get(int digit) {
			return digitCount[digit];
		}

		public void trade(int i, int j) {
			if (digitCount[i] > 0 && digitCount[j] >= 0) {
				digitCount[j]++;
				hashValue = hashValue * digitCount[i] / digitCount[j];
				digitCount[i]--;
			} else {
				incr(j);
				decr(i);
			}
		}

		public void setCount(Count otherCount) {
			System.arraycopy(otherCount.digitCount, 0, digitCount, 0,
					digits.length);
			pieces = otherCount.pieces;
			negDigit = otherCount.negDigit;
			hashValue = otherCount.hashValue;
		}

		public void setCount(int[] numType) {
			reset();
			for (int type = 0; type < digits.length; type++) {
				if (numType[type] < 0) {
					if (numType[type] == -1)
						decr(type);
					else
						throw new Error(
								"Cannot have more than one negative at a time");
				} else {
					for (int i = 0; i < numType[type]; i++) {
						incr(type);
					}
				}
			}
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof Count) {
				Count c = (Count) other;
				return Arrays.equals(digitCount, c.digitCount);
			} else
				return false;
		}
	}

	/**
	 * @param len
	 *            The number of spaces on the board
	 * @param digits
	 *            The different possible pieces which can occupy this board
	 */
	public DartboardHasher2(final int len, final char... digits) {
		super(len, digits);
		pieces = new int[len];
		pieceHashes = new long[len];
		this.digits = new char[digits.length];
		countPool = new Pool<Count>(new Factory<Count>() {

			public Count newObject() {
				return new Count(len);
			}

			public void reset(Count t) {
				t.reset();
			}

		});
		numType = countPool.get();
		System.arraycopy(digits, 0, this.digits, 0, digits.length);
	}

	/**
	 * Sets the number of each type of digit. The arguments are in the same
	 * order as this hasher's digits were initialized in.
	 * 
	 * @param numType
	 *            The number of each type of digit. Should sum to len
	 */
	@Override
	public void setNums(int... numType) {
		if (numType.length != digits.length)
			throw new Error("Wrong number of arguments");
		this.numType.setCount(numType);
		Count count = countPool.get();
		count.setCount(this.numType);
		reset(count);
		countPool.release(count);
		hash = 0L;
	}

	/**
	 * <p>
	 * Notifies this hasher which type of children should be memoized. The
	 * arguments are pairs of chars such that the first one is the character in
	 * the board string and the second one is the character it should be
	 * replaced with.
	 * </p>
	 * 
	 * <p>
	 * Once this has been called, you may ask for getChildren for any pair of
	 * provided replacements
	 * </p>
	 * 
	 * Ex: <br />
	 * <code>
	 * DartboardHasher hasher = new DartboardHasher(9,' ','O','X');<br />
	 * hasher.unhash("X OXO XO ");<br />
	 * hasher.setReplacements(' ','O',' ','X');<br />
	 * long[] children = new long[9];<br />
	 * hasher.getChildren(' ','O',children);<br />
	 * </code>
	 * 
	 * @param replacements
	 *            The set of replacements allowed
	 */
	@Override
	public void setReplacements(char... replacements) {
		replacementSize = replacements.length / 2;
		if (this.replacements.length < replacementSize) {
			this.replacements = new int[replacementSize][2];
			this.difs = new long[replacementSize][pieces.length];
			this.moveDifs = new long[replacementSize][pieces.length];
		}
		for (int replacement = 0; replacement < replacementSize; replacement++) {
			this.replacements[replacement][0] = findDigit(replacements[replacement * 2]);
			this.replacements[replacement][1] = findDigit(replacements[replacement * 2 + 1]);
		}
		calculateReplacements();
	}

	/**
	 * Iterates to the next arrangement (if possible)
	 * 
	 * @return Whether or not there are any remaining arrangements
	 */
	@Override
	public boolean next() {
		return next(null);
	}

	/**
	 * Iterates to the next arrangement (if possible). After calling this, you
	 * may iterate over the values in changed to know what was changed. They
	 * will be returned in strictly increasing order
	 * 
	 * @param changed
	 *            An iterator to be reset and filled.
	 * @return Whether or not there are any remaining arrangements
	 */
	@Override
	public boolean next(ChangedIterator changed) {
		return next(changed, 0);
	}

	private boolean next(int lowBit) {
		return next(null, lowBit);
	}

	private boolean next(ChangedIterator changed, int lowBit) {
		if (changed != null)
			changed.reset();
		Count count = countPool.get();
		int highest = 0;
		int piece;
		boolean didChange = false;
		for (piece = 0; piece < pieces.length; piece++) {
			int digit = pieces[piece];
			count.incr(digit);
			if (piece >= lowBit && digit < highest)
				break;
			else if (digit > highest)
				highest = digit;
		}
		if (piece < pieces.length) {
			int digit = pieces[piece];
			do {
				digit++;
			} while (count.get(digit) == 0);
			pieces[piece] = digit;
			count.decr(digit);
			long removed = reset(count, changed);
			count.incr(digit);
			if (lowBit == 0) {
				pieceHashes[piece] += removed + 1;
				hash++;
			} else {
				hash -= removed;
				hash -= pieceHashes[piece];
				pieceHashes[piece] = 0;
				for (int d = 0; d < digit; d++) {
					pieceHashes[piece] += count.getDecr(d);
				}
				hash += pieceHashes[piece];
			}
			for (int i = 0; i < replacementSize; i++) {
				int[] replacement = replacements[i];
				count.trade(replacement[0], replacement[1]);
				difs[i][piece] = -pieceHashes[piece];
				for (int j = 0; j < digit; j++) {
					difs[i][piece] += count.getDecr(j);
				}
				count.trade(replacement[1], replacement[0]);
			}
			if (changed != null)
				changed.add(piece);
			didChange = true;
		}
		countPool.release(count);
		return didChange;
	}

	private boolean previous(int lowBit) {
		Count count = countPool.get();
		int lowest = digits.length - 1;
		int piece;
		boolean didChange = false;
		for (piece = 0; piece < pieces.length; piece++) {
			int digit = pieces[piece];
			count.incr(digit);
			if (piece >= lowBit && digit > lowest)
				break;
			else if (digit < lowest)
				lowest = digit;
		}
		if (piece < pieces.length) {
			int digit = pieces[piece];
			do {
				digit--;
			} while (count.get(digit) == 0);
			pieces[piece] = digit;
			count.decr(digit);
			long added = forset(count);
			count.incr(digit);
			hash += added;
			hash -= pieceHashes[piece];
			pieceHashes[piece] = 0;
			for (int d = 0; d < digit; d++) {
				pieceHashes[piece] += count.getDecr(d);
			}
			hash += pieceHashes[piece];
			for (int i = 0; i < replacementSize; i++) {
				int[] replacement = replacements[i];
				count.trade(replacement[0], replacement[1]);
				difs[i][piece] = -pieceHashes[piece];
				for (int j = 0; j < digit; j++) {
					difs[i][piece] += count.getDecr(j);
				}
				count.trade(replacement[1], replacement[0]);
			}
			didChange = true;
		}
		countPool.release(count);
		return didChange;
	}

	private long forset(Count count) {
		long oldHashes = 0L;
		int piece = 0;
		Count secondCount = countPool.get();
		for (int digit = 0; digit < digits.length; digit++) {
			int digitCount = count.get(digit);
			for (int i = 0; i < digitCount; i++) {
				oldHashes -= pieceHashes[piece];
				pieceHashes[piece] = 0L;
				secondCount.incr(digit);
				for (int k = 0; k < digit; k++) {
					pieceHashes[piece] += secondCount.getDecr(k);
				}
				pieces[piece] = digit;
				oldHashes += pieceHashes[piece];
				for (int j = 0; j < replacementSize; j++) {
					int[] replacement = replacements[j];
					long[] dif = difs[j];
					long[] moveDif = moveDifs[j];
					dif[piece] = -pieceHashes[piece];
					moveDif[piece] = 0L;
					secondCount.trade(replacement[0], replacement[1]);
					for (int k = 0; k < digit || digit == replacement[0]
							&& k < replacement[1]; k++) {
						long arrangement = secondCount.getDecr(k);
						if (k < digit) {
							dif[piece] += arrangement;
							if (k >= replacement[1]) {
								moveDif[piece] -= arrangement;
							}
						} else if (k < replacement[1])
							moveDif[piece] += arrangement;
						else
							throw new Error("Why am I here?");
					}
					secondCount.trade(replacement[1], replacement[0]);
				}
				piece++;
			}
		}
		countPool.release(secondCount);
		return oldHashes;
	}

	private long reset(Count count) {
		return reset(count, null);
	}

	private long reset(Count count, ChangedIterator changed) {
		long oldHashes = 0L;
		int piece = 0;
		Count secondCount = countPool.get();
		for (int digit = digits.length - 1; digit >= 0; digit--) {
			int digitCount = count.get(digit);
			for (int i = 0; i < digitCount; i++) {
				if (pieces[piece] != digit) {
					pieces[piece] = digit;
					if (changed != null)
						changed.add(piece);
				}
				secondCount.incr(digit);
				oldHashes += pieceHashes[piece];
				pieceHashes[piece] = 0L;
				for (int j = 0; j < replacementSize; j++) {
					int[] replacement = replacements[j];
					long[] dif = difs[j];
					long[] moveDif = moveDifs[j];
					dif[piece] = 0L;
					moveDif[piece] = 0L;
					secondCount.trade(replacement[0], replacement[1]);
					for (int k = 0; k < digit || digit == replacement[0]
							&& k < replacement[1]; k++) {
						long arrangement = secondCount.getDecr(k);
						if (k < digit) {
							dif[piece] += arrangement;
							if (k >= replacement[1]) {
								moveDif[piece] -= arrangement;
							}
						} else if (k < replacement[1])
							moveDif[piece] += arrangement;
						else
							throw new Error("Why am I here?");
					}
					secondCount.trade(replacement[1], replacement[0]);
				}
				piece++;
			}
		}
		countPool.release(secondCount);
		return oldHashes;
	}

	/**
	 * Sets this hasher to match the passed array and returns the hash. Throws
	 * an error if the number of each type does not match those specified in the
	 * last call to setNums
	 * 
	 * @param pieces
	 *            The array of chars to match
	 * @return The hash of this arrangement
	 */
	@Override
	public long hash(char[] pieces) {
		return hash(pieces, false);
	}

	private long hash(char[] pieces, boolean setNums) {
		for (int piece = 0; piece < pieces.length; piece++) {
			int digit = findDigit(pieces[piece]);
			this.pieces[piece] = digit;
		}
		return rehash(setNums);
	}

	private long rehash(boolean setNums) {
		long totalHash = 0L;
		Count count = countPool.get();
		for (int piece = 0; piece < pieces.length; piece++) {
			long hash = 0L;
			int digit = pieces[piece];
			count.incr(digit);
			for (int i = 0; i < digit; i++)
				hash += count.getDecr(i);
			pieceHashes[piece] = hash;
			totalHash += hash;
		}
		if (setNums) {
			numType.setCount(count);
		} else if (!numType.equals(count))
			throw new Error("Wrong number of pieces");
		countPool.release(count);
		this.hash = totalHash;
		calculateReplacements();
		return totalHash;
	}

	/**
	 * This is equivalent to counting the number of each type in the array and
	 * calling setNums for the respective values followed by then calling hash
	 * on the array.
	 * 
	 * @param pieces
	 *            The array of chars to match
	 * @return The hash of this arrangement
	 */
	@Override
	public long setNumsAndHash(char[] pieces) {
		return hash(pieces, true);
	}

	/**
	 * Sets this hasher to match the specified hash
	 * 
	 * @param hash
	 *            The hash to set this to
	 */
	@Override
	public void unhash(long hash) {
		this.hash = hash;
		int digit;
		Count count = countPool.get();
		count.setCount(numType);
		for (int piece = pieces.length - 1; piece >= 0; piece--) {
			long digitHash = 0L;
			long pieceHash = 0L;
			if (hash > 0) {
				for (digit = 0; true; digit++) {
					digitHash = count.getDecr(digit);
					if (hash >= digitHash) {
						hash -= digitHash;
						pieceHash += digitHash;
					} else
						break;
				}
				pieceHashes[piece] = pieceHash;
			} else {
				for (digit = 0; count.get(digit) == 0; digit++)
					;
				pieceHashes[piece] = 0L;
			}
			count.decr(digit);
			pieces[piece] = digit;
		}
		countPool.release(count);
		calculateReplacements();
	}

	private void calculateReplacements() {
		for (int replacement = 0; replacement < replacementSize; replacement++) {
			int[] rPair = replacements[replacement];
			long[] rDifs = difs[replacement];
			long[] moveDif = moveDifs[replacement];
			Count count = countPool.get();
			count.trade(rPair[0], rPair[1]);
			for (int piece = 0; piece < pieces.length; piece++) {
				count.incr(pieces[piece]);
				rDifs[piece] = -pieceHashes[piece];
				moveDif[piece] = 0L;
				for (int digit = 0; digit < pieces[piece]
						|| (pieces[piece] == rPair[0] && digit < rPair[1]); digit++) {
					long arrangement = count.getDecr(digit);
					if (digit < pieces[piece]) {
						rDifs[piece] += arrangement;
						if (digit >= rPair[1])
							moveDif[piece] -= arrangement;
					} else if (digit < rPair[1])
						moveDif[piece] += arrangement;
					else
						throw new Error("Why am I here?");
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

	/**
	 * @param piece
	 *            The index into the internal array
	 * @return The char currently at that index
	 */
	@Override
	public char get(int piece) {
		return digits[pieces[piece]];
	}

	/**
	 * @return The total number of possible rearrangements for the last call to
	 *         setNums
	 */
	@Override
	public long numHashes() {
		return numType.hashValue;
	}

	@Override
	public void getChildren(char old, char replace, long[] childArray) {
		int[] replacement = null;
		long[] dif = null;
		long[] moveDif = null;
		int i;
		for (i = 0; i < replacementSize; i++) {
			replacement = replacements[i];
			if (digits[replacement[0]] == old
					&& digits[replacement[1]] == replace) {
				dif = difs[i];
				moveDif = moveDifs[i];
				break;
			}
		}
		if (i == replacementSize)
			throw new Error("No such replacement set");
		long hashDif = 0L;
		for (int piece = pieces.length - 1; piece >= 0; piece--) {
			hashDif += dif[piece];
			if (pieces[piece] == replacement[0]) {
				childArray[piece] = hash + hashDif + moveDif[piece];
			} else
				childArray[piece] = -1;
		}
	}

	private void fillChildren(char old, char replace, long[] childArray,
			boolean prev) {
		long curHash = hash;
		int[] replacement = null;
		long[] dif = null;
		int i;
		for (i = 0; i < replacementSize; i++) {
			replacement = replacements[i];
			if (digits[replacement[0]] == old
					&& digits[replacement[1]] == replace) {
				dif = difs[i];
				break;
			}
		}
		if (i == replacementSize)
			throw new Error("No such replacement set");
		getChildren(old, replace, childArray);
		int remainingPieces = 0;
		for (int piece = 0; piece < pieces.length; piece++) {
			if (childArray[piece] < 0)
				remainingPieces++;
		}
		if (remainingPieces == pieces.length)
			return;
		OUTER: while (remainingPieces > 0) {
			boolean oldSeen = false, emptySeen = false, madeChange = false;
			for (int piece = 0; piece < pieces.length; piece++) {
				if (pieces[piece] == replacement[0]) {
					oldSeen = true;
					if (childArray[piece] < 0) {
						childArray[piece] = getChild(dif, replacement[0],
								replacement[1], piece);
						remainingPieces--;
						emptySeen = true;
					}
				} else if (childArray[piece] < 0)
					emptySeen = true;
				if (oldSeen && emptySeen && !madeChange) {
					if (prev) {
						if (!previous(piece))
							break OUTER;
					} else {
						if (!next(piece))
							break OUTER;
					}
					madeChange = true;
				}
			}
		}
		unhash(curHash);
	}

	@Override
	public void nextChildren(char old, char replace, long[] childArray) {
		fillChildren(old, replace, childArray, false);
	}

	@Override
	public void previousChildren(char old, char replace, long[] childArray) {
		fillChildren(old, replace, childArray, true);
	}

	private long getChild(long[] dif, int oldNum, int replaceNum, int i) {
		long hashDif = 0L;
		Count count = countPool.get();
		count.setCount(numType);
		count.trade(oldNum, replaceNum);
		for (int piece = pieces.length - 1; piece > i; piece--) {
			count.decr(pieces[piece]);
			hashDif += dif[piece];
		}
		long pDif = 0L;
		for (int digit = 0; digit < replaceNum; digit++) {
			long arrangement = count.getDecr(digit);
			pDif += arrangement;
		}
		countPool.release(count);
		return hash + hashDif + pDif - pieceHashes[i];
	}

	public long getHash() {
		return hash;
	}

	@Override
	public void getCharArray(char[] charArray) {
		if (charArray.length != pieces.length)
			throw new Error("Wrong length char array");
		for (int i = 0; i < pieces.length; i++) {
			charArray[i] = get(i);
		}
	}

	@Override
	public String toString() {
		char[] stringArr = new char[pieces.length];
		getCharArray(stringArr);
		return new String(stringArr);
	}

	public static void main(String[] args) {
		DartboardHasher2 dh = new DartboardHasher2(9, ' ', 'O', 'X');
		DartboardHasher2 dh2 = new DartboardHasher2(9, ' ', 'O', 'X');
		dh.setNums(4, 2, 3);
		dh2.setNums(3, 3, 3);
		dh.setReplacements(' ', 'O');
		long[] childrenPrev = new long[9];
		long[] childrenNext = new long[9];
		for (int i = 0; i < 1260; i++) {
			if (dh.hash != i)
				throw new Error("Hashes don't match");
			System.out.println(dh.toString());
			dh.previousChildren(' ', 'O', childrenPrev);
			if (childrenPrev[5] == -1)
				System.out.println("Nothing");
			else {
				dh2.unhash(childrenPrev[5]);
				System.out.println(dh2.toString());
			}
			dh.nextChildren(' ', 'O', childrenNext);
			if (childrenNext[5] == -1)
				System.out.println("Nothing");
			else {
				dh2.unhash(childrenNext[5]);
				System.out.println(dh2.toString());
			}
			for (int child = 0; child < childrenPrev.length; child++)
				if (childrenPrev[child] > childrenNext[child]
						&& childrenNext[child] > 0)
					throw new Error("Wrong Order!");
			dh.next();
			System.out.println();
		}
	}

	@Override
	public void set(int boardNum, char p) {
		pieces[boardNum] = findDigit(p);
		rehash(true);
	}

	@Override
	public int boardSize() {
		return pieces.length;
	}

	public boolean majorChanged() {
		return true;
	}
}