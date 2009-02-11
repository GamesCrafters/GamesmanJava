package edu.berkeley.gamesman.master;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.Master;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.OptionProcessor;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.TaskFactory;
import edu.berkeley.gamesman.util.Util;

/**
 * LocalMaster runs one or more threads on the local machine to solve a game
 * @author Steven Schlansker
 */
public final class LocalMaster implements Master,TaskFactory {

	private Game<?> game;
	Solver solver;
	Hasher<?> hasher;
	Database database;
	
	Configuration conf;
	
	static{
		OptionProcessor.acceptOption("j", "threads", true, "The number of threads to launch", "1");
	}
	
	public void initialize(Configuration inconf, Class<? extends Solver> solverc, Class<? extends Database> databasec) {
		
		Task.setTaskFactory(this);
		
		try{
			game = inconf.getGame();
			solver = solverc.newInstance();
			hasher = inconf.getHasher();
			database = databasec.newInstance();
		}catch(IllegalAccessException e){
			Util.fatalError("Fatal error while initializing",e);
		}catch (InstantiationException e) {
			Util.fatalError("Fatal error while initializing",e);
		}
		
		conf = inconf;
		//conf = new Configuration(game,hasher,EnumSet.of(RecordFields.Value)); //TODO: have more than Value here
		
		database.initialize(OptionProcessor.checkOption("uri"),inconf);
		
		solver.setDatabase(database);
		game.initialize(conf);
		
		Util.debug(DebugFacility.Master,"Done initializing LocalMaster");
		
		
	}
	
	public void run() {
		System.out.println("Launched!");
		int threads = Integer.parseInt(OptionProcessor.checkOption("threads"));
		Util.debug(DebugFacility.Master,"Launching "+threads+" threads...");
		List<WorkUnit> list = solver.prepareSolve(conf,game).divide(threads);
		
		ArrayList<Thread> myThreads = new ArrayList<Thread>();
		
		ThreadGroup solverGroup = new ThreadGroup("Solver Group: "+game);
		for(WorkUnit w : list){
			Thread t = new Thread(solverGroup,new LocalMasterRunnable(w));
			t.start();
			myThreads.add(t);
		}
		
		for(Thread t : myThreads)
			try{
				t.join();
			}catch (InterruptedException e) {
				Util.warn("Interrupted while joined on thread "+t);
			}
		//System.out.println(myThreads);
		database.close();
		Util.debug(DebugFacility.Master, "Finished master run");
	}
	
	private class LocalMasterRunnable implements Runnable {
		WorkUnit w;
		LocalMasterRunnable(WorkUnit u){
			w = u;
		}
		
		public void run(){
			Util.debug(DebugFacility.Master,"LocalMasterRunnable begin");
			w.conquer();
			Util.debug(DebugFacility.Master,"LocalMasterRunnable end");
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

	/**
	 * @return the game
	 */
	public Game<?> getGame() {
		return game;
	}

}
