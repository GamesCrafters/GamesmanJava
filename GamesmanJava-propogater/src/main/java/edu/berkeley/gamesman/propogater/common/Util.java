package edu.berkeley.gamesman.propogater.common;

public class Util {
	public static <T> boolean contains(T[] pathList, T p1) {
		for (int i = 0; i < pathList.length; i++) {
			if (pathList[i].equals(p1))
				return true;
		}
		return false;
	}
}
