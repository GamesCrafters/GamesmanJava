package edu.berkeley.gamesman.game.graph;

import java.util.ArrayList;

import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;
import edu.berkeley.gamesman.hasher.genhasher.GenHasher;

public class GenNode {
	public final GenHasher<?> myHasher;
	public final int numChildren;
	private final GenEdge[] allChildren;
	private int moveCount = 0;
	final ArrayList<GenNode> allParents = new ArrayList<GenNode>();
	int depth = -1;
	int nodeNum = -1;

	public GenNode(GenHasher<?> hasher, int numMoves) {
		myHasher = hasher;
		numChildren = numMoves;
		allChildren = new GenEdge[numMoves];
	}

	public int addChild(CacheMove move, GenNode child) {
		if (moveCount == allChildren.length) {
			throw new Error("All children already set");
		}
		allChildren[moveCount++] = new GenEdge(this, move, child);
		return moveCount - 1;
	}

	public GenEdge getChild(int i) {
		return allChildren[i];
	}
}
