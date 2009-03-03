package edu.berkeley.gamesman.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.TreeMap;
import java.util.Map.Entry;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.PrimitiveValue;
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
		//w.println("\tfontname = \"Courier\";");
		for(BigInteger i : Util.bigIntIterator(gm.lastHash())){
			S parent = gm.hashToState(i);
			TreeMap<String, String> attrs = new TreeMap<String, String>();

			PrimitiveValue v = db.getRecord(i).get();
			attrs.put("label","<"+i+"<br/>"+gm.displayState(parent).replaceAll("\n", "<br/>")+"<br/>"+v+" >");
			
			String color = "";
			switch(v){
			case Undecided:
				color = "black";
				break;
			case Lose:
				color = "red";
				break;
			case Win:
				color = "green";
				break;
			case Tie:
				color = "yellow";
				break;
			}
			
			attrs.put("color",color);
			attrs.put("fontname","courier");
			
			PrimitiveValue pv = gm.primitiveValue(parent);
			if(!pv.equals(PrimitiveValue.Undecided))
				attrs.put("style","filled");
			
			Util.assertTrue(pv.equals(PrimitiveValue.Undecided) || pv.equals(v), "Primitive values don't match!");
			
			w.print("\th"+i+" [ ");
			boolean didOne = false;
			for(Entry<String, String> attr : attrs.entrySet()){
				w.print((didOne?',':' ')+" "+attr.getKey()+" = "+attr.getValue());
				didOne = true;
			}
			w.println(" ];");
			for(Pair<String,S> child : gm.validMoves(parent)){
				BigInteger ch = gm.stateToHash(child.cdr);
				w.println("\th"+i+" -> h"+ch+" [ label = \""+child.car+"\" ];");
			}
		}
		w.println("}");
		
		w.close();
	}

}
