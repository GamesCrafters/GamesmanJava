package edu.berkeley.gamesman.database;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;

/**
 * Caches records obtained from another database passed as an argument
 * 
 * @author dnspies
 */
public class DatabaseCache extends Database {
	private final Record[][][] records; // index,n,offset

	private final Random pageChooser = new Random();

	private final int indexBits, indices;

	private final int offsetBits, pageSize;

	private final int nWayAssociativeBits = 2,
			nWayAssociative = 1 << nWayAssociativeBits;

	private final long[][] tags;

	private final boolean[][] dirty, used;

	private long tag;

	private int index, offset;

	private final Database db;

	/**
	 * @param db
	 *            The inner database to be used.
	 * @param recordsHeld
	 *            The number of records this database can hold (this will be
	 *            rounded up to the nearest power of 2)
	 */
	public DatabaseCache(Database db, int recordsHeld) {
		int numRecordsBits = (int) Math.ceil(Math.log(recordsHeld)
				/ Math.log(2));
		offsetBits = numRecordsBits / 2;
		pageSize = 1 << offsetBits;
		indexBits = numRecordsBits - (offsetBits + nWayAssociativeBits);
		indices = 1 << indexBits;
		records = new Record[indices][nWayAssociative][pageSize];
		tags = new long[indices][nWayAssociative];
		dirty = new boolean[indices][nWayAssociative];
		used = new boolean[indices][nWayAssociative];
		for (boolean[] u : used)
			Arrays.fill(u, false);
		this.db = db;
		conf = db.getConfiguration();
	}

	/**
	 * Instantiates a database cache using the default size of 1 MebiRecord.
	 * 
	 * @param db
	 *            The inner database
	 */
	public DatabaseCache(Database db) {
		this(db, 1 << 20);
	}

	private void setPoint(long recordIndex) {
		offset = (int) (recordIndex & (pageSize - 1));
		recordIndex >>= offsetBits;
		index = (int) (recordIndex & (indices - 1));
		recordIndex >>= indexBits;
		tag = recordIndex;
	}

	@Override
	public Record getRecord(long recordIndex) {
		setPoint(recordIndex);
		int i;
		for (i = 0; i < nWayAssociative; i++) {
			if (!used[index][i]) {
				loadPage(i);
				break;
			} else if (tags[index][i] == tag)
				break;
		}
		if (i >= nWayAssociative) {
			i = pageChooser.nextInt(nWayAssociative);
			loadPage(i);
		}
		return records[index][i][offset].clone();
	}

	@Override
	public void putRecord(long recordIndex, Record r) {
		setPoint(recordIndex);
		int i;
		for (i = 0; i < nWayAssociative; i++) {
			if (!used[index][i]) {
				loadPage(i);
				break;
			} else if (tags[index][i] == tag)
				break;
		}
		if (i >= nWayAssociative) {
			i = pageChooser.nextInt(nWayAssociative);
			loadPage(i);
		}
		records[index][i][offset] = r.clone();
		dirty[index][i] = true;
	}

	private void loadPage(int i) {
		if (used[index][i] && dirty[index][i])
			writeBack(i);
		tags[index][i] = tag;
		dirty[index][i] = false;
		used[index][i] = true;
		long firstRecord = ((tag << indexBits) | index) << offsetBits;
		Iterator<Record> it = db.getRecords(firstRecord, pageSize);
		for (int off = 0; off < pageSize; off++)
			records[index][i][off] = it.next();
	}

	private void writeBack(int i) {
		long thisTag = tags[index][i];
		long firstRecord = ((thisTag << indexBits) | index) << offsetBits;
		db.putRecords(firstRecord, records[index][i], 0, pageSize);
		dirty[index][i] = false;
	}

	@Override
	public void close() {
		flush();
		db.close();
	}

	@Override
	public void flush() {
		for (index = 0; index < indices; index++)
			for (int i = 0; i < nWayAssociative; i++)
				if (used[index][i] && dirty[index][i])
					writeBack(i);
		db.flush();
	}

	@Override
	public RecordGroup getRecordGroup(long loc) {
		throw new UnsupportedOperationException(
				"DatabaseCache does not operate on the group level");
	}

	@Override
	public void initialize(String uri) {
		if (conf != null)
			db.initialize(uri, conf);
		else {
			db.initialize(uri);
			conf = db.getConfiguration();
		}
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup rg) {
		throw new UnsupportedOperationException(
				"DatabaseCache does not operate on the group level");
	}
}
