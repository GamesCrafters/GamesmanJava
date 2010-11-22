package edu.berkeley.gamesman.hasher;

import java.util.Scanner;

import edu.berkeley.gamesman.util.Util;

public final class RearrangeHasher {
	private DartboardHasher xHash;
	private DartboardHasher oHash;
	private DartboardHasher[] xMinor;
	private DartboardHasher[] oMinor;
	private int length;
	private char[] chars;
	private char[] board;
	private char[] Mboard;
	private char[][] mboard;
	private int numx;
	private int numo;
	private int nums;
	private boolean OX = false; // true means X's turn
	private int[] majorChildren;
	private ChangedIterator c;
	private ChangedIterator d;
	private long hash = 0;
	private char xChar;
	private char oChar;

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		RearrangeHasher rh = new RearrangeHasher(5, ' ', 'O', 'X');
		int nums = 1;
		int numo = 2;
		int numx = 2;
		rh.setNums(nums, numo, numx);
		long[] q = new long[9];
		char[] c = "OOXX ".toCharArray();
		rh.hash(c);
		rh.next();
		rh.getChildren(' ', ' ', q);
		rh.printHash(q);
		rh.printBoard(q);
		rh.getCharArray(c);
		System.out.println(c);
		while (sc.next().equals("0")) {
			rh.next();
			long saved = rh.getHash();
			rh.getChildren(' ', ' ', q);
			rh.getCharArray(c);
			rh.printBoard(c);
			System.out.println();
			if (numo == numx) {
				rh.setNums(nums - 1, numo, numx + 1);
			} else {
				rh.setNums(nums - 1, numo + 1, numx);
			}
			rh.printHash(q);
			System.out.println();
			rh.printBoard(q);
			rh.setNums(nums, numo, numx);
			rh.unhash(saved);
		}
		System.out.println("done");
		long[] children = new long[10];
		// children[9] = 0;
		// rh.getChildren(' ', oChar, children);
		int i = 0;
		/*
		 * while(children[i]!=0){ char[] childboard = new char[9];
		 * rh.unhash(children[i]); rh.getCharArray(childboard);
		 * //System.out.println(childboard); i++; }
		 */
		char[] childboard = new char[9];
		long startTimeMs = System.currentTimeMillis();
		// int nexttime = 86574831;
		int nexttime = 1000000;
		int times = 1;
		int k = 0;
		int j = 0;
		/*
		 * while(j<times){ while(k < nexttime){ rh.next(); rh.getChildren(' ',
		 * oChar, children); k++; } j++; }
		 */
		long taskTimeMs = System.currentTimeMillis() - startTimeMs;
		rh.getCharArray(childboard);
		// System.out.println(childboard);
		// System.out.println(taskTimeMs);
	}

	public RearrangeHasher(int len, char... digits) {
		oChar = digits[1];
		xChar = digits[2];
		xHash = new DartboardHasher(len, ' ', xChar);
		oHash = new DartboardHasher(len, ' ', oChar);
		length = len;
		chars = new char[] { xChar, 'p', oChar, 'q' };
		board = new char[len];
		xMinor = new DartboardHasher[len + 1];
		oMinor = new DartboardHasher[len + 1];
		mboard = new char[len + 1][];
		majorChildren = new int[len];
		c = new ChangedIterator();
		d = new ChangedIterator();
		Mboard = new char[len];

		for (int i = 0; i < len + 1; i++) {
			xMinor[i] = new DartboardHasher(i, ' ', xChar);
			oMinor[i] = new DartboardHasher(i, ' ', oChar);
			mboard[i] = new char[i];
		}

	}

	public long numHashes() {
		return Util.nCr(nums, numx + numo) * Util.nCr(numx + numo, numx);
	}

	public char get(int i) {
		return board[i];
	}

	public void setNums(int spaces, int o, int x) {
		int i = 0;
		for (i = 0; i < x; i++) {
			board[i] = xChar;
		}
		for (; i < x + o; i++) {
			board[i] = oChar;
		}
		for (; i < length; i++) {
			board[i] = ' ';
		}
		numo = o;
		numx = x;
		if (o == x) {
			oHash.setNums(length - o, o);
			xHash.setNums(length - x - 1, x); // for getChildren
			// oMinor[length - x - 1].setNums(spaces - 1, o); // for getChildren
			oMinor[length - x].setNums(spaces, o);
			xMinor[length - o].setNums(spaces, x);
			OX = true;
		} else {
			xHash.setNums(length - x, x);
			oHash.setNums(length - o - 1, o); // for getChildren
			// xMinor[length - o - 1].setNums(spaces - 1, x); // for getChildren
			xMinor[length - o].setNums(spaces, x);
			oMinor[length - x].setNums(spaces, o);
			OX = false;
		}
		hash = 0L;
	}

	public long hash(char[] board) {
		// System.out.println(board[100]);
		if (OX) {
			long hVal = ohash(board);
			unhash(hVal);
			return hVal;
		} else {
			long hVal = xhash(board);
			unhash(hVal);
			return hVal;
		}
	}

	public long xhash(char[] board) {
		int cnumx = 0;
		int cnumo = 0;
		int cnums = 0;
		int sizeOfMinor = 0;
		for (int i = 0; i < board.length; i++) {
			if (board[i] == xChar) {
				Mboard[i] = xChar;
				cnumx++;
			} else if (board[i] == oChar) {
				Mboard[i] = ' ';
				mboard[length][sizeOfMinor] = oChar;
				cnumo++;
				sizeOfMinor++;
			} else {
				Mboard[i] = ' ';
				mboard[length][sizeOfMinor] = ' ';
				sizeOfMinor++;
			}
		}
		for (int i = 0; i < sizeOfMinor; i++) {
			mboard[sizeOfMinor][i] = mboard[length][i];
		}
		return xHash.hash(Mboard) * Util.nCr(length - cnumx, cnumo)
				+ oMinor[length - cnumx].hash(mboard[sizeOfMinor]);
	}

	public long ohash(char[] board) {
		int cnumx = 0;
		int cnumo = 0;
		int cnums = 0;
		int k = 0;
		for (int i = 0; i < board.length; i++) {
			if (board[i] == oChar) {
				Mboard[i] = oChar;
				cnumo++;
			} else if (board[i] == xChar) {
				Mboard[i] = ' ';
				mboard[length][k] = xChar;
				cnumx++;
				k++;
			} else {
				Mboard[i] = ' ';
				mboard[length][k] = ' ';
				k++;
			}
		}
		for (int i = 0; i < k; i++) {
			mboard[k][i] = mboard[length][i];
		}
		return oHash.hash(Mboard) * Util.nCr(length - cnumo, cnumx)
				+ xMinor[length - cnumo].hash(mboard[k]);
	}

	public long getHash() {
		return hash;
	}

	public void unhash(long hash) {
		int i = numo + numx;
		this.hash = hash;
		// System.out.println(hash);
		// System.out.println(hash);
		int o;
		char maj;
		char min;
		DartboardHasher majorHash;
		DartboardHasher minorHash;
		DartboardHasher otherMajorHash;
		DartboardHasher otherMinorHash;

		int s;
		if (i % 2 == 0) { // X's turn
			o = i / 2; // number of o's
			s = length - o;
			maj = oChar;
			min = xChar;
			majorHash = oHash;
			minorHash = xMinor[s];
			otherMajorHash = xHash;
			otherMinorHash = oMinor[length - i + o];

			numo = o;
			numx = i - o;
			OX = true;
		} else {
			o = (i + 1) / 2; // number of x's
			s = length - o;
			maj = xChar;
			min = oChar;
			majorHash = xHash;
			minorHash = oMinor[s];
			otherMajorHash = oHash;
			otherMinorHash = xMinor[length - i + o];

			numx = o;
			numo = i - o;
			OX = false;
		}
		int numotherpiece = i - o;
		// majorHash.setNums(s, o);
		// minorHash.setNums(s - numotherpiece, numotherpiece);

		long major = hash / (Util.nCr(s, numotherpiece));
		long minor = hash % (Util.nCr(s, numotherpiece));
		majorHash.unhash(major);
		majorHash.getCharArray(Mboard);
		minorHash.unhash(minor);
		minorHash.getCharArray(mboard[s]);
		// System.out.print("mboard = ");
		// System.out.println(mboard[s]);

		// System.out.print("mboard = ");
		// System.out.println(mboard[s]);
		int j = 0;
		for (int k = 0; k < length; k++) {
			if (Mboard[k] == maj) {
				board[k] = maj;
			} else if (mboard[s][j] == min) {
				board[k] = min;
				j++;
			} else {
				board[k] = ' ';
				j++;
			}
		}
		// System.out.println("unhash: ");
		// System.out.println(mboard[s]);
	}

	public long setNumsAndHash(char[] pieces) {
		numo = 0;
		numx = 0;
		nums = 0;
		for (int k = 0; k < pieces.length; k++) {
			if (pieces[k] == ' ') {
				nums++;
			} else if (pieces[k] == oChar) {
				numo++;
			} else
				numx++;

		}
		setNums(nums, numo, numx);

		return hash(board);
	}

	public void printBoard(char[] majorBoard, char[] minorBoard) {
		for (int majorIndex = 0, minorIndex = 0; majorIndex < length; majorIndex++) {
			if (majorBoard[majorIndex] == ' ') {
				if (minorBoard[minorIndex] == ' ') {
					System.out.print("-");
				} else
					System.out.print(minorBoard[minorIndex]);
				minorIndex++;
			} else
				System.out.print(majorBoard[majorIndex]);

		}
	}

	public void printBoard(char[] board) {
		for (int i = 0; i < board.length; i++) {
			System.out.print((board[i] == ' ') ? "-" : board[i]);
		}
	}

	public void printBoard(long[] array) {
		System.out.print("[");
		for (int i = 0; i < array.length - 1; i++) {
			if (array[i] != -1) {
				unhash(array[i]);
				printBoard(board);
			}
			System.out.print(" ");
		}
		unhash(array[array.length - 1]);
		printBoard(board);
		System.out.print("]");
		System.out.println();
	}

	public void printHash(long[] array) {
		System.out.print("[");
		for (int i = 0; i < array.length - 1; i++) {
			System.out.print(array[i] + " ");
		}
		System.out.print(array[array.length - 1]);
		System.out.print("]");
	}

	public void getChildren(char old, char replace, long[] childArray) {
		int newM = (OX ? numx + 1 : numo + 1);
		int newm = (OX ? numo : numx);
		int intM = (OX ? numx : numo);
		int intm = (OX ? numo : numx);
		int oldM = (OX ? numo : numx);
		int oldm = (OX ? numx : numo);
		int newmL = length - newM;
		int intmL = length - intM;
		int oldmL = length - oldM;
		char cNew = (OX ? xChar : oChar);
		char cOld = (OX ? oChar : xChar);
		DartboardHasher majorHash = (OX ? oHash : xHash);
		DartboardHasher minorHash[] = (OX ? xMinor : oMinor);
		long majorValue = 0;
		long minorValue = 0;

		for (int i = 0, j = 0, M = 0, m = 0; i < length; i++) {
			if (board[i] == cNew) {
				Mboard[i] = cNew;
				majorValue += Util.nCr(i, M + 1);
				M++;
			} else if (board[i] == cOld) {
				Mboard[i] = ' ';
				mboard[intmL][j] = cOld;
				minorValue += Util.nCr(j, m + 1);
				j++;
				m++;
			} else if (board[i] == ' ') {
				Mboard[i] = ' ';
				mboard[intmL][j] = ' ';
				j++;
			}
		}

		if ((length - intM - intm) > 0) {
			for (int majorIndex = length - 1, minorIndex = intmL - 1, childIndex = length - 1, M = newM, m = newm; majorIndex >= 0; majorIndex--) {
				if (Mboard[majorIndex] == cNew) {
					majorValue += Util.nCr(majorIndex, M)
							- Util.nCr(majorIndex, M - 1);
					M--;
					// System.out.println(majorValue);
					childArray[childIndex] = -1;
					childIndex--;
				} else if (Mboard[majorIndex] == ' ') {
					if (mboard[intmL][minorIndex] == cOld) {
						minorValue += Util.nCr(minorIndex - 1, m)
								- Util.nCr(minorIndex, m);
						m--;
						childArray[childIndex] = -1;
						childIndex--;
					} else if (mboard[intmL][minorIndex] == ' ') {
						majorValue += Util.nCr(majorIndex, M); // Add a new
						// piece
						// to the major
						// array
						minorValue += 0; // Nothing needed here since we are not
						// adding pieces to the minor array
						childArray[childIndex] = majorValue
								* Util.nCr(newmL, newm) + minorValue;
						// Stores new childboard in childArray
						// System.out.println(childArray[childIndex]);
						childIndex--;
						majorValue -= Util.nCr(majorIndex, M);
					}
					minorIndex--;
				} else
					throw new Error("GetChildren Error");
			}
		} else if ((length - intM - intm) == 0) {
			for (int i = 0; i < childArray.length; i++) {
				childArray[i] = -1;
			}
		} else
			throw new Error("Too many pieces!");
		// System.out.println(board);
	}

	public void setReplacements(char[] replacements) {
		if (OX) {
			oHash.setReplacements(' ', oChar);
			oMinor[length - numx].setReplacements(' ', oChar);

		} else
			xHash.setReplacements(' ', xChar);
		xMinor[length - numo].setReplacements(' ', xChar);

	}

	public void getCharArray(char[] copyTo) {
		if (copyTo.length != board.length)
			throw new Error("Wrong length char array");
		for (int i = 0; i < board.length; i++) {
			copyTo[i] = board[i];
		}
	}

	/**
	 * @param changed
	 */
	public void next() {
		next(null);
	}

	public void next(ChangedIterator changed) {
		DartboardHasher minor;
		DartboardHasher major;
		char Mchar, mchar;
		char[] minboard;
		int countmin;
		hash++;
		c.reset();
		d.reset();
		if (changed == null) {
			changed = c;
		} else {
			changed.reset();
		}
		if (OX) {
			minor = xMinor[length - numo];
			major = oHash;
			Mchar = oChar;
			countmin = numx;
			mchar = xChar;
			minboard = mboard[length - numo];
		} else {
			minor = oMinor[length - numx];
			major = xHash;
			Mchar = xChar;
			countmin = numo;
			mchar = oChar;
			minboard = mboard[length - numx];
		}
		// System.out.print("prev minor: ");
		// System.out.println(mboard[length-numo]);
		long prevmin = minor.getHash();
		minor.next(c);
		// System.out.print("next minor: ");
		// System.out.println(minboard);
		if (minor.getHash() == 0) {
			minor.setNums(nums, countmin);
			long prevmaj = major.getHash();
			major.next(d);
			if (major.getHash() == 0)
				hash--;
			while (d.hasNext()) {
				int n = d.next();
				board[n] = major.get(n);
				if (changed != null)
					changed.add(n);
			}
		}
		int smallcounter = 0;
		for (int a = 0; a < length; a++) {
			if (major.get(a) == Mchar)
				board[a] = Mchar;
			else if (minor.get(smallcounter) != board[a]) {
				changed.add(a);
				board[a] = minor.get(smallcounter);
				smallcounter++;
			} else {
				smallcounter++;
			}
		}

		/*
		 * int i = -1; int j = 0;
		 * xMinor[length-numo].getCharArray(mboard[length-numo]);
		 * System.out.print("next minorx: ");
		 * System.out.println(mboard[length-numo]); while (c.hasNext()) { int n
		 * = c.next(); //mboard[length - numx][n] = oMinor[length -
		 * numx].get(n); while(true){ if(Mboard[j]==' '){ i++; if(n==i) break; }
		 * j++; } System.out.print(n); System.out.println(j);
		 * board[j]=minor.get(n); if(changed!=null) changed.add(j); j++; }
		 */
	}

	public void setx() {
		OX = false;
		return;
	}

	public void seto() {
		OX = true;
		return;
	}
}
/*
 * for (int majorIndex = length - 1, oldMinorIndex = oldmL - 1, intMinorIndex =
 * intmL - 1, M = intM; majorIndex >= 0; majorIndex--) { if (Mboard[majorIndex]
 * == cOld) { Mboard[majorIndex] = ' '; mboard[intmL][intMinorIndex] = cOld;
 * minorValue += Util.nCr(intMinorIndex, intm); intMinorIndex--; } else if
 * (Mboard[majorIndex] == ' ') { if (mboard[oldmL][oldMinorIndex] == cNew) {
 * Mboard[majorIndex] = cNew; majorValue += Util.nCr(majorIndex, M); M--;
 * oldMinorIndex--; } else if (mboard[oldmL][oldMinorIndex] == ' ') {
 * intMinorIndex--; oldMinorIndex--; }
 * 
 * } }
 */
