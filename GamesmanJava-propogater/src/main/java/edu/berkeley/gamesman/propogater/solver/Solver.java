package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.common.IOCheckOperations;
import edu.berkeley.gamesman.propogater.tree.Tree;
import edu.berkeley.gamesman.propogater.tree.TreeNode;

public class Solver<K extends WritableComparable<K>> {
	private static final int CREATE_COMBINE = 0, PROPOGATE = 1, CLEANUP = 2;
	public static final PathFilter underscoreFilter = new PathFilter() {
		@Override
		public boolean accept(Path path) {
			return !path.getName().startsWith("_");
		}
	};
	private static final String START_FILE_NAME = "startFile";

	private final Configuration conf;
	private final TierGraph myGraph;
	private final Tree<K, ?, ?, ?, ?, ?> tree;
	private final Set<Tier> rootTiers;
	private final TaskManager taskManager;
	private int stage;

	public Solver(Configuration conf) throws IOException {
		this.conf = conf;
		tree = ConfParser
				.<K, Writable, Writable, Writable, Writable, Writable> newTree(conf);
		tree.prepareRun(conf);
		taskManager = new TaskManager(tree.isSingleLinear());
		myGraph = new TierGraph(tree);
		Path workPath = ConfParser.getWorkPath(conf);
		FileSystem fs = ConfParser.getWorkFileSystem(conf);
		boolean starting = IOCheckOperations.mkdirs(fs, workPath);
		Collection<K> keyRoots = tree.getRoots();
		if (starting) {
			start(fs, keyRoots);
		}
		HashSet<Tier> rootTiers = new HashSet<Tier>();
		for (K root : keyRoots) {
			Tier tier = myGraph.getTier(tree.getDivision(root));
			if (tier == null)
				throw new NullPointerException();
			rootTiers.add(tier);
		}
		this.rootTiers = Collections.unmodifiableSet(rootTiers);
	}

	public synchronized void findAll() throws IOException {
		HashSet<Integer> visited = new HashSet<Integer>();
		switch (stage) {
		case CREATE_COMBINE:
			visited.clear();
			for (Tier tier : rootTiers) {
				findCreateCombine(tier, visited);
			}
			break;
		case PROPOGATE:
			visited.clear();
			for (Tier tier : rootTiers) {
				findPropogate(tier, visited);
			}
			break;
		case CLEANUP:
			cleanup();
			break;
		default:
			throw new RuntimeException("Unrecognized stage: " + stage);
		}
	}

	private void cleanup() throws IOException {
		taskManager.add(new CleanupRunner(conf, myGraph));
	}

	private boolean findPropogate(Tier tier, HashSet<Integer> visited)
			throws IOException {
		boolean result = false;
		if (visited.contains(tier.num))
			return false;
		else
			visited.add(tier.num);
		if (tier.needsToPropogate()) {
			SortedSet<Tier> cycle = tier.getCycle();
			taskManager.add(new PropogateRunner(conf, myGraph, cycle));
			result = true;
			for (Tier t : cycle) {
				visited.add(t.num);
			}
			for (Tier t : cycle) {
				for (Tier child : t.getChildren()) {
					findPropogate(child, visited);
				}
			}
		} else {
			for (Tier child : tier.getChildren()) {
				result |= findPropogate(child, visited);
			}
		}
		return result;
	}

	private boolean findCreateCombine(Tier tier, HashSet<Integer> visited)
			throws IOException {
		boolean result = false;
		if (visited.contains(tier.num))
			return false;
		else
			visited.add(tier.num);
		if (tier.needsToCombine()) {
			taskManager.add(new CombineRunner(conf, tier, myGraph));
			result = true;
		}
		if (tier.needsToCreate()) {
			taskManager.add(new CreateRunner(conf, tier, myGraph));
			result = true;
		}
		for (Tier child : tier.getChildren()) {
			result |= findCreateCombine(child, visited);
		}
		return result;
	}

	public void run() throws InterruptedException, IOException {
		stage = CREATE_COMBINE;
		while (true) {
			findAll();
			if (taskManager.run())
				taskManager.nextEvent();
			else
				break;
		}
		assert taskManager.hasNone();
		stage = PROPOGATE;
		while (true) {
			findAll();
			if (taskManager.run())
				taskManager.nextEvent();
			else
				break;
		}
		assert taskManager.hasNone();
		stage = CLEANUP;
		findAll();
		taskManager.run();
	}

	private void start(FileSystem fs, Collection<K> keyRoots)
			throws IOException {
		HashMap<Integer, SequenceFile.Writer> startWriters = new HashMap<Integer, SequenceFile.Writer>();
		TreeNode<K, ?, ?, ?, ?, ?> startingNode = tree.newNode();
		for (K key : keyRoots) {
			int num = tree.getDivision(key);
			SequenceFile.Writer writer = startWriters.get(num);
			if (writer == null) {
				writer = new SequenceFile.Writer(fs, conf, new Path(
						myGraph.getTier(num).dataPath, START_FILE_NAME),
						tree.getKeyClass(), tree.getTreeNodeClass());
				startWriters.put(num, writer);
			}
			writer.append(key, startingNode);
		}
		for (Map.Entry<Integer, SequenceFile.Writer> pair : startWriters
				.entrySet()) {
			myGraph.getTierOrNull(pair.getKey()).setNeedsCreation();
			pair.getValue().close();
		}
	}
}
