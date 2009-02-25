package edu.berkeley.gamesman.testing.refactor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.PrimitiveValue;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordFields;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.util.Util;

/**
 * @author DNSpies Solves tiered mix hash-games
 * @param <T> The class of the game to be solved
 */
public class TieredMixGameHashSolver<T extends TieredMixGameHasher> extends
		MixGameHashSolver<T> {

	private class TieredGameHashWorkUnit implements WorkUnit {
		final Configuration conf;
		final T game;
		final int startTier, endTier; // Inclusive; startTier>endTier

		public TieredGameHashWorkUnit(Configuration conf, T game) {
			this.game = game;
			startTier = game.numberOfTiers() - 1;
			endTier = 0;
			this.conf = conf;
		}

		public TieredGameHashWorkUnit(Configuration conf, T game,
				int startTier, int endTier) {
			this.game = game;
			this.startTier = startTier;
			this.endTier = endTier;
			this.conf = conf;
		}

		public void conquer() {
			game.setTier(startTier);
			try {
				storeTier();
				for (int i = startTier - 1; i >= endTier; i--) {
					game.prevTier();
					storeTier();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void storeTier() throws Exception {
			game.storeOffset();
			storeHash();
			while (game.hasNextPositionInTier()) {
				game.nextPositionInTier();
				storeHash();
			}
		}

		private void storeHash() {
			PrimitiveValue pv = game.primitiveValue();
			Record r;
			if (pv == PrimitiveValue.Undecided) {
				Collection<BigInteger> moves = game.validMoveHashes();
				Iterator<BigInteger> moveIter = moves.iterator();
				Record best = null;
				while (moveIter.hasNext()) {
					r = db.getRecord(moveIter.next());
					if (best == null || r.isPreferableTo(best))
						best = r;
				}
				r = new Record(conf, best.get());
				r.set(RecordFields.Remoteness, best
						.get(RecordFields.Remoteness) + 1);
			} else {
				r = new Record(conf, pv);
				r.set(RecordFields.Remoteness, 0);
			}
			db.putRecord(game.getHash(), r);
		}

		public List<WorkUnit> divide(int num) {
			ArrayList<WorkUnit> units = new ArrayList<WorkUnit>(num);
			for (int i = 0; i < num; i++) {
				units
						.add(new TieredGameHashWorkUnit(
								conf,
								gameInstance(
										conf,
										Util
												.<Class<T>, Class<? extends TieredMixGameHasher>> checkedCast(game
														.getClass())),
								(endTier - startTier) * (i + 1) / num - 1,
								(endTier - startTier) * i / num));
			}
			return units;
		}
	}

	@Override
	public WorkUnit prepareSolve(Configuration config, Class<T> gameClass) {
		return new TieredGameHashWorkUnit(config, gameInstance(config,
				gameClass));
	}

	private T gameInstance(Configuration config, Class<T> gameClass) {
		Constructor<T> cons;
		try {
			cons = gameClass.getConstructor(Configuration.class);
			return cons.newInstance(config);
		} catch (SecurityException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		} catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
			return null;
		}
	}
}
