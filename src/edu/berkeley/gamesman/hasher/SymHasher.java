package edu.berkeley.gamesman.hasher;

import java.util.Arrays;
import java.util.LinkedList;

import edu.berkeley.gamesman.util.Util;

public class SymHasher {
	private static class DeepArray {
		final DeepArray[] inner;
		final long value;

		public DeepArray(DeepArray[] inner, boolean start) {
			this.inner = Arrays.copyOf(inner, inner.length);
			if (start) {
				long value = 0L;
				for (DeepArray da : inner)
					if (da != null) {
						value = da.value;
						break;
					}
				this.value = value;
			} else {
				long totalValue = 0L;
				for (DeepArray da : inner) {
					if (da != null)
						totalValue += da.value;
				}
				value = totalValue;
			}
		}

		public DeepArray(long value) {
			this.value = value;
			inner = null;
		}

		@Override
		public String toString() {
			if (inner != null)
				return Arrays.toString(inner);
			else
				return Long.toString(value);
		}
	}

	private final DeepArray[] offsets = new DeepArray[16];
	private final long[] tierSizes = new long[16];
	private final long totalSize;

	public SymHasher() {
		DeepArray[] sizes = new DeepArray[16];
		sizes[0] = new DeepArray(1);
		for (int tier = 1; tier < 16; tier++) {
			LinkedList<Integer> used = new LinkedList<Integer>();
			used.add(0);
			sizes[tier] = hashesForRemaining(used, tier, 0);
		}
		long totalSize = 0L;
		for (int i = 0; i < 16; i++) {
			Counter c = new Counter();
			offsets[i] = sumArray(sizes[i], c);
			fixPointers(offsets[i], 0);
			tierSizes[i] = c.get();
			totalSize += Util.nCr(16, i) * tierSizes[i];
		}
		this.totalSize = totalSize;
	}

	private static void fixPointers(DeepArray offsets, int differs) {
		if (offsets.inner == null)
			return;
		switch (differs) {
		case 0:
			fixPointers(offsets, 0, 0, 15);
			fixPointers(offsets, 1, 1);
			fixPointers(offsets, 2, 3);
			fixPointers(offsets, 4, 7);
			setEquivalent(offsets, 1, 2, 4, 8);
			setEquivalent(offsets, 3, 5, 6, 9, 10, 12);
			setEquivalent(offsets, 7, 11, 13, 14);
			break;
		case 1:
			fixPointers(offsets, 1, 0, 1, 14, 15);
			fixPointers(offsets, 3, 2, 3);
			fixPointers(offsets, 5, 6, 7);
			setEquivalent(offsets, 2, 4, 8);
			setEquivalent(offsets, 3, 5, 9);
			setEquivalent(offsets, 6, 10, 12);
			setEquivalent(offsets, 7, 11, 13);
			break;
		case 2:
			fixPointers(offsets, 2, 0, 3, 12, 15);
			fixPointers(offsets, 3, 1, 13);
			fixPointers(offsets, 6, 4, 7);
			fixPointers(offsets, 7, 5);
			setEquivalent(offsets, 1, 2);
			setEquivalent(offsets, 13, 14);
			setEquivalent(offsets, 4, 8);
			setEquivalent(offsets, 7, 11);
			setEquivalent(offsets, 5, 6, 9, 10);
			break;
		case 3:
			fixPointers(offsets, 3, 0, 1, 2, 3, 12, 13, 14, 15);
			fixPointers(offsets, 7, 4, 5, 6, 7);
			setEquivalent(offsets, 4, 8);
			setEquivalent(offsets, 5, 9);
			setEquivalent(offsets, 6, 10);
			setEquivalent(offsets, 7, 11);
			break;
		case 4:
			fixPointers(offsets, 4, 0, 7, 8, 15);
			fixPointers(offsets, 5, 1, 9);
			fixPointers(offsets, 6, 3, 11);
			setEquivalent(offsets, 1, 2, 4);
			setEquivalent(offsets, 9, 10, 12);
			setEquivalent(offsets, 3, 5, 6);
			setEquivalent(offsets, 11, 13, 14);
			break;
		case 5:
			fixPointers(offsets, 5, 0, 1, 6, 7, 8, 9, 14, 15);
			fixPointers(offsets, 7, 2, 3, 10, 11);
			break;
		case 6:
			fixPointers(offsets, 6, 0, 3, 4, 7, 8, 11, 12, 15);
			fixPointers(offsets, 7, 1, 5, 9, 13);
			setEquivalent(offsets, 1, 2);
			setEquivalent(offsets, 5, 6);
			setEquivalent(offsets, 9, 10);
			setEquivalent(offsets, 13, 14);
			break;
		default:
			throw new Error(
					"Differs should only be three bits.  If 7, offsets.inner should be null");
		}
	}

	private static void setEquivalent(DeepArray offsets, int n, int... others) {
		for (int o : others)
			offsets.inner[o] = offsets.inner[n];
	}

	private static void fixPointers(DeepArray offsets, int differs,
			int... indices) {
		for (int i : indices)
			if (offsets.inner[i] != null)
				fixPointers(offsets.inner[i], differs);
	}

	private static class Counter {
		private long n;

		public Counter() {
			n = 0L;
		}

		private void add(long val) {
			n += val;
		}

		private long get() {
			return n;
		}
	}

	private static DeepArray sumArray(DeepArray da, Counter c) {
		if (da.inner == null) {
			DeepArray result = new DeepArray(c.get());
			c.add(da.value);
			return result;
		} else {
			DeepArray[] results = new DeepArray[da.inner.length];
			for (int i = 0; i < results.length; i++) {
				if (da.inner[i] != null)
					results[i] = sumArray(da.inner[i], c);
			}
			return new DeepArray(results, true);
		}
	}

	public static void main(String[] args) {
		SymHasher sh = new SymHasher();
		System.out.println(sh.totalSize);
		System.out.println(sh.hash(new int[] { 0, 1, 2, 3 }));
	}

	private static DeepArray hashesForRemaining(LinkedList<Integer> used,
			int tier, int differs) {
		if (used.size() == tier)
			return new DeepArray(1L);
		else if (differs == 7) {
			return new DeepArray(pFact(16 - used.size(), tier - used.size()));
		}
		DeepArray[] resultArr = new DeepArray[16];
		switch (differs) {
		case 0:
			tryVals(used, tier, 0, resultArr, 0, 15);
			tryVals(used, tier, 1, resultArr, 1);
			tryVals(used, tier, 2, resultArr, 3);
			tryVals(used, tier, 4, resultArr, 7);
			return new DeepArray(resultArr, false);
		case 1:
			tryVals(used, tier, 1, resultArr, 0, 1, 14, 15);
			tryVals(used, tier, 3, resultArr, 2, 3);
			tryVals(used, tier, 5, resultArr, 6, 7);
			return new DeepArray(resultArr, false);
		case 2:
			tryVals(used, tier, 2, resultArr, 0, 3, 12, 15);
			tryVals(used, tier, 3, resultArr, 1, 13);
			tryVals(used, tier, 6, resultArr, 4, 7);
			tryVals(used, tier, 7, resultArr, 5);
			return new DeepArray(resultArr, false);
		case 3:
			tryVals(used, tier, 3, resultArr, 0, 1, 2, 3, 12, 13, 14, 15);
			tryVals(used, tier, 7, resultArr, 4, 5, 6, 7);
			return new DeepArray(resultArr, false);
		case 4:
			tryVals(used, tier, 4, resultArr, 0, 7, 8, 15);
			tryVals(used, tier, 5, resultArr, 1, 9);
			tryVals(used, tier, 6, resultArr, 3, 11);
			return new DeepArray(resultArr, false);
		case 5:
			tryVals(used, tier, 5, resultArr, 0, 1, 6, 7, 8, 9, 14, 15);
			tryVals(used, tier, 7, resultArr, 2, 3, 10, 11);
			return new DeepArray(resultArr, false);
		case 6:
			tryVals(used, tier, 6, resultArr, 0, 3, 4, 7, 8, 11, 12, 15);
			tryVals(used, tier, 7, resultArr, 1, 5, 9, 13);
			return new DeepArray(resultArr, false);
		default:
			throw new Error("Differs should only be three bits");
		}
	}

	private static void tryVals(LinkedList<Integer> used, int tier,
			int differs, DeepArray[] arr, int... vals) {
		for (int i : vals) {
			if (!used.contains(i)) {
				used.push(i);
				arr[i] = hashesForRemaining(used, tier, differs);
				used.pop();
			}
		}
	}

	private static long pFact(int n, int k) {
		long total = 1L;
		for (int i = 0; i < k; i++) {
			total *= n--;
		}
		return total;
	}

	public long hash(int[] chainInts) {
		return hash(chainInts, 0, offsets[chainInts.length]);
	}

	private static long hash(int[] chainInts, int index, DeepArray offsets) {
		if (index == 0) {
			if (chainInts.length == 0 || chainInts.length == 1)
				return chainInts.length;
			int xorWith = chainInts[0];
			for (int i = 0; i < chainInts.length; i++) {
				chainInts[i] ^= xorWith;
			}
			long hash = hash(chainInts, 1, offsets);
			for (int i = 0; i < chainInts.length; i++) {
				chainInts[i] ^= xorWith;
			}
			return hash;
		} else if (offsets.inner == null) {
			return offsets.value + hash(chainInts, index);
		} else {
			return hash(chainInts, index + 1, offsets.inner[chainInts[index]]);
		}
	}

	private static long hash(int[] chainInts, int index) {
		if (index == chainInts.length)
			return 0;
		int size = chainInts[index];
		for (int i = 0; i < index; i++)
			if (chainInts[i] < chainInts[index])
				size--;
		return size * pFact(16 - index, chainInts.length - index)
				+ hash(chainInts, index + 1);
	}
}
