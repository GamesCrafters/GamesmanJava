package edu.berkeley.gamesman.database;

/**
 * A simple class with a main method which returns a database's reported size
 * 
 * @author dnspies
 */
public class ReadLength {
	/**
	 * @param args The filename of the database
	 */
	public static void main(String[] args) {
		Database db = Database.openDatabase(args[0]);
		System.out.println(db.getSize());
		db.close();
	}
}
