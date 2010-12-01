package edu.berkeley.gamesman.solver;

import java.util.concurrent.BrokenBarrierException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Reversi;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

public class ReversiSolver extends TierSolver {

	public ReversiSolver(Configuration conf) {
		super(conf);
		// TODO Auto-generated constructor stub
	}

	public void secondRun(Configuration conf, long start, long hashes,
			TierSolverUpdater t, Database readDb, DatabaseHandle readDh,
			Database writeDb, DatabaseHandle writeDh) {
		Reversi game = (Reversi) conf.getGame();
		long otherSide = game.numHashesForTier(tier) / 2;
		Record r1 = game.newRecord(), r2 = game.newRecord();
		for (long pos = start; pos < start + hashes; pos++) {
			game.longToRecord(null, readDb.getRecord(readDh, pos), r1);
			game.longToRecord(null, readDb.getRecord(readDh, pos + otherSide),
					r2);
			if (r1.remoteness == 0 && r2.remoteness != 0) {
				r1.set(r2);
				r1.previousPosition();
				writeDb.putRecord(writeDh, pos, game.recordToLong(null, r1));
			}
			if (r2.remoteness == 0 && r1.remoteness != 0) {
				r2.set(r1);
				r2.previousPosition();
				writeDb.putRecord(writeDh, pos + otherSide,
						game.recordToLong(null, r2));
			}
		}
	}

	class ReversiSolverWorkUnit extends TierSolverWorkUnit {

		ReversiSolverWorkUnit(Configuration conf) {
			super(conf);
		}

		public void conquer() {
			assert Util.debug(DebugFacility.SOLVER, "Started the solver... ("
					+ index + ")");
			Thread.currentThread().setName(
					"Solver (" + index + "): " + conf.getGame().describe());
			Pair<Long, Long> slice;
			finalRun = false;
			while ((slice = nextSlice(conf)) != null) {
				thisSlice = slice;
				DatabaseHandle myWrite = writeDb.getHandle();
				DatabaseHandle readHandle;
				if (readDb == null)
					readHandle = null;
				else
					readHandle = readDb.getHandle();
				solvePartialTier(conf, slice.car, slice.cdr, updater, readDb,
						readHandle, writeDb, myWrite);
				writeDb.closeHandle(myWrite);
			}
			if (barr != null)
				try {
					barr.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
			finalRun = true;
			while ((slice = nextSlice(conf)) != null) {
				thisSlice = slice;
				DatabaseHandle myWrite = writeDb.getHandle();
				DatabaseHandle readHandle;
				if (readDb == null)
					readHandle = null;
				else
					readHandle = readDb.getHandle();
				secondRun(conf, slice.car, slice.cdr, updater, readDb,
						readHandle, writeDb, myWrite);
				writeDb.closeHandle(myWrite);
			}
			if (barr != null)
				try {
					barr.await();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (BrokenBarrierException e) {
					e.printStackTrace();
				}
		}
	}
}
