package edu.berkeley.gamesman.master;

import java.math.BigInteger;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.hasher.Hasher;
import edu.berkeley.gamesman.solver.Solver;
import edu.berkeley.gamesman.util.ProgressMeter;
import edu.berkeley.gamesman.util.Util;

public final class LocalMaster implements Master,ProgressMeter {

	private long start;
	
	public void initialize(Class<? extends Game<?, ?>> gamec, Class<? extends Solver> solverc, Class<? extends Hasher> hasherc, Class<? extends Database<?>> databasec) {
		Game game = null;
		Solver solver = null;
		Hasher hasher = null;
		Database database = null;
		try{
			game = gamec.newInstance();
			solver = solverc.newInstance();
			hasher = hasherc.newInstance();
			database = databasec.newInstance();
		}catch(IllegalAccessException e){
			Util.fatalError("Fatal error while initializing: "+e);
		}catch (InstantiationException e) {
			Util.fatalError("Fatal error while initializing: "+e);
		}
		
		database.initialize(null);
		
		solver.setDatabase(database);
		game.setHasher(hasher);
		
		start = System.currentTimeMillis();
		
		solver.solve(game,this);
		
	}
	
	public void run() {
		System.out.println("Launched!");
	}

	public void progress(BigInteger completed, BigInteger total) {
		long elapsedMillis = System.currentTimeMillis() - start;
		BigInteger thousandpct = completed.divide(total.divide(BigInteger.valueOf(100000)));
		double pct = thousandpct.doubleValue()/1000;
		long totalMillis = (long)((double)elapsedMillis / pct);
		System.out.print("Completed "+completed+" of "+total+", "+pct+"% estimate "+Util.millisToETA(totalMillis-elapsedMillis)+" remains\r");
	}

}
