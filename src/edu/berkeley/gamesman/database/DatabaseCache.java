package edu.berkeley.gamesman.database;

import java.util.Arrays;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Caches records obtained from another database passed as an argument
 * 
 * @author dnspies
 */
public class DatabaseCache extends Database {
	private static final class GroupHolder {
		private RecordGroup rg;

		private Record[] r;

		private boolean called;

		private boolean changed;

		public GroupHolder(Configuration conf) {
			rg = new RecordGroup(conf);
			r = new Record[conf.recordsPerGroup];
			for (int i = 0; i < conf.recordsPerGroup; i++)
				r[i] = conf.getGame().newRecord();
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
				rg.set(r, 0);
			return rg;
		}

		private static int byteSize(Configuration conf) {
			return 40 + RecordGroup.byteSize(conf) + (conf.recordsPerGroup + 1)
					/ 2 * 8 + conf.recordsPerGroup * Record.byteSize(conf);
		}
	}

	private GroupHolder[][][] records; // index,n,offset

	private int indexBits, indices;

	private int offsetBits, pageSize;

	private int nWayAssociative;

	private long[][] tags, used;

	private long[] current;

	private boolean[][] dirty;

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
	 */
	public DatabaseCache(Database db) {
		groupIterator = new CachedbGroupIterator();
		this.db = db;
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
	public synchronized Record getRecord(long recordIndex) {
		setPoint(recordIndex);
		int i;
		long lowest = Long.MAX_VALUE;
		int lowUsed = 0;
		for (i = 0; i < nWayAssociative; i++) {
			if (used[index][i] == 0) {
				loadPage(i);
				break;
			} else if (tags[index][i] == tag) {
				break;
			} else if (used[index][i] < lowest) {
				lowest = used[index][i];
				lowUsed = i;
			}
		}
		if (i >= nWayAssociative) {
			i = lowUsed;
			loadPage(i);
		}
		Record rec = conf.getGame().newRecord();
		records[index][i][offset].get(recordNum, rec);
		used[index][i] = ++current[index];
		return rec;
	}

	@Override
	public synchronized void getRecord(long recordIndex, Record r) {
		setPoint(recordIndex);
		int i;
		long lowest = Long.MAX_VALUE;
		int lowUsed = 0;
		for (i = 0; i < nWayAssociative; i++) {
			if (used[index][i] == 0) {
				loadPage(i);
				break;
			} else if (tags[index][i] == tag) {
				break;
			} else if (used[index][i] < lowest) {
				lowest = used[index][i];
				lowUsed = i;
			}
		}
		if (i >= nWayAssociative) {
			i = lowUsed;
			loadPage(i);
		}
		records[index][i][offset].get(recordNum, r);
		used[index][i] = ++current[index];
	}

	@Override
	public synchronized void putRecord(long recordIndex, Record r) {
		setPoint(recordIndex);
		int i;
		long lowest = Long.MAX_VALUE;
		int lowUsed = 0;
		for (i = 0; i < nWayAssociative; i++) {
			if (used[index][i] == 0) {
				loadPage(i);
				break;
			} else if (tags[index][i] == tag) {
				break;
			} else if (used[index][i] < lowest) {
				lowest = used[index][i];
				lowUsed = i;
			}
		}
		if (i >= nWayAssociative) {
			i = lowUsed;
			loadPage(i);
		}
		records[index][i][offset].set(recordNum, r);
		used[index][i] = ++current[index];
		dirty[index][i] = true;
	}

	private synchronized void loadPage(int i) {
		if (dirty[index][i])
			writeBack(i);
		assert Util.debug(DebugFacility.DATABASE, "Loading "
				+ ((tag << indexBits) | index));
		tags[index][i] = tag;
		dirty[index][i] = false;
		long firstGroup = ((tag << indexBits) | index) << offsetBits;
		Iterator<RecordGroup> it = db.getRecordGroups(firstGroup
				* conf.recordGroupByteLength, pageSize);
		for (int off = 0; off < pageSize; off++)
			records[index][i][off].setGroup(it.next());
	}

	private synchronized void writeBack(int i) {
		long thisTag = tags[index][i];
		assert Util.debug(DebugFacility.DATABASE, "Writing "
				+ ((thisTag << indexBits) | index));
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
	public synchronized void flush() {
		for (index = 0; index < indices; index++)
			for (int i = 0; i < nWayAssociative; i++)
				if (dirty[index][i])
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
		int totalBytes = Integer.parseInt(conf.getProperty(
				"gamesman.db.cacheSize", "67108864"));
		int groupHolderSize = GroupHolder.byteSize(conf);
		int pageBytes = Integer.parseInt(conf.getProperty(
				"gamesman.db.pageSize", "16384"));
		pageSize = pageBytes / (groupHolderSize + 4);
		offsetBits = (int) (Math.log(pageSize) / Math.log(2));
		pageSize = Math.max(1 << offsetBits, 1);
		nWayAssociative = Integer.parseInt(conf.getProperty(
				"gamesman.db.nWayAssociative", "4"));
		indices = (totalBytes - 160)
				/ (88 + 37 * nWayAssociative + (4 + groupHolderSize)
						* nWayAssociative * pageSize);
		indexBits = (int) (Math.log(indices) / Math.log(2));
		indices = Math.max(1 << indexBits, 1);
		records = new GroupHolder[indices][nWayAssociative][pageSize];
		tags = new long[indices][nWayAssociative];
		dirty = new boolean[indices][nWayAssociative];
		used = new long[indices][nWayAssociative];
		current = new long[indices];
		Arrays.fill(current, 0);
		for (long[] u : used)
			Arrays.fill(u, 0);
		for (boolean[] d : dirty)
			Arrays.fill(d, false);
		for (GroupHolder[][] pages : records) {
			for (GroupHolder[] page : pages) {
				for (int i = 0; i < page.length; i++)
					page[i] = new GroupHolder(conf);
			}
		}
		int bytesUsed = 80; // Size of this class
		bytesUsed += 4 * (16 + (indices + 1) / 2 * 8);
		// Top level records, tags, used, dirty
		bytesUsed += indices * (16 + (nWayAssociative + 1) / 2 * 8);
		// Second level records
		bytesUsed += indices * nWayAssociative * (16 + (pageSize + 1) / 2 * 8);
		// Third level records
		bytesUsed += indices * nWayAssociative * pageSize * groupHolderSize;
		// The records themselves
		bytesUsed += 2 * indices * (16 + nWayAssociative * 8);
		// Second level tags, used
		bytesUsed += indices * (16 + (nWayAssociative + 7) / 8 * 8);
		// Second level dirty
		bytesUsed += 16 + indices * 8;
		// Top level current
		System.out.println("Using " + bytesUsed + " bytes for cache");
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup rg) {
		throw new UnsupportedOperationException(
				"DatabaseCache does not operate on the group level");
	}
}
