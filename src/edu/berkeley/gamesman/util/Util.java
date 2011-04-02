package edu.berkeley.gamesman.util;

import java.io.DataInput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

/**
 * Various utility functions accessible from any class
 * 
 * @author Steven Schlansker
 * 
 */
public final class Util {

	private Util() {
	}

	static EnumSet<DebugFacility> debugOpts = EnumSet
			.noneOf(DebugFacility.class);

	/**
	 * Initialize the debugging facilities based on a Configuration object. Each
	 * facility is turned on by setting the property gamesman.debug.Facility
	 * (e.g. gamesman.debug.CORE) to some value v such that Util.parseBoolean(v)
	 * is true;
	 * 
	 * @see DebugFacility {@link DebugFacility}
	 * @param debugs
	 *            An EnumSet<DebugFacility> of the desired DebugFacility's to be
	 *            printed.
	 */
	public static void enableDebuging(EnumSet<DebugFacility> debugs) {
		debugOpts = debugs;
		assert Util.debug(DebugFacility.CORE, "Debugging enabled for: "
				+ debugOpts);
	}

	/**
	 * (Possibly) print a debugging message. Calling this method should allways
	 * be wrapped in an assert statement, so the debug statements can be removed
	 * at runtime. For example: assert Util.debug(DebugFacility.GAME,
	 * "Testing testing");
	 * 
	 * @param fac
	 *            The facility that is logging this message
	 * @param s
	 *            The message to print
	 * @return true
	 */
	public static boolean debug(DebugFacility fac, String s) {
		if (debug(fac)) {
			// StackTraceElement stack =
			// Thread.currentThread().getStackTrace()[3];
			// System.out.printf("DEBUG %s (%s)\n\t%s\n", stack.toString(),
			// Thread.currentThread().getName(), s);
			System.out.println("DEBUG " + fac + ": ("
					+ Thread.currentThread().getName() + ") " + s);
		}
		return true;
	}

	/**
	 * Calls Util.debug(fac, String.format(format, args))
	 * 
	 * @param fac
	 *            The facility that is logging this message
	 * @param format
	 *            The format string
	 * @param args
	 *            The arguments to the format string
	 * @return true
	 */
	public static boolean debugFormat(DebugFacility fac, String format,
			Object... args) {
		return debug(fac, String.format(format, args));
	}

	/**
	 * Convert milliseconds to a human-readable string
	 * 
	 * @param millis
	 *            the number of milliseconds
	 * @return a string for that time
	 */
	public static String millisToETA(long millis) {
		long sec = (millis / 1000) % 60;
		long min = (millis / 1000 / 60) % 60;
		long hr = (millis / 1000 / 60 / 60);
		return String.format("%02d:%02d:%02d", hr, min, sec);
	}

	/**
	 * Convert a number of bytes say 4096 and convert it into a more readable
	 * string like 4KB.
	 * 
	 * @param in_bytes
	 *            - The number of bytes that needs to be converted
	 * @return - Formatted string.
	 * @author Alex Trofimov
	 */
	public static String bytesToString(final long in_bytes) {
		long bytes = in_bytes;
		assert bytes > 0l;
		if (bytes < 1024)
			return String.format("%dB", bytes);
		char[] p = new char[] { 'K', 'M', 'G', 'T', 'P', 'E' };
		byte ind = 0;
		while (bytes >>> 10 > 0l) {
			bytes = bytes >>> 10;
			ind++;
		}
		return String.format("%d%cB", bytes, p[ind - 1]);
	}

	/**
	 * Convenience function to calculate linear offset for two dimensional
	 * coordinates
	 * 
	 * @param row
	 *            row position
	 * @param col
	 *            col position
	 * @param w
	 *            Board width
	 * @return Linear offset into 1-d array
	 */
	public static int index(int row, int col, int w) {
		return col + row * w;
	}

	/**
	 * Calculate b^e for longs. Relatively fast - O(log e). Not well defined for
	 * e < 0 or b^e > MAX_INT.
	 * 
	 * @param b
	 *            Base
	 * @param e
	 *            Exponent
	 * @return b^e
	 */
	public static long longpow(int b, int e) {
		if (e <= 0)
			return 1;
		if (e % 2 == 0) {
			long s = longpow(b, e / 2);
			return s * s;
		}
		return b * longpow(b, e - 1);
	}

	/**
	 * Calculate binomial coefficient (n k)
	 * 
	 * Shamelessly stolen from
	 * http://en.wikipedia.org/w/index.php?title=Binomial_coefficient
	 * &oldid=250717842
	 * 
	 * @param n
	 *            n
	 * @param k
	 *            k
	 * @return n choose k
	 */
	public static synchronized long nCr(int n, int k) {
		if (n < 0 || k < 0)
			return _nCr(n, k);
		if (n < nCr_arr.length && k < nCr_arr[0].length) {
			if (nCr_arr[n][k] != 0)
				return nCr_arr[n][k];
			nCr_arr[n][k] = _nCr(n, k);
			return nCr_arr[n][k];
		}
		return _nCr(n, k);
	}

	/**
	 * Precompute n choose k
	 * 
	 * @see Util#nCr(int, int)
	 * @param maxn
	 *            maximum n
	 * @param maxk
	 *            maximum k
	 */
	public static void nCr_prefill(int maxn, int maxk) {
		if (maxn <= nCr_arr.length && maxk <= nCr_arr[0].length)
			return;
		nCr_arr = new long[maxn + 1][maxk + 1];
		for (int n = 0; n <= maxn; n++)
			for (int k = 0; k <= maxk; k++)
				nCr_arr[n][k] = _nCr(n, k);
	}

	/**
	 * Static array of nCr values
	 */
	public static long[][] nCr_arr = new long[0][0]; // 50 is a made-up number,

	// you're free to adjust
	// as necessary...

	private static long _nCr(int n, int mk) {
		int k = mk;
		if (k > n)
			return 0;

		if (k > n / 2)
			k = n - k; // go faster

		double accum = 1;
		for (long i = 1; i <= k; i++)
			accum = accum * (n - k + i) / i;

		return (long) (accum + 0.5); // avoid rounding error
	}

	/**
	 * Find a child of a directory Checks for potentially unsafe entries such as
	 * ..
	 * 
	 * @param dir
	 *            The directory
	 * @param childname
	 *            Name of the child
	 * @return the File referring to that child
	 */
	public static File getChild(File dir, final String childname) {
		return new File(dir, childname); // TODO: sanity check childname
	}

	/**
	 * Compute a "Pascal-like" string where the string is prefixed by its
	 * length. In this case it is prefixed by a number as a string instead of a
	 * fixed-length number
	 * 
	 * @param s
	 *            The string
	 * @return a "Pascal-like" string
	 */
	public static String pstr(String s) {
		return s.length() + s;
	}

	/**
	 * Like Class.forName but checks for casting errors
	 * 
	 * @param <T>
	 *            The type we want
	 * @param name
	 *            What class to forName
	 * @param baseClass
	 *            A base class to ensure type safety--pass in Object if you
	 *            don't care.
	 * @return The Class object
	 * @throws ClassNotFoundException
	 *             Usually, calling code should trigger a Util.fatalError.
	 * @see Class#forName(String)
	 */
	public static <T> Class<? extends T> typedForName(String name,
			Class<T> baseClass) throws ClassNotFoundException {
		if (name.endsWith(".py")) {
			String pyClass = name.substring(0, name.length() - 3);
			return JythonUtil.getClass(pyClass, pyClass, baseClass);
		}
		Class<? extends T> cls = Class.forName(name).asSubclass(baseClass);
		return cls;
	}

	//
	// /**
	// * Handy method for working with 'unchecked' casts - send them here and it
	// * will throw a RuntimeException instead of giving you a compiler warning.
	// * DO NOT USE unless you are sure there's no other options! Use generics
	// * instead if at all possible
	// *
	// * @param <T>
	// * The type to cast to
	// * @param <V>
	// * The type we're casting from
	// * @param in
	// * The object to cast
	// * @return A casted object
	// */
	// public static <T, V> T checkedCast(V in) {
	// return (T) in;
	// }

	/**
	 * Method to join the elements of arr, separated by separator.
	 * 
	 * @param separator
	 *            What to separate the elements of arr by, usually something
	 *            like , or ;
	 * @param arr
	 *            An Array of the elements to join together.
	 * @return The toString() of each element of arr, separated by separator
	 */
	public static String join(String separator, Object arr) {
		if (!arr.getClass().isArray())
			throw new RuntimeException("join() needs an Array");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < Array.getLength(arr); i++)
			sb.append(separator).append(Array.get(arr, i));
		return sb.length() == 0 ? "" : sb.substring(separator.length());
	}

	/**
	 * Method to join the elements of i, separated by separator.
	 * 
	 * @param separator
	 *            What to separate the elements of i by, usually something like
	 *            , or ;
	 * @param i
	 *            An Iterable of the elements to join together.
	 * @return The toString() of each element of i, separated by separator
	 */
	public static String join(String separator, Iterable<?> i) {
		StringBuilder sb = new StringBuilder();
		for (Object o : i)
			sb.append(separator).append(o.toString());
		return sb.length() == 0 ? "" : sb.substring(separator.length());
	}

	/**
	 * @param lastValue
	 *            The last value to go to
	 * @return An Iteratable that runs from zero through lastValue incrementing
	 *         by one
	 */
	public static Iterable<BigInteger> bigIntIterator(final BigInteger lastValue) {
		return bigIntIterator(BigInteger.ZERO, lastValue);
	}

	/**
	 * @param firstValue
	 *            The first value to go to
	 * @param lastValue
	 *            The last value to go to
	 * @return An Iteratble that runs from firstValue to lastValue incrementing
	 *         by one
	 */
	public static Iterable<BigInteger> bigIntIterator(
			final BigInteger firstValue, final BigInteger lastValue) {
		return bigIntIterator(firstValue, lastValue, BigInteger.ONE);
	}

	/**
	 * @param firstValue
	 *            The first value to go to
	 * @param lastValue
	 *            The last value to go to
	 * @param stepSize
	 *            The number of values to step by
	 * @return An Iteratble that runs from firstValue to lastValue incrementing
	 *         by stepSize
	 */
	public static Iterable<BigInteger> bigIntIterator(
			final BigInteger firstValue, final BigInteger lastValue,
			final BigInteger stepSize) {
		return new Iterable<BigInteger>() {
			public Iterator<BigInteger> iterator() {
				return new Iterator<BigInteger>() {
					BigInteger cur = firstValue;

					public boolean hasNext() {
						return cur.compareTo(lastValue) <= 0;
					}

					public BigInteger next() {
						BigInteger old = cur;
						cur = cur.add(stepSize);
						return old;
					}

					public void remove() {
						throw new UnsupportedOperationException(
								"Cannot remove from a bigIntIterator");
					}
				};
			}
		};
	}

	/**
	 * @param <H>
	 *            The type of object arr contains
	 * @param arr
	 *            The array to look in
	 * @param i
	 *            The index
	 * @return arr[i%arr.length]
	 */
	public static <H> H moduloAccess(H[] arr, int i) {
		return arr[nonNegativeModulo(i, arr.length)];
	}

	/**
	 * @param a
	 *            Numerator
	 * @param b
	 *            Denominator
	 * @return a%b wrapped to always be non-negative
	 */
	public static long nonNegativeModulo(long a, long b) {
		long y = a % b;
		if (y >= 0)
			return y;
		return y + b;
	}

	/**
	 * @param a
	 *            Numerator
	 * @param b
	 *            Denominator
	 * @return a%b wrapped to always be non-negative
	 */
	public static int nonNegativeModulo(int a, int b) {
		int y = a % b;
		if (y >= 0)
			return y;
		return y + b;
	}

	/**
	 * @param arr
	 *            An array of strings of numbers
	 * @return An array of the equivalent integers
	 */
	public static int[] parseInts(String... arr) {
		int[] ints = new int[arr.length];
		for (int i = 0; i < ints.length; i++)
			ints[i] = Integer.parseInt(arr[i]);
		return ints;
	}

	/**
	 * Return max of two numbers
	 * 
	 * @param a
	 *            - a number
	 * @param b
	 *            - a number
	 * @return the max of and b
	 */
	public static Number max(Number a, Number b) {
		if (a.doubleValue() > b.doubleValue())
			return a;
		else
			return b;
	}

	/**
	 * @param index
	 *            The index to search for
	 * @param rangeEnds
	 *            The largest and smallest possible values
	 * @return The index that index is at
	 */
	public static int binaryRangeSearch(BigInteger index, BigInteger[] rangeEnds) {
		int l = 0, r = rangeEnds.length;
		int p;
		// System.out.println("search "+idx);
		while (true) {
			p = (r - l) / 2;

			if (p == 0 && rangeEnds[p].compareTo(index) < 0)
				throw new RuntimeException("Index " + index
						+ " not in binary search " + Arrays.toString(rangeEnds));
			p += l;
			// System.out.print(p);
			if (rangeEnds[p].compareTo(index) >= 0) {
				if (p == 0 || rangeEnds[p - 1].compareTo(index) < 0) {
					// System.out.println(idx+" E "+idx+" ("+sval(p)+"-"+ends[p]+")");
					break;
				}

				// System.out.println(" r");
				r = p;
			} else {
				// System.out.println(" l");
				l = p;
			}
		}
		assert p >= 0 && p < rangeEnds.length;
		return p;
	}

	/**
	 * @param tasks
	 *            The preferred number of tasks
	 * @param start
	 *            The start of the first task
	 * @param length
	 *            The length of all the tasks combined
	 * @param groupLength
	 *            The group length to align to
	 * @return The starts of each task
	 */
	public static long[] groupAlignedTasks(int tasks, long start, long length,
			int groupLength, int minGroup) {
		int split = (int) Math.min(tasks,
				Math.max(length / Math.max(groupLength, minGroup), 1));
		long[] starts = new long[split + 1];
		for (int count = 0; count < split; count++) {
			starts[count] = length * count / split + start;
			if (count > 0)
				starts[count] -= starts[count] % groupLength;
		}
		starts[split] = length + start;
		return starts;
	}

	/**
	 * @param arr
	 *            An array of strings of numbers
	 * @return An array of the equivalent integers
	 */
	public static Integer[] parseIntegers(String... arr) {
		Integer[] ints = new Integer[arr.length];
		for (int i = 0; i < ints.length; i++)
			ints[i] = Integer.parseInt(arr[i]);
		return ints;
	}

	/**
	 * @param fac
	 *            A debugging facility
	 * @return Is this debug facility turned on?
	 */
	public static boolean debug(DebugFacility fac) {
		return debugOpts.contains(fac) || debugOpts.contains(DebugFacility.ALL);
	}

	/**
	 * @param <T>
	 *            The type of the array
	 * @param objs
	 *            A 2D array of objects
	 * @param i1
	 *            First index
	 * @param i2
	 *            Second index
	 * @return objs[i1][i2] if it's in bounds, otherwise null
	 */
	public static <T> T getElement(T[][] objs, int i1, int i2) {
		if (i1 >= 0 && i1 < objs.length && i2 >= 0 && i2 < objs[i1].length)
			return objs[i1][i2];
		else
			return null;
	}

	public static void skipFully(DataInput in, int numBytes) throws IOException {
		while (numBytes > 0) {
			int skipped = in.skipBytes(numBytes);
			if (skipped < 0)
				throw new EOFException();
			numBytes -= skipped;
		}
	}

	public static void skipFully(InputStream in, long numBytes)
			throws IOException {
		while (numBytes > 0) {
			long skipped = in.skip(numBytes);
			if (skipped < 0)
				throw new EOFException();
			numBytes -= skipped;
		}
	}

	public static long[] getSplits(long start, long num, long minSplitSize,
			int minSplits, long preferredSplitSize) {
		minSplits = Math.max(minSplits, 1);
		int maxSplits = Math.max(
				(int) Math.min(Integer.MAX_VALUE, num / minSplitSize), 1);
		int numSplits = (int) Math.min(Integer.MAX_VALUE, num
				/ preferredSplitSize);
		if (numSplits < minSplits)
			numSplits = minSplits;
		if (numSplits > maxSplits)
			numSplits = maxSplits;
		long[] splits = new long[numSplits + 1];
		for (int i = 0; i <= numSplits; i++) {
			splits[i] = start + i * num / numSplits;
		}
		return splits;
	}
}
