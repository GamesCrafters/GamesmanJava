package edu.berkeley.gamesman.master;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.Master;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.util.OptionProcessor;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.TaskFactory;
import edu.berkeley.gamesman.util.Util;

public final class LocalMaster implements Master,TaskFactory {

	Game<?,?> game;
	Solver solver;
	Hasher hasher;
	Database database;
	
	static{
		OptionProcessor.acceptOption("u", "uri", true, "The URI or relative path of the databse", "out.db");
	}
	
	public void initialize(Class<? extends Game<?, ?>> gamec, Class<? extends Solver> solverc, Class<? extends Hasher> hasherc, Class<? extends Database> databasec) {
		
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
		
		database.initialize(OptionProcessor.checkOption("uri"),game.getDBValueExample());
		
		solver.setDatabase(database);
		game.setHasher(hasher);
		
		
	}
	
	public void run() {
		System.out.println("Launched!");
		solver.solve(game);
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
