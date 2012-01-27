package edu.berkeley.gamesman.parallel.game.connect4;

import java.util.Scanner;

public class C4Printer {
	public static void main(String[] args) {
		int width = Integer.parseInt(args[0]);
		int height = Integer.parseInt(args[1]);
		int place = new Scanner(System.in).nextInt();
		C4Hasher h = new C4Hasher(width, height, place);
		h.printStates();
	}
}
