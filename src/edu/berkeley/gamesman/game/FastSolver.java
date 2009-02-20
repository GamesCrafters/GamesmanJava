package edu.berkeley.gamesman.game;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.TieredHasher;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies
 * 
 */
public final class FastSolver extends TieredHasher<FastBoard> {
	private final int height, width, piecesToWin;
	private final FileDatabase fd;

	public FastSolver(Configuration conf) {
		super(conf);
		height = Integer.parseInt(conf.getProperty("gamesman.game.height"));
		width = Integer.parseInt(conf.getProperty("gamesman.game.width"));
		piecesToWin = Integer
				.parseInt(conf.getProperty("connect4.pieces", "4"));
		fd = new FileDatabase();
		fd.initialize("file:///tmp/database.db", conf);
		BigInteger offset;
		for (int tier = numberOfTiers() - 1; tier >= 0; tier--) {
			FastBoard fb = new FastBoard(height, width, tier);
			ArrayList<Integer> moveTiers = fb.moveTiers();
			ArrayList<BigInteger> moveOffsets = new ArrayList<BigInteger>(
					moveTiers.size());
			for (int i = 0; i < moveTiers.size(); i++) {
				moveOffsets.add(hashOffsetForTier(moveTiers.get(i)));
			}
			offset = hashOffsetForTier(tier);
			addHash(fb, fd, offset, moveOffsets, piecesToWin);
			System.out.println(fb.getHash());
			System.out.println(fb);
			while (fb.hasNext()) {
				fb.next();
				addHash(fb, fd, offset, moveOffsets, piecesToWin);
				System.out.println(fb.getHash());
				System.out.println(fb);
			}
		}
	}

	public static void main(String[] args) {
		Configuration conf = new Configuration(new Properties(System
				.getProperties()));
		if (args.length == 1)
			conf.addProperties(args[0]);
		FastSolver fs = new FastSolver(conf);
	}

	private static void addHash(FastBoard fb, FileDatabase fd,
			BigInteger tierOffset, ArrayList<BigInteger> moveOffsets,
			int piecesToWin) {
		Record r = fb.primitiveValue(piecesToWin);
		if (r == PrimitiveValue.Undecided) {
			Record bestMove = null;
			ArrayList<BigInteger> m = fb.moveHashes();
			for (int i = 0; i < m.size(); i++) {
				r = fd.getRecord(m.get(i).add(moveOffsets.get(i)));
				if (bestMove == null || r.isPreferableTo(bestMove))
					bestMove = r;
			}
			System.out.println(m);
		} else
			System.out.println(pv);
		fd.putRecord(fb.getHash().add(tierOffset), r);
	}

	@Override
	public FastBoard gameStateForTierAndOffset(int tier, BigInteger index) {
		FastBoard b = new FastBoard(height, width, tier);
		BigInteger i;
		for (i = BigInteger.ZERO; i.compareTo(index) < 0; i = i
				.add(BigInteger.ONE))
			b.next();
		return b;
	}

	@Override
	public BigInteger numHashesForTier(int tier) {
		FastBoard b = new FastBoard(height, width, tier);
		return b.maxHash();
	}

	@Override
	public int numberOfTiers() {
		return (int) Util.longpow(height + 1, width);
	}

	@Override
	public Pair<Integer, BigInteger> tierIndexForState(FastBoard state) {
		return new Pair<Integer, BigInteger>(state.getTier(), state.getHash());
	}

	@Override
	public String describe() {
		// TODO Auto-generated method stub
		return null;
	}
}
