package edu.berkeley.gamesman.master;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.hasher.Hasher;
import edu.berkeley.gamesman.solver.Solver;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.TaskFactory;
import edu.berkeley.gamesman.util.Util;

public final class LocalMaster implements Master,TaskFactory {

	
	public void initialize(Class<? extends Game<?, ?>> gamec, Class<? extends Solver> solverc, Class<? extends Hasher> hasherc, Class<? extends Database> databasec) {
		Game<?,?> game = null;
		Solver solver = null;
		Hasher hasher = null;
		Database database = null;
		
		Task.setTaskFactory(this);
		
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
		
		
		solver.solve(game);
		
	}
	
	public void run() {
		System.out.println("Launched!");
	}

	private class LocalMasterTextTask extends Task {
		private String name;
		LocalMasterTextTask(String name){ this.name = name; }
		private long start;
		@Override
		protected void begin() {
			start = System.currentTimeMillis();
		}
		@Override
		public void complete() {
			System.out.println("\nCompleted task "+name+".");
		}
		@Override
		public void update() {
			long elapsedMillis = System.currentTimeMillis() - start;
			double thousandpct = completed.doubleValue() / (total.doubleValue()/100000);
			double pct = thousandpct/1000;
			long totalMillis = (long)((double)elapsedMillis * 100 / pct);
			System.out.print("Task: "+name+", "+String.format("%4.02f",pct)+"% ETA "+Util.millisToETA(totalMillis-elapsedMillis)+" remains\r");
		}
	}
	
	public Task createTask(String name) {
		return new LocalMasterTextTask(name);
	}

}
