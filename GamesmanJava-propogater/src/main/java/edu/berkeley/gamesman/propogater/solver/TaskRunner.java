package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.propogater.common.Util;


abstract class TaskRunner implements Runnable {
	public static final int COMBINE = 0, CREATE = 1, PROPOGATE = 2,
			CLEANUP = 3;
	public static final int NUM_TYPES = 4;
	public final int type;
	protected final Configuration conf;
	protected final TierGraph myGraph;

	TaskRunner(Configuration conf, int type, TierGraph graph) {
		assert type >= 0 && type <= 4;
		this.conf = conf;
		this.type = type;
		myGraph = graph;
	}

	protected void splitUp(Tier tier) throws IOException {
		while (true) {
			Path[] files = tier.getCreationOutputFiles(Solver.underscoreFilter);
			if (files.length == 0)
				break;
			Path file = files[0];
			String name = file.getName();
			String ext = name.substring(name.lastIndexOf("."));
			Tier nextTier = getTier(ext);
			if (!nextTier.filter.accept(file)) {
				throw new Error(nextTier.filter + " does not accept " + file);
			}
			updateEdges(tier, nextTier);
			tier.createTempFolder();
			Path[] sameTier = tier.getCreationOutputFiles(nextTier.filter);
			assert Util.contains(sameTier, file);
			for (Path n : sameTier)
				tier.moveToTemp(n);
			putBack(tier, nextTier);
		}
	}

	protected Tier getTier(String ext) throws IOException {
		throw new UnsupportedOperationException();
	}

	protected void putBack(Tier from, Tier to) throws IOException {
		throw new UnsupportedOperationException();
	}

	protected void updateEdges(Tier tier, Tier nextTier) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void run() {
		try {
			runTask();
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	protected abstract void runTask() throws Throwable;
}
