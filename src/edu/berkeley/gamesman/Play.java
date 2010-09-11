package edu.berkeley.gamesman;

import java.util.Collection;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.State;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.util.Pair;

public final class Play {
	public static void main(String[] args) {
		Database db = Database.openDatabase(args[0]);
		Configuration conf = db.getConfiguration();
		Game<? extends State> g = conf.getGame();
		playGame(g, db);
	}

	public static <S extends State> void playGame(Game<S> g, Database db) {
		S position = g.startingPositions().iterator().next();
		Scanner scan = new Scanner(System.in);
		Record storeRecord = new Record(db.getConfiguration());
		DatabaseHandle dh = db.getHandle();
		while (g.primitiveValue(position) == Value.UNDECIDED) {
			System.out.println(g.displayState(position));
			g.longToRecord(position, db.getRecord(dh, g.stateToHash(position)),
					storeRecord);
			System.out.println(storeRecord);
			Collection<Pair<String, S>> moves = g.validMoves(position);
			StringBuilder availableMoves = new StringBuilder(
					"Available Moves: ");
			for (Pair<String, S> move : moves) {
				availableMoves.append(move.car);
				availableMoves.append(", ");
			}
			System.out.println(availableMoves);
			String moveString = scan.nextLine();
			for (Pair<String, S> move : moves) {
				if (move.car.equals(moveString)) {
					position = move.cdr;
					break;
				}
			}
		}
		System.out.println(g.displayState(position));
		System.out.println("Game over");
	}
}
