/**
 * 
 */
package edu.berkeley.gamesman.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import edu.berkeley.gamesman.core.Configuration;

/**
 * Various utility functions accessible from any class
 * 
 * @author Steven Schlansker
 * 
 */
public final class Util {

	private Util() {
	}

	protected static class AssertionFailedError extends Error {
		private static final long serialVersionUID = 2545784238123111405L;
	}

	protected static class FatalError extends Error {
		private static final long serialVersionUID = -5642903706572262719L;
	}

	protected static class Warning extends Error {
		private static final long serialVersionUID = -4160479272744795242L;
	}

	/**
	 * Throws a fatal Error if a required condition is not satisfied
	 * 
	 * @param b
	 *            The boolean (expression) that must be true
	 */
	public static void assertTrue(boolean b, String reason) {
		if (!b) {
			Util.fatalError("Assertion failed: " + reason);
		}
	}

	public static void fatalError(String s) {
		System.err.println("FATAL: (" + Thread.currentThread().getName() + ") "
				+ s);
		System.err.println("Stack trace follows:");
		try {
			throw new FatalError();
		} catch (FatalError e) {
			e.printStackTrace(System.err);
		}
		System.exit(1234);
	}

	public static void fatalError(String s, Exception cause) {
		System.err.println("FATAL: (" + Thread.currentThread().getName() + ") "
				+ s);
		System.err.println(cause.getMessage());
		cause.printStackTrace(System.err);
		System.exit(1235);
	}

	public static void warn(String s) {
		System.err.println("WARN: (" + Thread.currentThread().getName() + ") "
				+ s);
		System.err.println("Stack trace follows:");
		try {
			throw new Warning();
		} catch (Warning e) {
			e.printStackTrace(System.err);
		}
	}

	static boolean debugInit = true, debugOn = true;

	public static void debug(String s) {
		if (!debugOn)
			return;
		if (debugInit) {
			debugInit = false;
			debugOn = OptionProcessor.checkOption("d") != null;
			if (!debugOn)
				return;
		}
		System.err.println("DEBUG: (" + Thread.currentThread().getName() + ") "
				+ s);
	}

	public static String millisToETA(long millis) {
		long sec = (millis / 1000) % 60;
		long min = (millis / 1000 / 60) % 60;
		long hr = (millis / 1000 / 60 / 60);
		return String.format("%02d:%02d:%02d", hr, min, sec);
	}

	/**
	 * Convenience function to calculate linear offset for two dimensional
	 * coordinates
	 * 
	 * @param x
	 *            X position
	 * @param y
	 *            Y position
	 * @param w
	 *            Board width
	 * @return Linear offset into 1-d array
	 */
	public static int index(int x, int y, int w) {
		return x + y * w;
	}

	/**
	 * Calculate b^e for integers. Relatively fast - O(log e). Not well defined
	 * for e < 0 or b^e > MAX_INT.
	 * 
	 * @param b
	 *            Base
	 * @param e
	 *            Exponent
	 * @return b^e
	 */
	public static int intpow(int b, int e) {
		if (e <= 0)
			return 1;
		if (e % 2 == 0) {
			int s = intpow(b, e / 2);
			return s * s;
		}
		return b * intpow(b, e - 1);
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

	public static void nCr_prefill(int maxn, int maxk) {
		nCr_arr = new long[maxn + 1][maxk + 1];
		for (int n = 0; n <= maxn; n++)
			for (int k = 0; k <= maxk; k++)
				nCr_arr[n][k] = _nCr(n, k);
	}

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

	public static File getChild(File dir, final String childname) {
		return new File(dir, childname); // TODO: sanity check childname
	}

	public static String pstr(String s) {
		return s.length() + s;
	}

	public static String encodeBase64(byte[] bytes) {
		return Base64.encodeBytes(bytes,Base64.GZIP | Base64.DONT_BREAK_LINES);
	}

	public static byte[] decodeBase64(String string) {
		return Base64.decode(string);
	}

	public static <T> T deserialize(byte[] bytes) {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(bais);
			return (T) ois.readObject();
		} catch (Exception e) {
			Util.fatalError("Could not deserialize object", e);
		}
		return null;
	}

	public static byte[] serialize(Object obj) {
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
	
	@SuppressWarnings("unchecked")
	public static <T> Class<T> typedForName(String name){
		try {
			Class<T> cls = (Class<T>) Class.forName(name);
			return cls;
		} catch (ClassNotFoundException e) {
			Util.fatalError("Could not find class \""+name+"\"");
		}
		return null;
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
}
