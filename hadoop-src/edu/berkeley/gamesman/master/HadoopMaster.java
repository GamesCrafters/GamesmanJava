package edu.berkeley.gamesman.master;

import org.apache.hadoop.util.ToolRunner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Master;
import edu.berkeley.gamesman.core.Solver;
import edu.berkeley.gamesman.hadoop.TieredHadoopTool;
import edu.berkeley.gamesman.util.Util;

/**
 * HadoopMaster loads a game into the Hadoop Map-Reduce core for solving
 * @author Steven Schlansker
 */
public class HadoopMaster implements Master {
	
	private Configuration conf;

	public void initialize(Configuration conf1,
			Class<? extends Solver> solver,
			Class<? extends Database> database,
			boolean whyDoesThisInterfaceKeepChanging) {
		this.conf = conf1;
	}

	public void run(boolean close) {
		run();
	}
	
	public void run() {
		try {
			ToolRunner.run(new TieredHadoopTool(), new String[]{Util.encodeBase64(conf.store())});
		} catch (Exception e) {
			Util.fatalError("Hadoop tool runner threw an exception: ",e);
		}
	}

}
