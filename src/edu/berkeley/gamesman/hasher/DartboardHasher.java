package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

/**
 * For a given dartboard game, iterates over possible positions and provides
 * child-replacement hashes
 * 
 * @author dnspies
 */
public final class DartboardHasher {

	private class Replacement {
		private final int oldPiece;
		private final int newPiece;

		private final Group[] groups;
		private final Piece[] pieces;

		private Replacement(int oldPiece, int newPiece) {
			this.oldPiece = oldPiece;
			this.newPiece = newPiece;
			this.groups = new Group[numType.get(oldPiece)];
			for (int i = 0; i < groups.length; i++)
				groups[i] = new Group();
			this.pieces = new Piece[DartboardHasher.this.pieces.length];
			for (int i = 0; i < pieces.length; i++)
				pieces[i] = new Piece();
		}

		private class Group {
			int bottom;
			long dif = 0L, moveDif;
		}

		private class Piece {
			int myGroup = -1;
			long dif = 0L;

			public void setGroup(int group) {
				if (myGroup >= 0)
					groups[myGroup].dif -= dif;
				if (group >= 0)
					groups[group].dif += dif;
				myGroup = group;
			}

			public void setDif(long dif) {
				if (myGroup >= 0)
					groups[myGroup].dif += dif - this.dif;
				this.dif = dif;
			}
		}
	}

	private final Piece[] pieces;
	private final Count numType;
	private final Count count;
	private final Count secondCount;
	private final char[] digits;
	private Replacement[] replacements = new Replacement[0];
	private int replacementSize = 0;
	private long hash;

	private class Piece {
		int digit;
		long hash;

		public String toString() {
			return Character.toString(getChar());
		}

		public char getChar() {
			return digits[digit];
		}
	}

	private class Count {
		private int pieces = 0;
		private boolean negDigit = false;
		private final int[] digitCount;
		private long hashValue = 1L;
		private long preTrade;

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

		// This method only exists because profiling says it might help. It's
		// not "safe" in any sense of the word.
		public void hashlessIncr(int type) {
			digitCount[type]++;
			if (digitCount[type] > 0) {
				pieces++;
			} else if (digitCount[type] == 0)
				negDigit = false;
			else
				throw new Error("Multiple negatives not caught");
		}

		// This method only exists because profiling says it might help. It's
		// not "safe" in any sense of the word.
		public void hashlessDecr(int type) {
			if (digitCount[type] > 0) {
				pieces--;
			} else {
				if (negDigit)
					throw new Error(
							"Cannot have more than one negative at a time");
				negDigit = true;
			}
			digitCount[type]--;
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
			for (int i = 0; i < digitCount.length; i++) {
				digitCount[i] = 0;
			}
			hashValue = 1L;
			negDigit = false;
		}

		public int get(int digit) {
			return digitCount[digit];
		}

		public void trade(int i, int j) {
			if (digitCount[i] > 0 && digitCount[j] >= 0) {
				preTrade = hashValue;
				digitCount[j]++;
				hashValue = hashValue * digitCount[i] / digitCount[j];
				digitCount[i]--;
			} else {
				incr(j);
				decr(i);
			}
		}

		// This method only exists because profiling says it might help. It's
		// not "safe" in any sense of the word.
		public void tradeBack(int i, int j) {
			if (digitCount[i] > 0 && digitCount[j] >= 0) {
				digitCount[j]++;
				hashValue = preTrade;
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
	public DartboardHasher(final int len, final char... digits) {
		pieces = new Piece[len];
		for (int i = 0; i < len; i++) {
			pieces[i] = new Piece();
		}
		this.digits = new char[digits.length];
		numType = new Count(digits.length);
		count = new Count(digits.length);
		secondCount = new Count(digits.length);
		System.arraycopy(digits, 0, this.digits, 0, digits.length);
	}

	/**
	 * Sets the number of each type of digit. The arguments are in the same
	 * order as this hasher's digits were initialized in.
	 * 
	 * @param numType
	 *            The number of each type of digit. Should sum to len
	 */
	public void setNums(int... numType) {
		if (numType.length != digits.length)
			throw new Error("Wrong number of arguments");
		this.numType.setCount(numType);
		resetReplacements();
		reset(this.numType, false, null);
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
	public void setReplacements(char... replacements) {
		replacementSize = replacements.length / 2;
		if (this.replacements.length < replacementSize) {
			this.replacements = new Replacement[replacementSize];
		}
		for (int replacement = 0; replacement < replacementSize; replacement++) {
			this.replacements[replacement] = new Replacement(
					findDigit(replacements[replacement * 2]),
					findDigit(replacements[replacement * 2 + 1]));
		}
		calculateReplacements();
	}

	/**
	 * Iterates to the next arrangement (if possible)
	 * 
	 * @return Whether or not there are any remaining arrangements
	 */
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
	public boolean next(ChangedIterator changed) {
		return next(changed, false, 0);
	}

	private boolean next(ChangedIterator changed, boolean backwards, int lowBit) {
		if (changed != null)
			changed.reset();
		count.reset();
		int highest = backwards ? Integer.MAX_VALUE : -1;
		int piece;
		boolean didChange = false;
		for (piece = 0; piece < pieces.length; piece++) {
			int digit = pieces[piece].digit;
			count.hashlessIncr(digit);
			if (piece >= lowBit
					&& (backwards ? digit > highest : digit < highest))
				break;
			else if (backwards ? digit < highest : digit > highest)
				highest = digit;
		}
		if (piece < pieces.length) {
			int digit = pieces[piece].digit;
			do {
				if (backwards)
					digit--;
				else
					digit++;
			} while (count.get(digit) == 0);
			pieces[piece].digit = digit;
			count.hashlessDecr(digit);
			long removed = reset(count, backwards, changed);
			// reset gives count the correct hash value. According to profiling
			// if there's a place for an ugly hack like that, this is it!
			count.incr(digit);
			long pieceHash;
			if (lowBit == 0 && !backwards) {
				pieces[piece].hash += removed + 1;
				hash += removed + 1;
				pieceHash = pieces[piece].hash;
			} else {
				pieceHash = 0L;
				for (int d = 0; d < digit; d++) {
					pieceHash += count.getDecr(d);
				}
				hash += pieceHash - pieces[piece].hash;
				pieces[piece].hash = pieceHash;
			}
			setReplacements(piece, count);
			if (changed != null)
				changed.add(piece);
			didChange = true;
		}
		return didChange;
	}

	private void setReplacements(int piece, Count count) {
		long pieceHash = pieces[piece].hash;
		int digit = pieces[piece].digit;
		for (int i = 0; i < replacementSize; i++) {
			Replacement replacement = replacements[i];
			int group = count.get(replacement.oldPiece) - 1;
			if (group >= 0) {
				long dif = -pieceHash, moveDif = 0L;
				count.trade(replacement.oldPiece, replacement.newPiece);
				for (int k = 0; k < digit
						|| (digit == replacement.oldPiece && k < replacement.newPiece); k++) {
					long arrangement = count.getDecr(k);
					if (k < digit) {
						dif += arrangement;
						if (k >= replacement.newPiece) {
							moveDif -= arrangement;
						}
					} else if (k < replacement.newPiece)
						moveDif += arrangement;
					else
						throw new Error("Why am I here?");
				}
				if (digit == replacement.oldPiece) {
					replacement.groups[group].bottom = piece;
					replacement.groups[group].moveDif = moveDif;
					group--;
				}
				replacement.pieces[piece].setGroup(group);
				replacement.pieces[piece].setDif(dif);
				count.tradeBack(replacement.newPiece, replacement.oldPiece);
			} else if (group == -1) {
				replacement.pieces[piece].setGroup(-1);
			} else {
				throw new Error("Bad group number " + group);
			}
		}
	}

	private long reset(Count count, boolean backwards, ChangedIterator changed) {
		long oldHashes = 0L;
		int piece = 0;
		secondCount.reset();
		int start, end, dir;
		if (backwards) {
			start = 0;
			end = digits.length;
			dir = 1;
		} else {
			start = digits.length - 1;
			end = -1;
			dir = -1;
		}
		for (int digit = start; digit != end; digit += dir) {
			int digitCount = count.get(digit);
			for (int i = 0; i < digitCount; i++) {
				if (pieces[piece].digit != digit) {
					pieces[piece].digit = digit;
					if (changed != null)
						changed.add(piece);
				}
				secondCount.incr(digit);
				oldHashes += pieces[piece].hash;
				hash -= pieces[piece].hash;
				long pieceHash = 0;
				if (backwards) {
					for (int j = 0; j < digit; j++) {
						pieceHash += secondCount.getDecr(j);
					}
					hash += pieceHash;
				}
				pieces[piece].hash = pieceHash;
				setReplacements(piece, secondCount);
				piece++;
			}
		}
		count.hashValue = secondCount.hashValue;
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
	public long hash(char[] pieces) {
		return hash(pieces, false);
	}

	private long hash(char[] pieces, boolean setNums) {
		for (int piece = 0; piece < pieces.length; piece++) {
			int digit = findDigit(pieces[piece]);
			this.pieces[piece].digit = digit;
		}
		return rehash(setNums);
	}

	private long rehash(boolean setNums) {
		long totalHash = 0L;
		count.reset();
		for (int piece = 0; piece < pieces.length; piece++) {
			long hash = 0L;
			int digit = pieces[piece].digit;
			count.incr(digit);
			for (int i = 0; i < digit; i++)
				hash += count.getDecr(i);
			pieces[piece].hash = hash;
			totalHash += hash;
		}
		if (setNums) {
			numType.setCount(count);
		} else if (!numType.equals(count))
			throw new Error("Wrong number of pieces");
		this.hash = totalHash;
		if (setNums)
			resetReplacements();
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
	public long setNumsAndHash(char[] pieces) {
		return hash(pieces, true);
	}

	/**
	 * Sets this hasher to match the specified hash
	 * 
	 * @param hash
	 *            The hash to set this to
	 */
	public void unhash(long hash) {
		this.hash = hash;
		int digit;
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
				pieces[piece].hash = pieceHash;
			} else {
				for (digit = 0; count.get(digit) == 0; digit++)
					;
				pieces[piece].hash = 0L;
			}
			count.decr(digit);
			pieces[piece].digit = digit;
		}
		calculateReplacements();
	}

	private void resetReplacements() {
		for (int replacement = 0; replacement < replacementSize; replacement++) {
			this.replacements[replacement] = new Replacement(
					this.replacements[replacement].oldPiece,
					this.replacements[replacement].newPiece);
		}
	}

	private void calculateReplacements() {
		for (int replacement = 0; replacement < replacementSize; replacement++) {
			Replacement rPair = replacements[replacement];
			int group = -1;
			count.reset();
			count.trade(rPair.oldPiece, rPair.newPiece);
			for (int piece = 0; piece < pieces.length; piece++) {
				int pieceDigit = pieces[piece].digit;
				if (pieceDigit == rPair.oldPiece)
					group++;
				count.incr(pieceDigit);
				if (group >= 0) {
					long dif = -pieces[piece].hash, moveDif = 0L;
					for (int digit = 0; digit < pieceDigit
							|| (pieceDigit == rPair.oldPiece && digit < rPair.newPiece); digit++) {
						long arrangement = count.getDecr(digit);
						if (digit < pieceDigit) {
							dif += arrangement;
							if (digit >= rPair.newPiece)
								moveDif -= arrangement;
						} else if (digit < rPair.newPiece)
							moveDif += arrangement;
						else
							throw new Error("Why am I here?");
					}
					if (pieceDigit == rPair.oldPiece) {
						rPair.groups[group].bottom = piece;
						rPair.groups[group].moveDif = moveDif;
						rPair.pieces[piece].setGroup(group - 1);
					} else
						rPair.pieces[piece].setGroup(group);
					rPair.pieces[piece].setDif(dif);
				} else if (group == -1)
					rPair.pieces[piece].setGroup(-1);
				else
					throw new Error("Bad group " + group);
			}
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
	public char get(int piece) {
		return pieces[piece].getChar();
	}

	/**
	 * @return The total number of possible rearrangements for the last call to
	 *         setNums
	 */
	public long numHashes() {
		return numType.hashValue;
	}

	public int getChildren(char old, char replace, int[] places,
			long[] childArray) {
		Replacement replacement = null;
		int i;
		for (i = 0; i < replacementSize; i++) {
			replacement = replacements[i];
			if (digits[replacement.oldPiece] == old
					&& digits[replacement.newPiece] == replace)
				break;
		}
		if (i == replacementSize)
			throw new Error("No such replacement set");
		long hashDif = 0L;
		int numReplacements = numType.get(replacement.oldPiece);
		for (int groupNum = numReplacements - 1; groupNum >= 0; groupNum--) {
			Replacement.Group group = replacement.groups[groupNum];
			hashDif += group.dif;
			if (places != null)
				places[groupNum] = group.bottom;
			childArray[groupNum] = hash + hashDif + group.moveDif;
		}
		return numReplacements;
	}

	private void fillChildren(char old, char replace, long[] childArray,
			boolean prev) {
		Replacement replacement = null;
		int i;
		for (i = 0; i < replacementSize; i++) {
			replacement = replacements[i];
			if (digits[replacement.oldPiece] == old
					&& digits[replacement.newPiece] == replace)
				break;
		}
		if (i == replacementSize)
			throw new Error("No such replacement set");
		int[] places = new int[numType.get(replacement.oldPiece)];
		long[] otherArray = new long[places.length];
		getChildren(old, replace, places, otherArray);
		Arrays.fill(childArray, -1);
		for (i = 0; i < places.length; i++) {
			childArray[places[i]] = otherArray[i];
		}
		long curHash = hash;
		int piecesFound = places.length;
		if (piecesFound == 0) {
			return;
		}
		while (piecesFound < pieces.length) {
			boolean foundUnused = false;
			boolean foundOld = false;
			for (i = 0; i < pieces.length; i++) {
				if (childArray[i] == -1)
					foundUnused = true;
				if (pieces[i].digit == replacement.oldPiece)
					foundOld = true;
				if (foundUnused && foundOld)
					break;
			}
			boolean successful = next(null, prev, i);
			if (successful) {
				for (int k = 0; k < pieces.length; k++) {
					if (pieces[k].digit == replacement.oldPiece
							&& childArray[k] == -1) {
						childArray[k] = getChild(replacement, k);
						piecesFound++;
					}
				}
			} else {
				break;
			}
		}
		unhash(curHash);
	}

	public void nextChildren(char old, char replace, long[] childArray) {
		fillChildren(old, replace, childArray, false);
	}

	public void previousChildren(char old, char replace, long[] childArray) {
		fillChildren(old, replace, childArray, true);
	}

	public long previousChild(char old, char replace, int place) {
		return findChild(old, replace, place, true);
	}

	public long nextChild(char old, char replace, int place) {
		return findChild(old, replace, place, false);
	}

	private long findChild(char old, char replace, int place, boolean prev) {
		Replacement replacement = null;
		int i;
		for (i = 0; i < replacementSize; i++) {
			replacement = replacements[i];
			if (digits[replacement.oldPiece] == old
					&& digits[replacement.newPiece] == replace)
				break;
		}
		if (i == replacementSize)
			throw new Error("No such replacement set");
		long curHash = hash;
		long result = getChild(replacement, place);
		while (result == -1L) {
			boolean foundOld = false;
			for (i = 0; i < pieces.length; i++) {
				if (pieces[i].digit == replacement.oldPiece)
					foundOld = true;
				if (i >= place && foundOld)
					break;
			}
			boolean successful = next(null, prev, i);
			if (successful) {
				if (pieces[place].digit == replacement.oldPiece)
					result = getChild(replacement, place);
			} else {
				break;
			}
		}
		unhash(curHash);
		return result;
	}

	public long getChild(char old, char replace, int place) {
		Replacement replacement = null;
		int i;
		for (i = 0; i < replacementSize; i++) {
			replacement = replacements[i];
			if (digits[replacement.oldPiece] == old
					&& digits[replacement.newPiece] == replace)
				break;
		}
		if (i == replacementSize)
			throw new Error("No such replacement set");
		return getChild(replacement, place);
	}

	private long getChild(Replacement replacement, int i) {
		long hashDif = 0L;
		int numReplacements = numType.get(replacement.oldPiece);
		for (int groupNum = numReplacements - 1; groupNum >= 0; groupNum--) {
			Replacement.Group group = replacement.groups[groupNum];
			hashDif += group.dif;
			if (group.bottom == i)
				return hash + hashDif + group.moveDif;
		}
		return -1L;
	}

	public long getHash() {
		return hash;
	}

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
		DartboardHasher dh = new DartboardHasher(9, ' ', 'O', 'X');
		DartboardHasher dh2 = new DartboardHasher(9, ' ', 'O', 'X');
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

	public void set(int boardNum, char p) {
		pieces[boardNum].digit = findDigit(p);
		rehash(true);
	}

	public int boardSize() {
		return pieces.length;
	}

	public boolean majorChanged() {
		return true;
	}
}