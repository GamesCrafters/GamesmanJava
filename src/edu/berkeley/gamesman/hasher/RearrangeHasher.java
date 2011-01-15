package edu.berkeley.gamesman.hasher;

import java.util.Arrays;

import edu.berkeley.gamesman.util.CoefTable;

public final class RearrangeHasher extends DartboardHasher {
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
		// Scanner sc = new Scanner(System.in);
		RearrangeHasher rh = new RearrangeHasher(13);
		int nums = 5;
		int numo = 4;
		int numx = 4;
		rh.setNums(nums, numo, numx);
		long[] q = new long[5];
		char[] c = "XXXXOOOO     ".toCharArray();
		System.out.println(rh.hash(c));
		rh.getChildren(' ', ' ', q);
		System.out.println(Arrays.toString(q));
		return;
	}

	public RearrangeHasher(int len, char... types) {
		super(len, ' ', 'O', 'X');
		if (!Arrays.equals(types, new char[] { ' ', 'O', 'X' }))
			throw new Error("Types are not correct");
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

	@Override
	public long numHashes() {
		return ct.get(length, numx + numo) * ct.get(numx + numo, numx);
	}

	@Override
	public char get(int i) {
		return board[i];
	}

	@Override
	public int boardSize() {
		return length;
	}

	@Override
	public void set(int index, char turn) {
		board[index] = turn;
	}

	@Override
	public void setNums(int... types) {
		int spaces = types[0], o = types[1], x = types[2];
		int i = 0;
		if (o == x) {
			for (i = 0; i < o; i++) {
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

			oMinor[length - x].setNums(spaces, o);
			xMinor[length - o].setNums(spaces, x);
			OX = true;
		} else {
			xHash.setNums(length - x, x);
			oHash.setNums(length - o - 1, o); // for getChildren

			xMinor[length - o].setNums(spaces, x);
			oMinor[length - x].setNums(spaces, o);
			OX = false;
		}
		hash = 0L;
		majorChanged = true;
	}

	@Override
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

	@Override
	public long getHash() {
		return hash;
	}

	@Override
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

		long major = hash / (ct.get(s, numotherpiece));
		long minor = hash % (ct.get(s, numotherpiece));
		majorHash.unhash(major);
		majorHash.getCharArray(majorBoard);
		minorHash.unhash(minor);
		minorHash.getCharArray(minorboard[s]);

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

	@Override
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

	@Override
	public int getChildren(char old, char replace, int[] places,
			long[] childArray) {
		/*
		 * int newM = (OX ? numx + 1 : numo + 1); int newm = (OX ? numo : numx);
		 * int intM = (OX ? numx : numo); int intm = (OX ? numo : numx);
		 */
		/*
		 * char cNew = (OX ? 'X' : 'O'); char cOld = (OX ? 'O' : 'X');
		 */
		long majorValue = 0;
		long minorValue = 0;

		int newM, newm, intM, intm;
		char cNew, cOld;

		if (OX) {
			newM = numx + 1;
			newm = numo;
			intM = numx;
			intm = numo;
			cNew = 'X';
			cOld = 'O';
		} else {
			newM = numo + 1;
			newm = numx;
			intM = numo;
			intm = numx;
			cNew = 'O';
			cOld = 'X';
		}

		int newmL = length - newM;
		int intmL = length - intM;

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

		int c = nums - 1;

		if ((length - intM - intm) > 0) {
			for (int majorIndex = length - 1, minorIndex = intmL - 1, childIndex = length - 1, M = newM, m = newm; majorIndex >= 0; majorIndex--) {
				if (majorBoard[majorIndex] == cNew) {
					majorValue += ct.get(majorIndex, M)
							- ct.get(majorIndex, M - 1);
					M--;
					childIndex--;
				} else if (majorBoard[majorIndex] == ' ') {
					if (minorboard[intmL][minorIndex] == cOld) {
						minorValue += ct.get(minorIndex - 1, m)
								- ct.get(minorIndex, m);
						m--;
						childIndex--;
					} else if (minorboard[intmL][minorIndex] == ' ') {
						majorValue += ct.get(majorIndex, M); // Add a new
						// piece
						// to the major
						// array
						minorValue += 0;
						if (places != null)
							places[c] = childIndex;
						childArray[c] = majorValue * ct.get(newmL, newm)
								+ minorValue;
						c--;
						childIndex--;
						majorValue -= ct.get(majorIndex, M);
					}
					minorIndex--;
				} else
					throw new Error("GetChildren Error");
			}
		} else if ((length - intM - intm) != 0)
			throw new Error("Too many pieces!");
		return nums;
	}

	@Override
	public void getCharArray(char[] copyTo) {
		if (copyTo.length != board.length)
			throw new Error("Wrong length char array");
		for (int i = 0; i < board.length; i++) {
			copyTo[i] = board[i];
		}
	}

	/**
	 * @param changed
	 * @return
	 */
	@Override
	public boolean next() {
		return next(null);
	}

	@Override
	public boolean next(ChangedIterator changed) {
		DartboardHasher minor;
		DartboardHasher major;
		char Mchar;
		int countmin;
		int countmax;
		boolean advanced = true;
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
			if (major.getHash() == majorRearrangements - 1) {
				hash--;
				advanced = false;
			} else
				major.next(d);
			while (d.hasNext()) {
				int n = d.next();
				board[n] = major.get(n);
				if (changed != null)
					changed.add(n);
			}
		} else {
			majorChanged = false;
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
		return advanced;
	}

	@Override
	public boolean majorChanged() {
		return majorChanged;
	}

	@Override
	public void setReplacements(char... replacements) {
	}

	@Override
	public void nextChildren(char old, char replace, long[] childArray) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void previousChildren(char old, char replace, long[] childArray) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		char[] stringArr = new char[length];
		getCharArray(stringArr);
		return new String(stringArr);
	}
}
