package edu.berkeley.gamesman.testing;

import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.Quarto;
import edu.berkeley.gamesman.game.util.TierState;

@SuppressWarnings("javadoc")
public class QuartoPlayer {
	public static void main(String[] args) throws ClassNotFoundException {
		Configuration conf = new Configuration(
				QuartoPlayer.class.getResourceAsStream("jobs/quarto.job"));
		Quarto game = (Quarto) conf.getGame();
		game.setStartingPosition(0);
		int tier = 0;
		Scanner scan = new Scanner(System.in);
		TierState[] moves = game.newStateArray(256);
		while (game.primitiveValue() == Value.UNDECIDED) {
			System.out.println(game.displayState());
			System.out.println("Choose a piece: ");
			for (int i = 0; i < 16; i++) {
				if (!game.used(i)) {
					for (int k = 3; k >= 0; k--) {
						System.out.print((i >>> k) & 1);
					}
					System.out.print(" ");
				}
			}
			System.out.println();
			String next = scan.next();
			int choice = Integer.parseInt(next, 2);
			game.validMoves(moves);
			for (int i = choice - 1; i >= 0; i--) {
				if (game.used(i))
					choice--;
			}
			int startPoint = (16 - tier) * choice;
			System.out.println("Select place");
			String placeChoice = scan.next();
			int col = Character.toUpperCase(placeChoice.charAt(0)) - 'A';
			int row = placeChoice.charAt(1) - '1';
			int place = row * 4 + col;
			for (int i = place - 1; i >= 0; i--) {
				if (game.usedPlace(i))
					place--;
			}
			game.setState(moves[startPoint + place]);
			tier++;
		}
		System.out.println(game.displayState());
	}
}
