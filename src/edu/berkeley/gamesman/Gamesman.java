/**
 * 
 */
package edu.berkeley.gamesman;

import edu.berkeley.gamesman.game.Game;
import edu.berkeley.gamesman.master.Master;

/**
 * @author Gamescrafters Project
 *
 */
public final class Gamesman {

	private Gamesman() {}
	
	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		OptionProcessor.initializeOptions(args);
		OptionProcessor.acceptOption("h", "help", false, "Display this help string and exit");
		OptionProcessor.nextGroup();
		OptionProcessor.acceptOption("x", "with-graphics", false, "Enables use of graphical displays");
		OptionProcessor.nextGroup();
		OptionProcessor.acceptOption("g", "game", true, "Specifies which game to play", "NullGame");
		OptionProcessor.acceptOption("s", "solver", true, "Specifies which solver to use", "TreeSolver");
		OptionProcessor.acceptOption("#", "hasher", true, "Specifies which hasher to use", "GenericHasher");
		OptionProcessor.acceptOption("d", "database", true, "Specifies which database backend to use", "FileDatabase");
		OptionProcessor.acceptOption("m", "master", true, "Specifies which master controller to use", "LocalMaster");
		OptionProcessor.nextGroup();

		
		String masterName = OptionProcessor.checkOption("master");
		
		Object omaster = null;
		try{
			omaster = Class.forName("edu.berkeley.gamesman.master."+masterName).newInstance();
		}catch(ClassNotFoundException cnfe){
			System.err.println("Could not load master controller '"+masterName+"': "+cnfe);
			System.exit(1);
		}catch(IllegalAccessException iae){
			System.err.println("Not allowed to access requested master '"+masterName+"': "+iae);
			System.exit(1);
		}catch(InstantiationException ie){
			System.err.println("Master failed to instantiate: "+ie);
			System.exit(1);
		}
		
		if(! (omaster instanceof Master)){
			System.err.println("Master does not implement master.Master interface");
			System.exit(1);
		}
		
		Master m = (Master) omaster;
		
		Util.debug("Preloading classes...");
		
		String gameName,solverName,hasherName,databaseName;
		
		gameName = OptionProcessor.checkOption("game");
		solverName = OptionProcessor.checkOption("solver");
		hasherName = OptionProcessor.checkOption("hasher");
		databaseName = OptionProcessor.checkOption("database");
		
		Class<?> g,s,h,d;
		
		try{
			g = Class.forName("edu.berkeley.gamesman.game."+gameName);
			s = Class.forName("edu.berkeley.gamesman.solver."+solverName);
			h = Class.forName("edu.berkeley.gamesman.hasher."+hasherName);
			d = Class.forName("edu.berkeley.gamesman.database."+databaseName);
		}catch(Exception e){
			System.err.println("Fatal error in preloading: "+e);
			return;
		}
		
		if(OptionProcessor.checkOption("h") != null){
			System.out.println("Gamesman help stub, please fill this out!"); // TODO: help text
			OptionProcessor.help();
			return;
		}
		
		m.initialize(g,s,h,d);
		m.launch();
		
	}
	

}
