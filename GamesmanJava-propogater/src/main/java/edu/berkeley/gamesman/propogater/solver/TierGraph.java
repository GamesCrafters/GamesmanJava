package edu.berkeley.gamesman.propogater.solver;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.propogater.common.ConfParser;
import edu.berkeley.gamesman.propogater.tree.Tree;

public class TierGraph {
	private final Tree<?, ?, ?, ?, ?, ?> tree;
	private final HashMap<Integer, Tier> tierMap = new HashMap<Integer, Tier>();

	public TierGraph(Tree<?, ?, ?, ?, ?, ?> tree) {
		this.tree = tree;
	}

	public Tier getTier(int t) throws IOException {
		return getTier(t, false);
	}

	public Tier getTierOrNull(int t) throws IOException {
		return getTier(t, true);
	}

	private synchronized Tier getTier(int t, boolean orNull) throws IOException {
		Tier result = tierMap.get(t);
		if (!orNull && result == null) {
			result = new Tier(tree.getConf(), t, this);
			tierMap.put(t, result);
			for (int child : tree.getChildren(t)) {
				getTier(child).addParent(t);
				result.addChild(child);
			}
		}
		return result;
	}

	public Tier getTier(String ext) throws IOException {
		return getTier(getTierNum(ext));
	}

	private static int getTierNum(String ext) {
		int extLen = ConfParser.EXTENSION_PREF.length();
		String pref = ext.substring(0, extLen);
		if (!pref.equals(ConfParser.EXTENSION_PREF))
			throw new RuntimeException("Not a valid tier extension: " + ext);
		return Integer.parseInt(ext.substring(extLen));
	}

	public static Path[] mixPaths(Set<Tier> tiers) {
		Path[] allPaths = new Path[tiers.size()];
		int i = 0;
		for (Tier tier : tiers) {
			allPaths[i++] = tier.dataPath;
		}
		return allPaths;
	}

	public Collection<Tier> getTiers() {
		return tierMap.values();
	}

	public Tier getTierOrNull(String ext) throws IOException {
		return getTierOrNull(getTierNum(ext));
	}

}
