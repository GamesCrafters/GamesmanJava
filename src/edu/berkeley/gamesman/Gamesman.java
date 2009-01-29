/**
 * 
 */
package edu.berkeley.gamesman;

import edu.berkeley.gamesman.game.Game;

/**
 * @author Gamescrafters Project
 *
 */
public final class Gamesman {

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
		OptionProcessor.acceptOption("m", "master", true, "Specifies which master controller to use", "DefaultMaster");
		OptionProcessor.nextGroup();

		
		String gameName = OptionProcessor.checkOption("game");
		
		Object ogame = null;
		try{
			ogame = Class.forName("edu.berkeley.gamesman.game."+gameName).newInstance();
		}catch(ClassNotFoundException cnfe){
			System.err.println("Could not load game '"+gameName+"': "+cnfe);
			System.exit(1);
		}catch(IllegalAccessException iae){
			System.err.println("Not allowed to access requested game '"+gameName+"': "+iae);
			System.exit(1);
		}catch(InstantiationException ie){
			System.err.println("Game failed to instantiate: "+ie);
			System.exit(1);
		}
		
		if(! (ogame instanceof Game)){
			System.err.println("Game does not implement game.Game interface");
			System.exit(1);
		}
		
		Game game = (Game) ogame;
		
		
		if(OptionProcessor.checkOption("h") != null){
			System.out.println("Gamesman help stub, please fill this out!"); // TODO: help text
			OptionProcessor.help();
			return;
		}
		
		
	}

}
