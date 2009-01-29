package edu.berkeley.gamesman.master;

import org.apache.hadoop.util.ToolRunner;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Game;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.core.Master;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.hadoop.HadoopTool;
import edu.berkeley.gamesman.util.OptionProcessor;
import edu.berkeley.gamesman.util.Util;

public class HadoopMaster implements Master {

	public void initialize(Class<? extends Game<?>> game,
			Class<? extends Solver> solver, Class<? extends Hasher<?>> hasher,
			Class<? extends Database> database) {
	}

	public void run() {
		try {
			ToolRunner.run(new HadoopTool(), OptionProcessor.getAllOptions());
		} catch (Exception e) {
			e.printStackTrace(System.err);
			Util.fatalError("Hadoop tool runner threw an exception: "+e);
		}
	}

}
