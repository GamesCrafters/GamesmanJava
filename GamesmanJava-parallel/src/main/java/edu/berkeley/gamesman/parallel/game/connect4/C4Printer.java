package edu.berkeley.gamesman.parallel.game.connect4;

import java.util.Scanner;

public class C4Printer {
	public static void main(String[] args) {
		int width = Integer.parseInt(args[0]);
		int height = Integer.parseInt(args[1]);
		int place = new Scanner(System.in).nextInt();
		C4Hasher h1 = new C4Hasher(width, height, place);
		C4Hasher h2 = new C4Hasher(width, height);
		int[] use = new int[1];
		for (int i = 0; i < h1.boardSize; i++) {
			use[0] = i;
			System.out.println("Tier " + i + ":");
			System.out.println(String.format("%.3f", h1.numPositions(use)
					/ (double) h2.numPositions(use)));
		}
		System.out.println("Total:");
		System.out.println(String.format("%.3f", h1.totalPositions()
				/ (double) h2.totalPositions()));
	}
}
