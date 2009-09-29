package edu.berkeley.gamesman.database;

import java.util.Arrays;
import java.util.Iterator;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.database.util.Page;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Caches records obtained from another database passed as an argument
 * 
 * @author dnspies
 */
public class DatabaseCache extends Database {

	private Page[][] records; // index,n,offset

	private int indexBits, indices;

	private int offsetBits, pageSize;

	private int nWayAssociative;

	private long[][] tags, used;

	private long[] current;

	private boolean[][] dirty;

	private final Database db;

	/**
	 * @param db
	 *            The inner database to be used.
	 */
	public DatabaseCache(Database db) {
		this.db = db;
	}

	@Override
	public Record getRecord(long recordIndex) {
		long tag = recordIndex;
		int recordNum = (int) (tag % conf.recordsPerGroup);
		tag /= conf.recordsPerGroup;
		int offset = (int) (tag & (pageSize - 1));
		tag >>= offsetBits;
		int index = (int) (tag & (indices - 1));
		tag >>= indexBits;
		int i;
		long lowest = Long.MAX_VALUE;
		int lowUsed = 0;
		for (i = 0; i < nWayAssociative; i++) {
			if (used[index][i] == 0) {
				loadPage(tag, index, i);
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
			loadPage(tag, index, i);
		}
		Record rec = conf.getGame().newRecord();
		boolean failedCheck;
		synchronized (records[index][i]) {
			if (tags[index][i] == tag) {
				records[index][i].get(offset, recordNum, rec);
				used[index][i] = ++current[index];
				failedCheck = false;
			} else
				failedCheck = true;
		}
		if (failedCheck)
			return getRecord(recordIndex);
		else
			return rec;
	}

	@Override
	public void getRecord(long recordIndex, Record r) {
		long tag = recordIndex;
		int recordNum = (int) (tag % conf.recordsPerGroup);
		tag /= conf.recordsPerGroup;
		int offset = (int) (tag & (pageSize - 1));
		tag >>= offsetBits;
		int index = (int) (tag & (indices - 1));
		tag >>= indexBits;
		int i;
		long lowest = Long.MAX_VALUE;
		int lowUsed = 0;
		for (i = 0; i < nWayAssociative; i++) {
			if (used[index][i] == 0) {
				loadPage(tag, index, i);
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
			loadPage(tag, index, i);
		}
		boolean failedCheck;
		synchronized (records[index][i]) {
			if (tags[index][i] == tag) {
				records[index][i].get(offset, recordNum, r);
				used[index][i] = ++current[index];
				failedCheck = false;
			} else
				failedCheck = true;
		}
		if (failedCheck)
			getRecord(recordIndex, r);
	}

	@Override
	public void putRecord(long recordIndex, Record r) {
		long tag = recordIndex;
		int recordNum = (int) (tag % conf.recordsPerGroup);
		tag /= conf.recordsPerGroup;
		int offset = (int) (tag & (pageSize - 1));
		tag >>= offsetBits;
		int index = (int) (tag & (indices - 1));
		tag >>= indexBits;
		int i;
		long lowest = Long.MAX_VALUE;
		int lowUsed = 0;
		for (i = 0; i < nWayAssociative; i++) {
			if (used[index][i] == 0) {
				loadPage(tag, index, i);
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
			loadPage(tag, index, i);
		}
		boolean failedCheck;
		synchronized (records[index][i]) {
			if (tags[index][i] == tag) {
				records[index][i].set(offset, recordNum, r);
				used[index][i] = ++current[index];
				dirty[index][i] = true;
				failedCheck = false;
			} else
				failedCheck = true;
		}
		if (failedCheck)
			putRecord(recordIndex, r);
	}

	private void loadPage(long tag, int index, int i) {
		synchronized (records[index][i]) {
			if (dirty[index][i]) {
				writeBack(tags[index][i], index, i);
			}
			assert Util.debug(DebugFacility.DATABASE, "Loading "
					+ ((tag << indexBits) | index));
			tags[index][i] = tag;
			dirty[index][i] = false;
			long firstGroup = ((tag << indexBits) | index) << offsetBits;
			synchronized (db) {
				Iterator<RecordGroup> it = db.getRecordGroups(firstGroup
						* conf.recordGroupByteLength, pageSize);
				for (int off = 0; off < pageSize; off++)
					records[index][i].setGroup(off, it.next());
			}
		}
	}

	private void writeBack(long tag, int index, int i) {
		synchronized (records[index][i]) {
			assert Util.debug(DebugFacility.DATABASE, "Writing "
					+ ((tag << indexBits) | index));
			long firstRecordGroup = ((tag << indexBits) | index) << offsetBits;
			synchronized (db) {
				db.putRecordGroups(firstRecordGroup
						* conf.recordGroupByteLength, records[index][i]
						.iterator(), pageSize);
			}
			dirty[index][i] = false;
		}
	}

	@Override
	public void close() {
		flush();
		db.close();
	}

	@Override
	public void flush() {
		for (int index = 0; index < indices; index++)
			for (int i = 0; i < nWayAssociative; i++)
				if (dirty[index][i])
					synchronized (records[index][i]) {
						writeBack(tags[index][i], index, i);
					}
		synchronized (db) {
			db.flush();
		}
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
		int groupHolderSize = Page.groupHolderByteSize(conf);
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
		records = new Page[indices][nWayAssociative];
		tags = new long[indices][nWayAssociative];
		dirty = new boolean[indices][nWayAssociative];
		used = new long[indices][nWayAssociative];
		current = new long[indices];
		Arrays.fill(current, 0);
		for (long[] u : used)
			Arrays.fill(u, 0);
		for (boolean[] d : dirty)
			Arrays.fill(d, false);
		for (Page[] pages : records) {
			for (int i = 0; i < pages.length; i++) {
				pages[i] = new Page(conf, pageSize);
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
		Util.debug(DebugFacility.DATABASE, "Pages contain " + pageSize
				+ " record groups");
		Util.debug(DebugFacility.DATABASE, "There are " + indices
				+ " indices each " + nWayAssociative + "-way associative");
		Util
				.debug(DebugFacility.DATABASE, nWayAssociative * indices
						+ " pages");
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup rg) {
		throw new UnsupportedOperationException(
				"DatabaseCache does not operate on the group level");
	}
}
