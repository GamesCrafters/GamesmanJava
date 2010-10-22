package                                                                                                                                                                                                                                                                                      edu.berkeley.gamesman.solver;

import java.util.List;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Value;
import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.MemoryDatabase;
import edu.berkeley.gamesman.game.LoopyMutaGame;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.RecycleLinkedList;

/**
 * @author David, Brent, Nancy, Kevin, Peter, Sharmishtha, Raji
 *
 */
public class LoopySolver extends Solver {
	Pool<Record> recordPool;
	
	protected RecycleLinkedList<Record[]> recordList; //Nancy: added this for else clause in solve loopy game function

	public LoopySolver(Configuration conf) {
		super(conf);
	}

	@Override
	public WorkUnit prepareSolve(final Configuration conf) {
		final LoopyMutaGame game = (LoopyMutaGame) conf.getGame();
		recordPool = new Pool<Record>(new Factory<Record>() {

			public Record newObject() {
				return game.getRecord();
			}

			public void reset(Record t) {
				t.value = Value.UNDECIDED;
			}

		});
		long hashSpace = game.numHashes();
		Record defaultRecord = game.getRecord();
		defaultRecord.value = Value.IMPOSSIBLE;
		writeDb.fill(conf.getGame().recordToLong(null, defaultRecord), 0,
				hashSpace);

		return new WorkUnit() {

			public void conquer() {
				solve(conf);
			}

			public List<WorkUnit> divide(int num) {
				throw new UnsupportedOperationException();
			}

		};
	}

	public void solve(Configuration conf) {
		LoopyMutaGame game = (LoopyMutaGame) conf.getGame();
		for (int startNum = 0; startNum < game.numStartingPositions(); startNum++) {
			game.setStartingPosition(startNum);
			solve(game, game.getRecord(), 0, readDb.getHandle(),
					writeDb.getHandle());
		}
	}

	private void solve(LoopyMutaGame game, Record value, int depth,
			DatabaseHandle readDh, DatabaseHandle writeDh) {
		long hash = game.getHash();
		game.longToRecord(readDb.getRecord(readDh, hash), value);
		Record bestValue;

		switch (value.value) {
		case IMPOSSIBLE:
			value.value = game.primitiveValue();
			if (value.value != Value.UNDECIDED) {
				value.remoteness = 0;
				writeDb.putRecord(writeDh, hash, game.recordToLong(value));
				value.previousPosition();
				int numParents = game.unmakeMove();
				for (int parent = 0; parent < numParents; parent++) {
					fix(game, value, readDh, writeDh);
					game.changeUnmakeMove();
				}
				game.remakeMove();
			} else {
				value.value = Value.DRAW;
				writeDb.putRecord(writeDh, hash, game.recordToLong(value));
				bestValue = recordPool.get();
				boolean unassigned = true;
				int numChildren = game.makeMove();
				for (int child = 0; child < numChildren; child++) {
					solve(game, value, depth + 1, readDh, writeDh);
					if (value.value == Value.UNDECIDED) {
						game.longToRecord(readDb.getRecord(readDh, hash), bestValue);
					} else {
						if (unassigned || value.value.isPreferableTo(bestValue.value)) {
							unassigned = false;
							bestValue.set(value);
							writeDb.putRecord(writeDh, hash,
									game.recordToLong(bestValue));
						}
					}
					game.changeMove();
				}
				value.set(bestValue);
				recordPool.release(bestValue);
				value.previousPosition();
				int numParents = game.unmakeMove();
				for (int parent = 0; parent < numParents; parent++) {
					fix(game, value, readDh, writeDh);
					game.changeUnmakeMove();
				}
				game.remakeMove();
			}
			value.value = Value.UNDECIDED;
			break;
		default:
			value.previousPosition();
			break;
		}
	}

	private void fix(LoopyMutaGame game, Record value, DatabaseHandle readDh,
			DatabaseHandle writeDh) {

		Record dbValue = recordPool.get();
		long hash = game.getHash();
		game.longToRecord(readDb.getRecord(readDh, hash), dbValue);
		boolean update = false;

		switch (value.value) {
		case IMPOSSIBLE:
			return;
		default:
			if (Value.DRAW.isPreferableTo(dbValue.value)) {
				throw new Error("Draw should not be > database Value");
			} else if (dbValue.value.isPreferableTo(Value.DRAW)) {
				writeDb.putRecord(writeDh, hash, game.recordToLong(value));
			} else if (dbValue.value == Value.DRAW) {
				boolean unassigned = true;
				int numChildren = game.makeMove();
				Record childValue = recordPool.get();
				for (int child = 0; child < numChildren; child++) {
					game.longToRecord(readDb.getRecord(readDh, hash),
							childValue);
					if (childValue.value == Value.DRAW) {
						break;
					} else if (childValue.value.isPreferableTo(Value.DRAW)) {
						throw new Error("childValue should not be > DRAW");
					} else if (unassigned
							|| childValue.value.isPreferableTo(value.value)) {
						value.set(childValue);
						update = true;
					}
					game.changeMove();
				}
				value.previousPosition();
				writeDb.putRecord(writeDh, hash, game.recordToLong(value));
			}
			if (update) {
				value.previousPosition();
				int numParents = game.unmakeMove();
				for (int parent = 0; parent < numParents; parent++) {
					fix(game, value, readDh, writeDh);
					game.changeUnmakeMove();
				}
				game.remakeMove();
			}
		}
	}
}