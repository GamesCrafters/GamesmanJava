package edu.berkeley.gamesman.propogater.writable;

public class Resetables {
	public static void reset(Object obj) {
		if (obj instanceof Resetable) {
			((Resetable) obj).reset();
		}
	}

	public static boolean checkReset(Object obj) {
		if (obj instanceof Resetable) {
			return ((Resetable) obj).checkReset();
		} else
			return true;
	}
}
