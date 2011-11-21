package edu.berkeley.gamesman.game.graph;

import java.util.TreeSet;
import java.util.Comparator;

public abstract class GenGraph {
	private static final Comparator<GenNode> nodeCompare = new Comparator<GenNode>() {
		@Override
		public int compare(GenNode o1, GenNode o2) {
			return o1.nodeNum < o2.nodeNum ? -1 : (o1.nodeNum > o2.nodeNum ? 1
					: 0);
		}
	};
	private final GenNode root;
	private final TreeSet<GenNode> toSolve = new TreeSet<GenNode>(nodeCompare);
	private int numNodes = 0;
	private int numSCCs = 0;

	public GenGraph(GenNode root) {
		this.root = root;
	}

	public final void prepareSolve() {
		addSolve(root, 0);
	}

	public final GenNode nextSolve() {
		return toSolve.pollFirst();
	}

	public final void finishSolve(GenNode node, boolean changed) {
		if (changed) {
			for (GenNode parent : node.allParents) {
				toSolve.add(parent);
			}
		}
	}

	private int addSolve(GenNode node, int depth) {
		if (node.nodeNum != -1) {
			return depth;
		} else if (node.depth != -1) {
			return node.depth;
		}
		node.depth = depth;
		int lowest = depth;
		for (int i = 0; i < node.numChildren; i++) {
			GenNode child = node.getChild(i).child;
			child.allParents.add(node);
			int res = addSolve(child, depth + 1);
			if (res < lowest) {
				lowest = res;
			}
		}
		node.depth = -1;
		if (lowest == depth)
			addCycleSolve(node, numSCCs++);
		return lowest;
	}

	private void addCycleSolve(GenNode node, int sccNum) {
		if (node.nodeNum != -1)
			return;
		node.nodeNum = numNodes++;
		for (int i = 0; i < node.numChildren; i++) {
			GenNode child = node.getChild(i).child;
			addCycleSolve(child, sccNum);
		}
	}
}
