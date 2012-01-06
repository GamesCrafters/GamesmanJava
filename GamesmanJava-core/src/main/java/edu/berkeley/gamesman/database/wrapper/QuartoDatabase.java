package edu.berkeley.gamesman.database.wrapper;

import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.game.Quarto;
import edu.berkeley.gamesman.game.StrictQuarto;

/**
 * A database which wraps a database storing Quarto to be returned by
 * StrictQuarto
 * 
 * @author dnspies
 */
public class QuartoDatabase extends DatabaseWrapper {
	private class QHandle extends DatabaseHandle {
		private final DatabaseHandle innerHandle;
		private final StrictQuarto.QuartoState qs;

		private QHandle(int numBytes, boolean reading) {
			super(numBytes, reading);
			innerHandle = db.getHandle(reading);
			qs = sq.newState();
		}

	}

	private StrictQuarto sq;
	private Quarto q;

	/**
	 * Default database wrapper constructor
	 * 
	 * @param db
	 *            The underlying database
	 * @param conf
	 *            The configuration object
	 * @param firstRecord
	 *            The first record contained in this database (under the Quarto
	 *            hash, not StrictQuarto)
	 * @param numRecords
	 *            The number of records contained in this database (under the
	 *            Quarto hash, not StrictQuarto)
	 * @param reading
	 *            Whether reading from this database
	 * @param writing
	 *            Should be false
	 */
	public QuartoDatabase(Database db, Configuration conf, long firstRecord,
			long numRecords, boolean reading, boolean writing) {
		super(db, conf, firstRecord, numRecords, reading, writing);
		assert !writing;
		sq = (StrictQuarto) conf.getGame();
		q = new Quarto(conf);
	}

	@Override
	protected int readBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int writeBytes(DatabaseHandle dh, long location, byte[] array,
			int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public QHandle getHandle(boolean reading) {
		assert reading;
		return new QHandle(myLogic.recordBytes, reading);
	}

	@Override
	public long readRecord(DatabaseHandle dh, long recordIndex)
			throws IOException {
		QHandle qh = (QHandle) dh;
		sq.hashToState(recordIndex, qh.qs);
		long innerHash = q.getHash(qh.qs);
		return db.readRecord(qh.innerHandle, innerHash);
	}
}
