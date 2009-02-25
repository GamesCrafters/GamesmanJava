package edu.berkeley.gamesman.hasher;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.game.FastConnect4;
import edu.berkeley.gamesman.game.connect4.OneTierC4Board;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class FastConnect4Hasher extends TieredHasher<OneTierC4Board> {

	public FastConnect4Hasher(Configuration conf) {
		super(conf);
	}

	@Override
	public OneTierC4Board gameStateForTierAndOffset(int tier, BigInteger index) {
		FastConnect4 game = (FastConnect4) conf.getGame();
		OneTierC4Board b = new OneTierC4Board(game.getGameWidth(),game.getGameHeight(),game.piecesToWin,tier);
		b.unhash(index);
		return b;
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		FastConnect4 game = (FastConnect4) conf.getGame();
		return new OneTierC4Board(game.getGameWidth(),game.getGameHeight(),game.piecesToWin,tier).numHashesForTier();
	}

	@Override
	public int numberOfTiers() {
		return (int) Util.longpow((conf.getGame().getGameHeight() + 1), conf.getGame().getGameWidth());
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(OneTierC4Board state) {
		return new Pair<Integer,BigInteger>(state.getTier(),state.getHash());
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

}
