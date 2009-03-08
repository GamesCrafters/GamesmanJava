package edu.berkeley.gamesman.tool;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;

public class DatabaseCompare {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Configuration c1 = new Configuration(args[0]);
		Configuration c2 = new Configuration(args[1]);
		
		Database db1 = c1.openDatabase();
		Database db2 = c2.openDatabase();
		
		System.out.println("Comparing the databases by walking through the hash space of d1...");
		
		for(BigInteger i : db1.getConfiguration().getGame().lastHash()){
			
		}
	}

}
