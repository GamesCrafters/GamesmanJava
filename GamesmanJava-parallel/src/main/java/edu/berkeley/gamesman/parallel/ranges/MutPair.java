package edu.berkeley.gamesman.parallel.ranges;

import java.util.Arrays;

public class MutPair<T1, T2> {
	T1 car;
	T2 cdr;

	public String toString() {
		return Arrays.toString(new Object[] { car, cdr });
	}
}
