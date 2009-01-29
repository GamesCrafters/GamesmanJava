package edu.berkeley.gamesman.master;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.Master;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.util.OptionProcessor;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.TaskFactory;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.threading.Barrier;

public final class LocalMaster implements Master,TaskFactory {

	Game<?,?> game;
	Solver solver;
	Hasher hasher;
	Database database;
	
	static{
		OptionProcessor.acceptOption("u", "uri", true, "The URI or relative path of the databse", "out.db");
		OptionProcessor.acceptOption("j", "threads", true, "The number of threads to launch", "1");
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
		Barrier b = new Barrier();
		int threads = Integer.parseInt(OptionProcessor.checkOption("threads"));
		Util.debug("Launching "+threads+" threads...");
		List<WorkUnit> list = solver.prepareSolve(game).divide(threads);
		
		ArrayList<Thread> myThreads = new ArrayList<Thread>();
		for(WorkUnit w : list){
			Thread t = new Thread(new LocalMasterRunnable(w));
			t.start();
			myThreads.add(t);
		}
		
		for(Thread t : myThreads)
			try{
				t.join();
			}catch (InterruptedException e) {
				Util.warn("Interrupted while joined on thread "+t);
			}
	}
	
	private class LocalMasterRunnable implements Runnable {
		WorkUnit w;
		LocalMasterRunnable(WorkUnit u){
			w = u;
		}
		
		public void run(){
			Util.debug("ploink");
			w.conquer();
			Util.debug("plink");
		}
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
			System.out.println("\nCompleted task "+name+" in "+Util.millisToETA(System.currentTimeMillis()-start)+".");
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
