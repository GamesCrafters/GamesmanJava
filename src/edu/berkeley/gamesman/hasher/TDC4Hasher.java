package edu.berkeley.gamesman.hasher;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Hasher;
import edu.berkeley.gamesman.game.TopDownC4;
import edu.berkeley.gamesman.game.util.C4State;
import edu.berkeley.gamesman.util.ExpCoefs;

public class TDC4Hasher extends Hasher<C4State> {

	private long[] offsets;

	public TDC4Hasher(Configuration conf) {
		super(conf);
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}

	private void setOffsets() {
		TopDownC4 game = (TopDownC4) conf.getGame();
		offsets = new long[game.gameSize + 2];
		ExpCoefs ec = game.ec;
		for (int i = 0; i < offsets.length; i++) {
//			ec.getCoef(n, k);
		}
	}

	@Override
	public long hash(C4State board) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long numHashes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public C4State unhash(long hash) {
		// TODO Auto-generated method stub
		return null;
	}

}
