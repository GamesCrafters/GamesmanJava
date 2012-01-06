package edu.berkeley.gamesman.game.graph;

import edu.berkeley.gamesman.hasher.cachehasher.CacheMove;

public class GenEdge {
	public final CacheMove move;
	public final GenNode parent;
	public final GenNode child;

	public GenEdge(GenNode parent, CacheMove move, GenNode child) {
		this.parent = parent;
		this.move = move;
		this.child = child;
	}
}
