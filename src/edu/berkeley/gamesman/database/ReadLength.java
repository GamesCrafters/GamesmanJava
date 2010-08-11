package edu.berkeley.gamesman.database;

public class ReadLength {
	public static void main(String[] args) {
		Database db = Database.openDatabase(args[0]);
		System.out.println(db.getSize());
		db.close();
	}
}
