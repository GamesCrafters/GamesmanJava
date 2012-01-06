package edu.berkeley.gamesman.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.game.Alignment;
import edu.berkeley.gamesman.game.AlignmentState;

@SuppressWarnings("javadoc")
public class AlignmentTester {

	/**
	 * @param args
	 *            The job file to use
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub
		System.out.print(welcome);
		String opt1 = c.readLine();
		if (opt1.equals("q")) {
			System.exit(0);
		}
		Configuration conf = new Configuration(
				Configuration.readProperties(args[0]));
		Alignment game = (Alignment) conf.getGame();
		AlignmentState state = game.newState();
		System.out.println("The board is full of " + state.board[0][0]);
		System.out.println(game.displayState(state));
		String in = c.readLine();
		int row, col;
		while (!in.equals("!")) {
			row = Integer.parseInt(in.charAt(0) + "");
			col = Integer.parseInt(in.charAt(1) + "");
			state.put(row, col, Alignment.opposite(state.lastMove));
			state.fireGuns(5);
			state.setLastMove(Alignment.opposite(state.lastMove));
			Value p = game.primitiveValue(state);
			if (p != Value.UNDECIDED) {
				if (p == Value.WIN) {
					System.out.println(Alignment.opposite(state.lastMove)
							+ " just won!");
					break;
				}
				if (p == Value.LOSE) {
					System.out.println(state.lastMove + " just won!");
					break;
				}
				if (p == Value.TIE) {
					System.out.println("Tie game!");
					break;
				}

			}
			System.out.println(game.displayState(state));
			in = c.readLine();
		}
		System.out.println("You're done!");

	}

	private static BufferedReader c = new BufferedReader(new InputStreamReader(
			System.in));
	private static String welcome = "Welcome to Alignment (4x4 - 5 to win)!\n   As of now, we don't have the full version completed, but a simpler subset is implemented and solved.\n  - <p>lay game\n  - <q>quit\n";

}
