package edu.berkeley.gamesman.hasher;

public class PermutationHash {
	// Jython testing code
	// from edu.berkeley.gamesman.hasher import PermutationHash
	// from org.python.core import PyLong
	// p=PermutationHash(3)
	// [p.unhash(PyLong(i)) for i in range(p.maxHash().intValue()+1)]
	private final int permutationLength;

	private final int[] ident;

	private final int[] clone;

	private final long[] FACTORIAL;

	private final boolean evenPermutation;

	public PermutationHash(int permutationLength, boolean evenPermutation) {
		this.permutationLength = permutationLength;
		this.evenPermutation = evenPermutation;
		ident = new int[permutationLength];
		clone = new int[permutationLength];
		FACTORIAL = new long[permutationLength + 1];
		FACTORIAL[0] = 1;
		for (int i = 1; i < FACTORIAL.length; i++)
			FACTORIAL[i] = i * FACTORIAL[i - 1];
		if (evenPermutation)
			for (int i = 0; i < FACTORIAL.length; i++)
				FACTORIAL[i] /= 2;
	}

	public boolean isEven(int[] pieces) {
		for (int i = 0; i < pieces.length; i++)
			clone[i] = pieces[i];
		int count = 0;
		for (int i = 0; i < pieces.length; i++) {
			if (clone[i] != i) {
				count++;
				int j;
				for (j = i + 1; j < pieces.length; j++)
					if (clone[j] == i)
						break;
				clone[j] = clone[i]; // swap 'em
				clone[i] = i;
			}
		}
		// basically, we're counting the number of swaps to
		// get back to the identity permutation, so if we had
		// an even number of swaps, we have an even permutation
		return (count & 1) == 0;
	}

	// public static void main(String[] args) {
	// PermutationHash ph = new PermutationHash(4, true);
	// for (BigInteger h : Util.bigIntIterator(ph.numHashes().subtract(
	// BigInteger.ONE))) {
	// Integer[] unhash1 = Util.toArray(ph.unhash(h));
	// System.out.print(Arrays.toString(unhash1));
	// System.out.println(" " + ph.hash(unhash1) + " "
	// + ph.isEven(unhash1));
	// assert ph.isEven(unhash1);
	// assert ph.hash(unhash1).equals(h);
	// }
	// }

	private void resetIdentityPermutation() {
		for (int i = 0; i < permutationLength; i++)
			ident[i] = i;
	}

	public long hash(int[] pieces) {
		resetIdentityPermutation();
		int identLen = ident.length;
		// assert new HashSet<Integer>(Arrays.asList(pieces)).equals(ident);
		long hash = 0L;
		int last = evenPermutation ? permutationLength - 2
				: permutationLength - 1;
		for (int i = 0; i < last; i++) {
			int pos = --identLen;
			if (ident[pos] > pieces[i]) {
				int lastIdent = ident[pos];
				for (--pos; ident[pos] > pieces[i]; --pos) {
					int thisIdent = ident[pos];
					ident[pos] = lastIdent;
					lastIdent = thisIdent;
				}
				ident[pos] = lastIdent;
			}
			hash = hash + FACTORIAL[identLen] * pos;
		}
		return hash;
	}

	public void unhash(long hash, int[] pieces) {
		resetIdentityPermutation();
		int identLen = ident.length;
		int last = evenPermutation ? permutationLength - 2 : permutationLength;
		for (int i = 0; i < last; i++) {
			long fact = FACTORIAL[identLen - 1];
			int location = (int) (hash / fact);
			hash = hash % fact;
			pieces[i] = ident[location];
			identLen--;
			for (int c = location; c < identLen; c++) {
				ident[c] = ident[c + 1];
			}
		}
		if (evenPermutation) {
			assert identLen == 2;
			pieces[last] = ident[0];
			pieces[last + 1] = ident[1];
			// is there a better way of doing this?
			// it could be merged with the above loop,
			// but that wouldn't make this it any faster
			if (!isEven(pieces)) {
				int p = pieces[last];
				pieces[last] = pieces[last + 1];
				pieces[last + 1] = p;
			}
			assert isEven(pieces);
		}
	}

	public long numHashes() {
		return FACTORIAL[permutationLength];
	}
}
