package edu.berkeley.gamesman.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredGame;
import edu.berkeley.gamesman.game.connect4.C4State;
import edu.berkeley.gamesman.game.connect4.OneTierC4Board;
import edu.berkeley.gamesman.hasher.FastConnect4Hasher;
import edu.berkeley.gamesman.util.DependencyResolver;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class FastConnect4 extends TieredGame<OneTierC4Board> {

	public final int piecesToWin;

	static {
		DependencyResolver.allowHasher(FastConnect4.class,
				FastConnect4Hasher.class);
	}
	
	public FastConnect4(Configuration conf) {
		super(conf);
		piecesToWin = Integer.parseInt(conf.getProperty("connect4.pieces", "4"));
	}

	@Override
	public int getDefaultBoardHeight() {
		return 6;
	}

	@Override
	public int getDefaultBoardWidth() {
		return 7;
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String displayState(OneTierC4Board pos) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] pieces() {
		return new char[] {'X','O'};
	}

	@Override
	public PrimitiveValue primitiveValue(OneTierC4Board pos) {
		return pos.primitiveValue();
	}

	@Override
	public Collection<OneTierC4Board> startingPositions() {
		ArrayList<OneTierC4Board> a = new ArrayList<OneTierC4Board>();
		a.add(new OneTierC4Board(gameWidth,gameHeight,piecesToWin,0));
		return a;
	}

	@Override
	public String stateToString(OneTierC4Board pos) {
		return pos.toString();
	}

	@Override
	public OneTierC4Board stringToState(String pos) {
		Util.fatalError("Not yet implemented");
		return null;
	}

	@Override
	public Collection<Pair<String, OneTierC4Board>> validMoves(
			OneTierC4Board pos) {
		FastConnect4Hasher h = (FastConnect4Hasher) conf.getHasher();
		ArrayList<Pair<String,OneTierC4Board>> al = new ArrayList<Pair<String,OneTierC4Board>>();
		Iterator<Pair<Integer, C4State>> moves = pos.validMoves().iterator();
		
		while(moves.hasNext()){
			Pair<Integer,C4State> m = moves.next();
			OneTierC4Board ob = h.gameStateForTierAndOffset(m.cdr.tier(), m.cdr.hash());
			al.add(new Pair<String,OneTierC4Board>("c"+m.car,ob));
		}
		
		return al;
	}

	
}
