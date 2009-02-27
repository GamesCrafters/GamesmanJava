package edu.berkeley.gamesman.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class DatabaseDump {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static <S> void main(String[] args) throws IOException {
		Configuration conf = new Configuration(System.getProperties());
		Game<S> gm;
		if(args.length > 0)
			conf.addProperties(args[0]);
		Database db = new FileDatabase();
		db.initialize(conf.getPropertyWithPrompt("gamesman.db.uri"), null);
		
		PrintWriter w = new PrintWriter(new FileWriter(conf.getPropertyWithPrompt("gamesman.outputfile")));
		
		gm = Util.checkedCast(db.getConfiguration().getGame());
		
		System.out.println("Running, this may take a while...");
		
		w.println("digraph gamesman_dump {");
		for(BigInteger i : Util.bigIntIterator(gm.lastHash())){
			S parent = gm.hashToState(i);
			w.println("\th"+i+" [ label = <"+gm.displayState(parent).replaceAll("\n", "<br/>")+"> ];");
			for(Pair<String,S> child : gm.validMoves(parent)){
				BigInteger ch = gm.stateToHash(child.cdr);
				w.println("\th"+i+" -> h"+ch+" [ label = \""+child.car+"\" ];");
			}
		}
		w.println("}");
		
		w.close();
	}

}
