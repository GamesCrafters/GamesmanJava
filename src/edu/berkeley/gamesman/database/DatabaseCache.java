package edu.berkeley.gamesman.database;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;

/**
 * Caches records obtained from another database passed as an argument
 * 
 * @author dnspies
 */
public final class DatabaseCache extends Database {
	private static final class GroupHolder {
		private RecordGroup rg;

		private Record[] r;

		private boolean called;

		private boolean changed;

		public GroupHolder(Configuration conf) {
			rg = new RecordGroup(conf);
			r = new Record[conf.recordsPerGroup];
			for (int i = 0; i < conf.recordsPerGroup; i++)
				r[i] = new Record(conf);
		}

		public void get(int recordNum, Record record) {
			if (!called) {
				called = true;
				rg.getRecords(r, 0);
			}
			record.set(r[recordNum]);
		}

		public void set(int recordNum, Record record) {
			if (!called) {
				called = true;
				rg.getRecords(r, 0);
			}
			changed = true;
			r[recordNum].set(record);
		}

		public void setGroup(RecordGroup group) {
			rg.set(group);
			called = false;
			changed = false;
		}

		public RecordGroup getGroup() {
			if (changed)
				rg.set(r);
			return rg;
		}
	}

	private final GroupHolder[][][] records; // index,n,offset

	private final Random pageChooser = new Random();

	private final int indexBits, indices;

	private final int offsetBits, pageSize;

	private final int nWayAssociativeBits = 2,
			nWayAssociative = 1 << nWayAssociativeBits;

	private final long[][] tags;

	private final boolean[][] dirty, used;

	private long tag;

	private int index, offset, recordNum;

	private final Database db;

	private final CachedbGroupIterator groupIterator;

	private static final class CachedbGroupIterator implements
			Iterator<RecordGroup> {

		private int i;

		private GroupHolder[] groups;

		CachedbGroupIterator() {
		}

		private void reset(GroupHolder[] groups) {
			this.groups = groups;
			i = 0;
		}

		public boolean hasNext() {
			return i < groups.length;
		}

		public RecordGroup next() {
			return groups[i++].getGroup();
		}

		public void remove() {
			throw new UnsupportedOperationException("remove not supported here");
		}
	};

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
		groupIterator = new CachedbGroupIterator();
		indexBits = numRecordsBits - (offsetBits + nWayAssociativeBits);
		indices = 1 << indexBits;
		records = new GroupHolder[indices][nWayAssociative][pageSize];
		tags = new long[indices][nWayAssociative];
		dirty = new boolean[indices][nWayAssociative];
		used = new boolean[indices][nWayAssociative];
		for (boolean[] u : used)
			Arrays.fill(u, false);
		this.db = db;
	}

	/**
	 * Instantiates a database cache using the default size of 1 MebiRecord.
	 * 
	 * @param db
	 *            The inner database
	 */
	public DatabaseCache(Database db) {
		this(db, 1 << 14);
	}

	private void setPoint(long recordIndex) {
		recordNum = (int) (recordIndex % conf.recordsPerGroup);
		recordIndex /= conf.recordsPerGroup;
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
		Record rec = new Record(conf);
		records[index][i][offset].get(recordNum, rec);
		return rec;
	}

	@Override
	public void getRecord(long recordIndex, Record r) {
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
		records[index][i][offset].get(recordNum, r);
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
		records[index][i][offset].set(recordNum, r);
		dirty[index][i] = true;
	}

	private void loadPage(int i) {
		if (used[index][i] && dirty[index][i])
			writeBack(i);
		tags[index][i] = tag;
		dirty[index][i] = false;
		used[index][i] = true;
		long firstGroup = ((tag << indexBits) | index) << offsetBits;
		Iterator<RecordGroup> it = db.getRecordGroups(firstGroup
				* conf.recordGroupByteLength, pageSize);
		for (int off = 0; off < pageSize; off++)
			records[index][i][off].setGroup(it.next());
	}

	private void writeBack(int i) {
		long thisTag = tags[index][i];
		long firstRecordGroup = ((thisTag << indexBits) | index) << offsetBits;
		groupIterator.reset(records[index][i]);
		db.putRecordGroups(firstRecordGroup * conf.recordGroupByteLength,
				groupIterator, pageSize);
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
		for (GroupHolder[][] pages : records) {
			for (GroupHolder[] page : pages) {
				for (int i = 0; i < page.length; i++)
					page[i] = new GroupHolder(conf);
			}
		}
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup rg) {
		throw new UnsupportedOperationException(
				"DatabaseCache does not operate on the group level");
	}
}
