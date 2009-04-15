package edu.berkeley.gamesman.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import edu.berkeley.gamesman.core.Game;

/**
 * Various utility functions accessible from any class
 * 
 * @author Steven Schlansker
 * 
 */
public final class Util {
	
	private static Base64 b64 = new Base64();

	private Util() {}

	protected static class AssertionFailedError extends Error {
		private static final long serialVersionUID = 2545784238123111405L;
	}

	/**
	 * A FatalError means that some part of Gamesman has barfed
	 * and it is about to exit ungracefully.
	 * 
	 * Be sure to rethrow it if you catch it to clean up but do
	 * not explicitly fix the error condition
	 * @author Steven Schlansker
	 */
	public static class FatalError extends Error {

		FatalError(String s, Exception cause) {
			super(s,cause);
		}

		private static final long serialVersionUID = -5642903706572262719L;
	}

	protected static class Warning extends Error {
		
		public Warning(String s, Exception cause) {
			super(s,cause);
		}
		
		private static final long serialVersionUID = -4160479272744795242L;
	}

	/**
	 * Throws a fatal Error if a required condition is not satisfied
	 * 
	 * @param b The boolean (expression) that must be true
	 * @param reason The reason to give if we fail
	 */
	public static void assertTrue(boolean b, String reason) {
		if (!b) {
			Util.fatalError("Assertion failed: " + reason);
		}
	}

	/**
	 * Throw a fatal error and print stack trace
	 * @param s The reason for failure
	 */
	public static void fatalError(String s) {
		System.err.println("FATAL: (" + Thread.currentThread().getName() + ") "
				+ s);
		System.err.println("Stack trace follows:");
		try {
			throw new FatalError(s,null);
		} catch (FatalError e) {
			e.printStackTrace(System.err);
			throw e;
		}
	}

	/**
	 * Throw a fatal error and print stack trace
	 * @param s The reason for failure
	 * @param cause An exception that caused this fatal error
	 */
	public static void fatalError(String s, Exception cause) {
		System.err.println("FATAL: (" + Thread.currentThread().getName() + ") "
				+ s);
		System.err.println(cause.getMessage());
		cause.printStackTrace(System.err);
		try{
			throw new FatalError(s,cause);
		} catch (FatalError e) {
			e.printStackTrace(System.err);
			throw e;
		}
	}

	/**
	 * Print a non-fatal warning and continue
	 * @param s The condition that caused this warning
	 */
	public static void warn(String s) {
		System.err.println("WARN: (" + Thread.currentThread().getName() + ") "
				+ s);
		System.err.println("Stack trace follows:");
		try {
			throw new Warning(s,null);
		} catch (Warning e) {
			e.printStackTrace(System.err);
		}
	}
	
	/**
	 * Print a non-fatal warning and continue
	 * @param s The condition that caused this warning
	 * @param ex The exception that caused the warning
	 */
	public static void warn(String s, Exception ex) {
		System.err.println("WARN: (" + Thread.currentThread().getName() + ") "
				+ s);
		System.err.println("Stack trace follows:");
		try {
			throw new Warning(s,ex);
		} catch (Warning e) {
			e.printStackTrace(System.err);
		}
	}

	static EnumSet<DebugFacility> debugOpts = EnumSet.noneOf(DebugFacility.class);
	
	/**
	 * Initialize the debugging facilities based on a Configuration object.
	 * Each facility is turned on by setting the property gamesman.debug.Facility
	 * (e.g. gamesman.debug.CORE) to some value v such that Util.parseBoolean(v) is true;
	 * @see DebugFacility {@link DebugFacility}
	 * @param debugs An EnumSet<DebugFacility> of the desired DebugFacility's to be printed.
	 */
	public static void enableDebuging(EnumSet<DebugFacility> debugs){
		debugOpts = debugs;
		assert Util.debug(DebugFacility.CORE, "Debugging enabled for: " + debugOpts);
	}

	/**
	 * (Possibly) print a debugging message. Calling this method should allways be 
	 * wrapped in an assert statement, so the debug statements can be removed at
	 * runtime. For example:
	 * assert Util.debug(DebugFacility.GAME, "Testing testing");
	 * @param fac The facility that is logging this message
	 * @param s The message to print
	 * @return true
	 */
	public static boolean debug(DebugFacility fac, String s) {
		if(debugOpts.contains(fac) || debugOpts.contains(DebugFacility.ALL)){
//			StackTraceElement stack = Thread.currentThread().getStackTrace()[3];
//			System.out.printf("DEBUG %s (%s)\n\t%s\n", stack.toString(), Thread.currentThread().getName(), s);
			System.out.println("DEBUG "+fac+": (" + Thread.currentThread().getName() + ") " + s);
		}
		return true;
	}
	
	/**
	 * Calls Util.debug(fac, String.format(format, args))
	 * @param fac The facility that is logging this message
	 * @param format The format string
	 * @param args The arguments to the format string
	 * @return true
	 */
	public static boolean debugFormat(DebugFacility fac, String format, Object... args){
		return debug(fac, String.format(format, args));
	}
	
	/**
	 * Convert milliseconds to a human-readable string
	 * @param millis the number of milliseconds
	 * @return a string for that time
	 */
	public static String millisToETA(long millis) {
		long sec = (millis / 1000) % 60;
		long min = (millis / 1000 / 60) % 60;
		long hr = (millis / 1000 / 60 / 60);
		return String.format("%02d:%02d:%02d", hr, min, sec);
	}
	
	/** 
	 * Convert a number of bytes say 4096 and convert it into
	 * a more readable string like 4KB.
	 * @param in_bytes - The number of bytes that needs to be converted
	 * @return - Formatted string.
	 * @author Alex Trofimov
	 */
	public static String bytesToString(final long in_bytes) {
		long bytes = in_bytes;
		assert bytes > 0l;
		if (bytes < 1024) return String.format("%dB", bytes);
		char[] p = new char[] { 'K', 'M', 'G', 'T', 'P', 'E' };
		byte ind = 0;
		while (bytes >>> 10 > 0l) { 
			bytes = bytes >>> 10;
			ind ++;
		}		
		return String.format("%d%cB", bytes, p[ind - 1]);
	}

	/**
	 * Convenience function to calculate linear offset for two dimensional
	 * coordinates
	 * 
	 * @param row row position
	 * @param col col position
	 * @param w Board width
	 * @return Linear offset into 1-d array
	 */
	public static int index(int row, int col, int w) {
		return col + row * w;
	}

	/**
	 * Calculate b^e for longs. Relatively fast - O(log e). Not well defined
	 * for e < 0 or b^e > MAX_INT.
	 * 
	 * @param b Base
	 * @param e Exponent
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
	 * @param n n
	 * @param k k
	 * @return n choose k
	 */
	public static long nCr(int n, int k) {
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
	 * @see Util#nCr(int, int)
	 * @param maxn maximum  n
	 * @param maxk maximum k
	 */
	public static void nCr_prefill(int maxn, int maxk) {
		if(maxn <= nCr_arr.length && maxk <= nCr_arr[0].length)
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
	 * Find a child of a directory
	 * Checks for potentially unsafe entries such as ..
	 * @param dir The directory
	 * @param childname Name of the child
	 * @return the File referring to that child
	 */
	public static File getChild(File dir, final String childname) {
		return new File(dir, childname); // TODO: sanity check childname
	}

	/**
	 * Compute a "Pascal-like" string where the string is prefixed
	 * by its length.  In this case it is prefixed by a number as a string
	 * instead of a fixed-length number
	 * @param s The string
	 * @return a "Pascal-like" string
	 */
	public static String pstr(String s) {
		return s.length() + s;
	}

	/**
	 * Convenience Base-64 encoder
	 * @param bytes The data
	 * @return Base-64 representation of bytes
	 */
	public static String encodeBase64(byte[] bytes) {
		//return Base64.encodeBytes(bytes,Base64.GZIP | Base64.DONT_BREAK_LINES);
		return new String(b64.encode(bytes));
	}

	/**
	 * Convenience Base-64 decoder
	 * @param string The Base-64 to decode
	 * @return Decoded byte array
	 */
	public static byte[] decodeBase64(String string) {
		//return Base64.decode(string);
		return b64.decode(string.getBytes());
	}

	/**
	 * Deserialize an object stored in a byte array
	 * @param <T> the type of the object stored
	 * @param bytes the array it is stored in
	 * @return the object stored
	 */
	public static <T> T deserialize(byte[] bytes) {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(bais);
			return checkedCast(ois.readObject());
		} catch (Exception e) {
			Util.fatalError("Could not deserialize object", e);
		}
		return null;
	}

	/**
	 * Serialize and object and return it in a new byte array
	 * @param obj The object to serialize (must be Serializable)
	 * @return a byte array
	 */
	public static byte[] serialize(Serializable obj) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();
		} catch (IOException e) {
			Util.fatalError("Could not serialize object", e);
		}
		return baos.toByteArray();
	}
	
	/**
	 * Like Class.forName but checks for casting errors
	 * 
	 * @param <T> The type we want
	 * @param name What class to forName
	 * @param baseClass A base class to ensure type safety--pass in Object if you don't care.
	 * @return The Class object
	 * @throws ClassNotFoundException Usually, calling code should trigger a Util.fatalError.
	 * @see Class#forName(String)
	 */
	public static <T> Class<? extends T> typedForName(String name, Class<T> baseClass)
			throws ClassNotFoundException {
		if (name.endsWith(".py")) {
			String pyClass = name.substring(0, name.length()-3);
			return JythonUtil.getClass(pyClass, pyClass, baseClass);
		}
		Class<? extends T> cls = Class.forName(name).asSubclass(baseClass);
		return cls;
	}
	
	/**
	 * Like Class.forName(name).newInstance() but with type checking
	 * @param <T> The type requested
	 * @param name The name of the class to instantiate
	 * @param baseClass Usually equals the template param
	 * @return A new instance (created with the default constructor)
	 * @throws ClassNotFoundException If the class could not be instantiated.
	 */
	public static <T> T typedInstantiate(String name, Class<T> baseClass) throws ClassNotFoundException{
		try {
			return typedForName(name, baseClass).getConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new ClassNotFoundException("Uncaught exception while instantiating "+name,e);
		}
	}
	
	/**
	 * Like Class.forName(name).newInstance() but with type checking
	 * and an argument
	 * @param <T> The type requested
	 * @param name The name of the class to instantiate
	 * @param baseClass Usually equals the template param
	 * @param arg The argument to provide to the constructor
	 * @return A new instance (created with the non-default constructor)
	 * @throws ClassNotFoundException The class could not be loaded
	 */
	public static <T> T typedInstantiateArg(String name, Class<T> baseClass, Object arg) throws ClassNotFoundException{
		try {
			return checkedCast(typedForName(name, baseClass).getConstructors()[0].newInstance(arg));
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (Exception e) {
			throw new ClassNotFoundException("Uncaught exception while instantiating "+name,e);
		}
	}
	
	
	/**
	 * Handy method for working with 'unchecked' casts -
	 * send them here and it will throw a RuntimeException
	 * instead of giving you a compiler warning.
	 * DO NOT USE unless you are sure there's no other options!
	 * Use generics instead if at all possible
	 * @param <T> The type to cast to
	 * @param <V> The type we're casting from
	 * @param in The object to cast
	 * @return A casted object
	 */
	@SuppressWarnings("unchecked")
	public static <T,V> T checkedCast(V in){
		return (T)in;
	}
	
	/**
	 * Method to join the elements of arr, separated by separator.
	 * @param separator What to separate the elements of arr by, usually something like , or ;
	 * @param arr An Array of the elements to join together.
	 * @return The toString() of each element of arr, separated by separator 
	 */
	public static String join(String separator, Object arr) {
		if(!arr.getClass().isArray()) {
			fatalError("join() needs an Array");
			return null;
		}
			
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<Array.getLength(arr); i++)
			sb.append(separator).append(Array.get(arr, i));
		return sb.length() == 0 ? "" : sb.substring(separator.length());
	}
	
	/**
	 * Method to join the elements of i, separated by separator.
	 * @param separator What to separate the elements of i by, usually something like , or ;
	 * @param i An Iterable of the elements to join together.
	 * @return The toString() of each element of i, separated by separator 
	 */
	public static String join(String separator, Iterable<?> i) {
		StringBuilder sb = new StringBuilder();
		for(Object o : i)
			sb.append(separator).append(o.toString());
		return sb.length() == 0 ? "" : sb.substring(separator.length());
	}
	
	public static <T> List<String> mapStateToString(Game<T> g, List<T> l){
		ArrayList<String> out = new ArrayList<String>();
		
		for(T i : l)
			out.add(g.stateToString(i));
		return out;
	}
	
	public static Iterable<BigInteger> bigIntIterator(final BigInteger lastValue) {
		return bigIntIterator(BigInteger.ZERO, lastValue);
	}
	public static Iterable<BigInteger> bigIntIterator(final BigInteger firstValue, final BigInteger lastValue) {
		return bigIntIterator(firstValue, lastValue, BigInteger.ONE);
	}
	public static Iterable<BigInteger> bigIntIterator(final BigInteger firstValue, final BigInteger lastValue, final BigInteger stepSize) {
		return new Iterable<BigInteger>(){
			public Iterator<BigInteger> iterator() {
				return new Iterator<BigInteger>(){
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
						throw new UnsupportedOperationException("Cannot remove from a bigIntIterator");
					}
				};
			}
		};
	}
	
	public static <H> H moduloAccess(H[] arr, int i) {
		return arr[positiveModulo(i, arr.length)];
	}
	
	public static long positiveModulo(long a, long b){
		long y = a % b;
		if(y >= 0) return y;
		return y+b;
	}
	
	public static int positiveModulo(int a, int b){
		int y = a % b;
		if(y >= 0) return y;
		return y+b;
	}
	
	public static Integer[] parseIntegers(String... arr) {
		Integer[] ints = new Integer[arr.length];
		for(int i=0; i<ints.length; i++)
			ints[i] = Integer.parseInt(arr[i]);
		return ints;
	}
	
	@SuppressWarnings("unchecked")
	public static <H> H[] toArray(List<H> list) {
		return list.toArray((H[])Array.newInstance(list.get(0).getClass(), list.size()));
	}
	
	public static int binaryRangeSearch(BigInteger index,BigInteger[] rangeEnds){
		int l = 0, r = rangeEnds.length;
		int p;
		//System.out.println("search "+idx);
		while(true){
			p = (r-l)/2;

			if(p == 0 && rangeEnds[p].compareTo(index) < 0)
				Util.fatalError("Index "+index+" not in binary search "+Arrays.toString(rangeEnds));
			p += l;
			//System.out.print(p);
			if(rangeEnds[p].compareTo(index) >= 0){
				if(p == 0 || rangeEnds[p-1].compareTo(index) < 0){
					//System.out.println(idx+" E "+idx+" ("+sval(p)+"-"+ends[p]+")");
					break;
				}

				//System.out.println(" r");
				r = p;
			}else{
				//System.out.println(" l");
				l = p;
			}
		}
		assert p>=0 && p < rangeEnds.length;
		return p;
	}
}
