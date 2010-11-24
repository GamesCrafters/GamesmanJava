package edu.berkeley.gamesman.hasher;

import java.util.Scanner;

import edu.berkeley.gamesman.util.CoefTable;

public final class RearrangeHasher {
	private DartboardHasher xHash;
	private DartboardHasher oHash;
	private DartboardHasher[] xMinor;
	private DartboardHasher[] oMinor;
	private int length;
	private char[] board;
	private char[] majorBoard;
	private char[][] minorboard;
	private int numx;
	private int numo;
	private int nums;
	private boolean OX = false; // true means X's turn
	private ChangedIterator c;
	private ChangedIterator d;
	private long hash = 0;
	private final CoefTable ct;
	private boolean majorChanged = true;

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		RearrangeHasher rh = new RearrangeHasher(5);
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
		// long[] children = new long[10];
		// children[9] = 0;
		// rh.getChildren(' ', 'O', children);
		// int i = 0;
		/*
		 * while(children[i]!=0){ char[] childboard = new char[9];
		 * rh.unhash(children[i]); rh.getCharArray(childboard);
		 * //System.out.println(childboard); i++; }
		 */
		char[] childboard = new char[9];
		// long startTimeMs = System.currentTimeMillis();
		// int nexttime = 86574831;
		// int nexttime = 1000000;
		// int times = 1;
		// int k = 0;
		// int j = 0;
		/*
		 * while(j<times){ while(k < nexttime){ rh.next(); rh.getChildren(' ',
		 * 'O', children); k++; } j++; }
		 */
		// long taskTimeMs = System.currentTimeMillis() - startTimeMs;
		rh.getCharArray(childboard);
		// System.out.println(childboard);
		// System.out.println(taskTimeMs);
	}

	public RearrangeHasher(int len) {
		xHash = new DartboardHasher(len, ' ', 'X');
		oHash = new DartboardHasher(len, ' ', 'O');
		length = len;
		board = new char[len];
		xMinor = new DartboardHasher[len + 1];
		oMinor = new DartboardHasher[len + 1];
		minorboard = new char[len + 1][];
		c = new ChangedIterator();
		d = new ChangedIterator();
		majorBoard = new char[len];

		for (int i = 0; i < len + 1; i++) {
			xMinor[i] = new DartboardHasher(i, ' ', 'X');
			oMinor[i] = new DartboardHasher(i, ' ', 'O');
			minorboard[i] = new char[i];
		}
		ct = new CoefTable();
	}

	public long numHashes() {
		return ct.get(length, numx + numo) * ct.get(numx + numo, numx);
	}

	public char get(int i) {
		return board[i];
	}

	public void setNums(int spaces, int o, int x) {
		int i = 0;
		if (OX) {
			for (i = 0; i < x; i++) {
				board[i] = 'O';
			}
			for (; i < x + o; i++) {
				board[i] = 'X';
			}
		} else {
			for (i = 0; i < x; i++) {
				board[i] = 'X';
			}
			for (; i < x + o; i++) {
				board[i] = 'O';
			}
		}
		for (; i < length; i++) {
			board[i] = ' ';
		}
		numo = o;
		numx = x;
		nums = spaces;
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
		majorChanged = true;
	}

	public long hash(char[] board) {
		// System.out.println(board[100]);
		majorChanged = true;
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

	private long xhash(char[] board) {
		int cnumx = 0;
		int cnumo = 0;
		int sizeOfMinor = 0;
		for (int i = 0; i < board.length; i++) {
			if (board[i] == 'X') {
				majorBoard[i] = 'X';
				cnumx++;
			} else if (board[i] == 'O') {
				majorBoard[i] = ' ';
				minorboard[length][sizeOfMinor] = 'O';
				cnumo++;
				sizeOfMinor++;
			} else {
				majorBoard[i] = ' ';
				minorboard[length][sizeOfMinor] = ' ';
				sizeOfMinor++;
			}
		}
		for (int i = 0; i < sizeOfMinor; i++) {
			minorboard[sizeOfMinor][i] = minorboard[length][i];
		}
		majorChanged = true;
		return xHash.hash(majorBoard) * ct.get(length - cnumx, cnumo)
				+ oMinor[length - cnumx].hash(minorboard[sizeOfMinor]);
	}

	private long ohash(char[] board) {
		int cnumx = 0;
		int cnumo = 0;
		int k = 0;
		for (int i = 0; i < board.length; i++) {
			if (board[i] == 'O') {
				majorBoard[i] = 'O';
				cnumo++;
			} else if (board[i] == 'X') {
				majorBoard[i] = ' ';
				minorboard[length][k] = 'X';
				cnumx++;
				k++;
			} else {
				majorBoard[i] = ' ';
				minorboard[length][k] = ' ';
				k++;
			}
		}
		for (int i = 0; i < k; i++) {
			minorboard[k][i] = minorboard[length][i];
		}
		majorChanged = true;
		return oHash.hash(majorBoard) * ct.get(length - cnumo, cnumx)
				+ xMinor[length - cnumo].hash(minorboard[k]);
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

		int s;
		if (i % 2 == 0) { // X's turn
			o = i / 2; // number of o's
			s = length - o;
			maj = 'O';
			min = 'X';
			majorHash = oHash;
			minorHash = xMinor[s];

			numo = o;
			numx = i - o;
			OX = true;
		} else {
			o = (i + 1) / 2; // number of x's
			s = length - o;
			maj = 'X';
			min = 'O';
			majorHash = xHash;
			minorHash = oMinor[s];

			numx = o;
			numo = i - o;
			OX = false;
		}
		int numotherpiece = i - o;
		// majorHash.setNums(s, o);
		// minorHash.setNums(s - numotherpiece, numotherpiece);

		long major = hash / (ct.get(s, numotherpiece));
		long minor = hash % (ct.get(s, numotherpiece));
		majorHash.unhash(major);
		majorHash.getCharArray(majorBoard);
		minorHash.unhash(minor);
		minorHash.getCharArray(minorboard[s]);
		// System.out.print("mboard = ");
		// System.out.println(mboard[s]);

		// System.out.print("mboard = ");
		// System.out.println(mboard[s]);
		int j = 0;
		for (int k = 0; k < length; k++) {
			if (majorBoard[k] == maj) {
				board[k] = maj;
			} else if (minorboard[s][j] == min) {
				board[k] = min;
				j++;
			} else {
				board[k] = ' ';
				j++;
			}
		}
		majorChanged = true;
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
			} else if (pieces[k] == 'O') {
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
		int newmL = length - newM;
		int intmL = length - intM;
		char cNew = (OX ? 'X' : 'O');
		char cOld = (OX ? 'O' : 'X');
		long majorValue = 0;
		long minorValue = 0;

		for (int i = 0, j = 0, M = 0, m = 0; i < length; i++) {
			if (board[i] == cNew) {
				majorBoard[i] = cNew;
				majorValue += ct.get(i, M + 1);
				M++;
			} else if (board[i] == cOld) {
				majorBoard[i] = ' ';
				minorboard[intmL][j] = cOld;
				minorValue += ct.get(j, m + 1);
				j++;
				m++;
			} else if (board[i] == ' ') {
				majorBoard[i] = ' ';
				minorboard[intmL][j] = ' ';
				j++;
			}
		}

		if ((length - intM - intm) > 0) {
			for (int majorIndex = length - 1, minorIndex = intmL - 1, childIndex = length - 1, M = newM, m = newm; majorIndex >= 0; majorIndex--) {
				if (majorBoard[majorIndex] == cNew) {
					majorValue += ct.get(majorIndex, M)
							- ct.get(majorIndex, M - 1);
					M--;
					// System.out.println(majorValue);
					childArray[childIndex] = -1;
					childIndex--;
				} else if (majorBoard[majorIndex] == ' ') {
					if (minorboard[intmL][minorIndex] == cOld) {
						minorValue += ct.get(minorIndex - 1, m)
								- ct.get(minorIndex, m);
						m--;
						childArray[childIndex] = -1;
						childIndex--;
					} else if (minorboard[intmL][minorIndex] == ' ') {
						majorValue += ct.get(majorIndex, M); // Add a new
						// piece
						// to the major
						// array
						minorValue += 0; // Nothing needed here since we are not
						// adding pieces to the minor array
						childArray[childIndex] = majorValue
								* ct.get(newmL, newm) + minorValue;
						// Stores new childboard in childArray
						// System.out.println(childArray[childIndex]);
						childIndex--;
						majorValue -= ct.get(majorIndex, M);
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
		char Mchar;
		int countmin;
		int countmax;
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
			Mchar = 'O';
			countmin = numx;
			countmax = numo;
		} else {
			minor = oMinor[length - numx];
			major = xHash;
			Mchar = 'X';
			countmin = numo;
			countmax = numx;
		}
		long minorRearrangements = ct.get(length - countmax, countmin);
		if (minor.getHash() == minorRearrangements - 1) {
			majorChanged = true;
			minor.setNums(nums, countmin);
			long majorRearrangements = ct.get(length, countmax);
			if (major.getHash() == majorRearrangements - 1)
				hash--;
			else
				major.next(d);
			while (d.hasNext()) {
				int n = d.next();
				board[n] = major.get(n);
				if (changed != null)
					changed.add(n);
			}
		} else {
			minor.next(c);
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
	}

	public boolean majorChanged() {
		if (majorChanged) {
			majorChanged = false;
			return true;
		} else
			return false;
	}
}
/*
 * for (int majorIndex = length - 1, oldMinorIndex = oldmL - 1, intMinorIndex =
 * intmL - 1, M = intM; majorIndex >= 0; majorIndex--) { if (Mboard[majorIndex]
 * == cOld) { Mboard[majorIndex] = ' '; mboard[intmL][intMinorIndex] = cOld;
 * minorValue += ct.get(intMinorIndex, intm); intMinorIndex--; } else if
 * (Mboard[majorIndex] == ' ') { if (mboard[oldmL][oldMinorIndex] == cNew) {
 * Mboard[majorIndex] = cNew; majorValue += ct.get(majorIndex, M); M--;
 * oldMinorIndex--; } else if (mboard[oldmL][oldMinorIndex] == ' ') {
 * intMinorIndex--; oldMinorIndex--; }
 * 
 * } }
 */
