package edu.berkeley.gamesman.util;

import java.util.Arrays;

/**
 * Unsigned Mutable BigIntegers
 * 
 * @author dnspies
 */
public class UnsignedMutableBigInteger implements
		Comparable<UnsignedMutableBigInteger>, Cloneable {
	private static final long POSITIVE_MASK = ((1L << 32) - 1);

	private int[] ints;
	private int length;
	private UnsignedMutableBigInteger temp;

	/**
	 * Default constructor. Initially sets this equal to zero
	 */
	public UnsignedMutableBigInteger() {
		length = 0;
		ints = new int[0];
		temp = null;
	}

	/**
	 * Initializes this Integer with l as the starting value
	 * 
	 * @param l
	 *            The initial value
	 */
	public UnsignedMutableBigInteger(long l) {
		if (l == 0) {
			length = 0;
			ints = new int[0];
		} else if (l > POSITIVE_MASK) {
			ints = new int[] { (int) l, (int) (l >>> 32) };
			length = 2;
		} else {
			ints = new int[] { (int) l };
			length = 1;
		}
		temp = null;
	}

	private UnsignedMutableBigInteger(UnsignedMutableBigInteger other) {
		ints = new int[other.length];
		set(other);
	}

	private static long getPositive(int n) {
		return n & POSITIVE_MASK;
	}

	private void add(int other, int intNum) {
		if (other == 0)
			return;
		long myPos;
		if (length <= intNum)
			myPos = 0;
		else
			myPos = getPositive(ints[intNum]);
		long otherPos = getPositive(other);
		long total = myPos + otherPos;
		int carryAdd = (int) (total >>> 32);
		if (carryAdd > 0)
			add(1, intNum + 1);
		else
			ensureLength(intNum + 1);
		ints[intNum] = (int) total;
	}

	private void ensureSpace(int newLen) {
		if (ints.length < newLen)
			ints = Arrays.copyOf(ints, newLen);
	}

	private void ensureLength(int newLen) {
		ensureSpace(newLen);
		for (; length < newLen; length++)
			ints[length] = 0;
	}

	private void add(long other, int intNum) {
		if (other < 0)
			subtract(-other, intNum);
		else {
			add((int) (other >>> 32), intNum + 1);
			add((int) other, intNum);
		}
	}

	private void addUnsigned(long other, int intNum) {
		add((int) (other >>> 32), intNum + 1);
		add((int) other, intNum);
	}

	/**
	 * this+=other
	 * 
	 * @param other
	 *            The value to add
	 */
	public void add(long other) {
		add(other, 0);
	}

	private void add(UnsignedMutableBigInteger other, int intNum) {
		intNum += other.length - 1;
		for (int i = other.length - 1; i >= 0; i--)
			add(other.ints[i], intNum--);
	}

	/**
	 * this+=other
	 * 
	 * @param other
	 *            The value to add
	 */
	public void add(UnsignedMutableBigInteger other) {
		add(other, 0);
	}

	private void subtract(int other, int intNum) {
		if (other == 0)
			return;
		long myPos = getPositive(ints[intNum]);
		long otherPos = getPositive(other);
		long total = myPos - otherPos;
		if (total < 0) {
			total += 1L << 32;
			subtract(1, intNum + 1);
		}
		ints[intNum] = (int) total;
		if (total == 0 && intNum + 1 == length)
			length--;
	}

	private void subtract(long other, int intNum) {
		if (other < 0)
			add(-other, intNum);
		else {
			subtract((int) (other >>> 32), intNum + 1);
			subtract((int) other, intNum);
		}
	}

	/**
	 * this-=other
	 * 
	 * @param other
	 *            The value to subtract
	 */
	public void subtract(long other) {
		subtract(other, 0);
	}

	private void subtract(UnsignedMutableBigInteger other, int intNum) {
		intNum += other.length - 1;
		for (int i = other.length - 1; i >= 0; i--)
			subtract(other.ints[i], intNum--);
	}

	/**
	 * this-=other
	 * 
	 * @param other
	 *            Another integer
	 */
	public void subtract(UnsignedMutableBigInteger other) {
		subtract(other, 0);
	}

	private void multiplyAndAddToTemp(int other, int intNum) {
		if (other == 0)
			return;
		long otherPos = getPositive(other);
		intNum += length - 1;
		for (int i = length - 1; i >= 0; i--) {
			long total = getPositive(ints[i]) * otherPos;
			temp.addUnsigned(total, intNum--);
		}
	}

	private void multiplyAndAddToTemp(long other, int intNum) {
		multiplyAndAddToTemp((int) (other >>> 32), 1);
		multiplyAndAddToTemp((int) other, 0);
	}

	/**
	 * this*=other
	 * 
	 * @param other
	 *            The value to multiply by
	 */
	public void multiply(long other) {
		if (temp == null)
			temp = new UnsignedMutableBigInteger();
		else
			temp.setZero();
		multiplyAndAddToTemp(other, 0);
		set(temp);
	}

	/**
	 * this=0
	 */
	public void setZero() {
		length = 0;
	}

	/**
	 * this=other
	 * 
	 * @param other
	 *            Another Integer
	 */
	public void set(UnsignedMutableBigInteger other) {
		ensureSpace(other.length);
		length = other.length;
		for (int i = 0; i < length; i++)
			ints[i] = other.ints[i];
	}

	/**
	 * this=other
	 * 
	 * @param other
	 *            Another integer
	 */
	public void set(long other) {
		if (other == 0) {
			length = 0;
		} else if (other > POSITIVE_MASK) {
			ensureSpace(2);
			length = 2;
			ints[0] = (int) other;
			ints[1] = (int) (other >>> 32);
		} else {
			ensureSpace(1);
			ints[0] = (int) other;
			length = 1;
		}
	}

	private void multiplyAndAddToTemp(UnsignedMutableBigInteger other,
			int intNum) {
		intNum += other.length - 1;
		for (int i = other.length - 1; i >= 0; i--)
			multiplyAndAddToTemp(other.ints[i], intNum--);
	}

	/**
	 * this*=other
	 * 
	 * @param other
	 *            Another integer
	 */
	public void multiply(UnsignedMutableBigInteger other) {
		if (temp == null)
			temp = new UnsignedMutableBigInteger();
		else
			temp.setZero();
		multiplyAndAddToTemp(other, 0);
		set(temp);
	}

	/*
	 * Adds this/(n<<shiftBits) to temp. Returns (this>>>shiftBits)%n
	 */
	private int divideAndAddToTemp(int n, int shiftBits) {
		int firstInt = shiftBits >>> 5;
		int extraBits = shiftBits & 31;
		int shamt = 32 - extraBits;
		int shamtMask = (int) ~((1L << shamt) - 1);
		long div, mod = 0;
		long myPos = 0;
		long nPos = getPositive(n);
		int c = length - 1 - firstInt;
		for (int i = length - 1; i >= firstInt; i--) {
			myPos |= getPositive(ints[i]) >>> extraBits;
			long num = myPos | (mod << 32);
			num >>>= 1;
			div = num / nPos;
			mod = num % nPos;
			div <<= 1;
			mod <<= 1;
			if ((myPos & 1) == 1)
				mod |= 1;
			if (mod >= nPos) {
				mod -= nPos;
				div++;
			}
			temp.add((int) div, c--);
			myPos = getPositive((ints[i] << shamt) & shamtMask);
		}
		return (int) mod;
	}

	private int divideAndSubtractFromTemp(int n, int shiftBits) {
		int firstInt = shiftBits >>> 5;
		int extraBits = shiftBits & 31;
		int shamt = 32 - extraBits;
		int shamtMask = (int) ~((1L << shamt) - 1);
		long div, mod = 0;
		long myPos = 0;
		long nPos = getPositive(n);
		int c = length - 1 - firstInt;
		for (int i = length - 1; i >= firstInt; i--) {
			myPos |= getPositive(ints[i]) >>> extraBits;
			long num = myPos | (mod << 32);
			num >>>= 1;
			div = num / nPos;
			mod = num % nPos;
			div <<= 1;
			mod <<= 1;
			if ((myPos & 1) == 1)
				mod |= 1;
			if (mod >= nPos) {
				mod -= nPos;
				div++;
			}
			temp.subtract((int) div, c--);
			myPos = getPositive((ints[i] << shamt) & shamtMask);
		}
		return (int) mod;
	}

	/**
	 * Warning! Does not change this object, only returns the result this%other
	 * 
	 * @param other
	 *            A long to take the remainder from
	 * @return The remainder
	 */
	public long remainder(long other) {
		return divide(other, false);
	}

	/**
	 * div=this/other; mod=this%other; this=div; return mod;
	 * 
	 * @param other
	 *            A long to divide by
	 * @return The remainder
	 */
	public long divide(long other) {
		return divide(other, true);
	}

	// temp = mod
	// temp.temp = div
	private long divide(long other, boolean changeThis) {
		int otherBits = numBits(other);
		if (otherBits <= 32) {
			if (temp == null)
				temp = new UnsignedMutableBigInteger();
			else
				temp.setZero();
			long result = getPositive(divideAndAddToTemp((int) other, 0));
			set(temp);
			return result;
		} else {
			int offBy = otherBits - 32;
			int div = (int) (other >>> offBy);
			if (temp == null) {
				temp = new UnsignedMutableBigInteger(this);
				temp.temp = new UnsignedMutableBigInteger(0);
			} else {
				temp.set(this);
				if (temp.temp == null)
					temp.temp = new UnsignedMutableBigInteger(0);
				else
					temp.temp.setZero();
			}
			temp.divideAndAddToTemp(div, offBy);
			while (true) {
				if (temp.temp.temp == null)
					temp.temp.temp = new UnsignedMutableBigInteger();
				else
					temp.temp.temp.setZero();
				temp.temp.multiplyAndAddToTemp(other, 0);
				if (temp.temp.temp.lessThan(this)) {
					temp.set(this);
					temp.subtract(temp.temp.temp);
					if (temp.lessThan(other)) {
						if (changeThis)
							this.set(temp.temp);
						return temp.longValue();
					} else
						temp.divideAndAddToTemp(div, offBy);
				} else {
					temp.set(temp.temp.temp);
					temp.subtract(this);
					if (temp.lessThan(other)) {
						if (changeThis) {
							this.set(temp.temp);
							this.subtract(1);
						}
						return other - temp.longValue();
					} else
						temp.divideAndSubtractFromTemp(div, offBy);
				}
			}
		}
	}

	/**
	 * divStore=this/other; modStore=this%other;
	 * 
	 * @param other
	 *            Another integer
	 * @param divStore
	 *            An integer to store div in
	 * @param modStore
	 *            An integer to store mod in
	 */
	public void divMod(UnsignedMutableBigInteger other,
			UnsignedMutableBigInteger divStore,
			UnsignedMutableBigInteger modStore) {
		if (other.length <= 1) {
			divide(other.longValue());
			return;
		}
		int div = other.ints[other.length - 1];
		int offBy = numBits(getPositive(div));
		int divShamt = ((other.length - 2) << 5) | offBy;
		div = (div << (32 - offBy)) | (other.ints[other.length - 2] >>> offBy);
		if (temp == null) {
			temp = new UnsignedMutableBigInteger(this);
			temp.temp = new UnsignedMutableBigInteger(0);
		} else {
			temp.set(this);
			if (temp.temp == null)
				temp.temp = new UnsignedMutableBigInteger(0);
			else
				temp.temp.setZero();
		}
		temp.divideAndAddToTemp(div, divShamt);
		while (true) {
			if (temp.temp.temp == null)
				temp.temp.temp = new UnsignedMutableBigInteger();
			else
				temp.temp.temp.setZero();
			temp.temp.multiplyAndAddToTemp(other, 0);
			if (lessThan(temp.temp.temp)) {
				temp.set(temp.temp.temp);
				temp.subtract(this);
				if (temp.lessThan(other)) {
					if (divStore != null) {
						divStore.set(temp.temp);
						divStore.subtract(1);
					}
					if (modStore != null) {
						modStore.set(other);
						modStore.subtract(temp);
					}
					break;
				} else
					temp.divideAndSubtractFromTemp(div, divShamt);
			} else {
				temp.set(this);
				temp.subtract(temp.temp.temp);
				if (temp.lessThan(other)) {
					if (divStore != null)
						divStore.set(temp.temp);
					if (modStore != null)
						modStore.set(temp);
					break;
				} else
					temp.divideAndAddToTemp(div, divShamt);
			}
		}
	}

	/**
	 * this/=other
	 * 
	 * @param other
	 *            Another integer
	 */
	public void divide(UnsignedMutableBigInteger other) {
		divMod(other, this, null);
	}

	/**
	 * this%=other
	 * 
	 * @param other
	 *            Another integer
	 */
	public void remainder(UnsignedMutableBigInteger other) {
		divMod(other, null, this);
	}

	/**
	 * @return (long) this
	 */
	public long longValue() {
		if (length == 0)
			return 0;
		else if (length == 1)
			return getPositive(ints[0]);
		else
			return (getPositive(ints[1]) << 32) | getPositive(ints[0]);
	}

	/**
	 * @param other
	 *            Another integer
	 * @return this>other
	 */
	public boolean greaterThan(UnsignedMutableBigInteger other) {
		if (length > other.length)
			return true;
		else if (other.length > length)
			return false;
		else {
			for (int c = length - 1; c >= 0; c--) {
				if (ints[c] > other.ints[c])
					return true;
				else if (other.ints[c] > ints[c])
					return false;
			}
		}
		return false;
	}

	/**
	 * @param other
	 *            Another integer
	 * @return this>other
	 */
	public boolean greaterThan(long other) {
		if (other < 0)
			return true;
		else if (other > POSITIVE_MASK) {
			if (length > 2)
				return true;
			else if (length < 2)
				return false;
			else if (getPositive(ints[1]) > (other >>> 32))
				return true;
			else if (getPositive(ints[1]) < (other >>> 32))
				return false;
			else
				return getPositive(ints[0]) > (other & POSITIVE_MASK);
		} else {
			if (length > 1)
				return true;
			else if (length < 1)
				return false;
			else
				return getPositive(ints[0]) > other;
		}
	}

	/**
	 * @param other
	 *            Another integer
	 * @return this<other
	 */
	public boolean lessThan(UnsignedMutableBigInteger other) {
		if (length < other.length)
			return true;
		else if (other.length < length)
			return false;
		else {
			for (int c = length - 1; c >= 0; c--) {
				if (ints[c] < other.ints[c])
					return true;
				else if (other.ints[c] < ints[c])
					return false;
			}
		}
		return false;
	}

	/**
	 * @param other
	 *            Another integer
	 * @return this<other
	 */
	public boolean lessThan(long other) {
		if (other <= 0)
			return false;
		else if (other > POSITIVE_MASK) {
			if (length < 2)
				return true;
			else if (length > 2)
				return false;
			else if (getPositive(ints[1]) < (other >>> 32))
				return true;
			else if (getPositive(ints[1]) > (other >>> 32))
				return false;
			else
				return getPositive(ints[0]) < (other & POSITIVE_MASK);
		} else {
			if (length < 1)
				return true;
			else if (length > 1)
				return false;
			else
				return getPositive(ints[0]) < other;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof UnsignedMutableBigInteger) {
			UnsignedMutableBigInteger other = (UnsignedMutableBigInteger) obj;
			if (other.length != length)
				return false;
			else {
				for (int c = length - 1; c >= 0; c--) {
					if (other.ints[c] != ints[c])
						return false;
				}
			}
			return true;
		} else
			return false;
	}

	/**
	 * @param other
	 *            Another integer
	 * @return this==other
	 */
	public boolean equals(long other) {
		if (other < 0)
			return false;
		else if (other > POSITIVE_MASK) {
			if (length != 2)
				return false;
			else if (getPositive(ints[1]) != (other >>> 32))
				return false;
			else
				return getPositive(ints[0]) == (other & POSITIVE_MASK);
		} else if (other > 0) {
			if (length != 1)
				return false;
			else
				return getPositive(ints[0]) == other;
		} else
			return length == 0;
	}

	private static int numBits(long l) {
		int result = 1;
		for (int i = 32; i > 0; i >>= 1) {
			long res = l >>> i;
			if (res > 0) {
				l = res;
				result += i;
			}
		}
		return result;
	}

	@Override
	public String toString() {
		if (temp == null)
			temp = new UnsignedMutableBigInteger(this);
		else
			temp.set(this);
		String s = "";
		while (temp.greaterThan(0)) {
			s = Long.toString(temp.divide(10)) + s;
		}
		return s;
	}

	public int compareTo(UnsignedMutableBigInteger other) {
		if (length < other.length)
			return -1;
		else if (other.length < length)
			return 1;
		else {
			for (int c = length - 1; c >= 0; c--) {
				if (ints[c] < other.ints[c])
					return -1;
				else if (other.ints[c] < ints[c])
					return 1;
			}
		}
		return 0;
	}

	public UnsignedMutableBigInteger clone() {
		return new UnsignedMutableBigInteger(this);
	}
}
