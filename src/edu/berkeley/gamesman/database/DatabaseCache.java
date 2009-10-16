package edu.berkeley.gamesman.database;

import java.util.Arrays;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.util.Page;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * Caches records obtained from another database passed as an argument
 * 
 * @author dnspies
 */
public final class DatabaseCache extends Database {

	private Page[][] records;

	private int indexBits, indices;

	private int offsetBits, pageSize;

	private int nWayAssociative;

	private long[][] tags, used;

	private long[] current;

	private long maxGroups;

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
				failedCheck = false;
			} else
				failedCheck = true;
		}
		if (failedCheck)
			putRecord(recordIndex, r);
	}

	private void loadPage(long tag, int index, int i) {
		synchronized (records[index][i]) {
			if (records[index][i].isDirty()) {
				writeBack(index, i);
			}
			tags[index][i] = tag;
			long pageNum = (tag << indexBits) | index;
			assert Util.debug(DebugFacility.DATABASE, "Loading " + pageNum);
			long firstGroup = pageNum << offsetBits;
			if (firstGroup + pageSize > maxGroups)
				records[index][i].loadPage(db, firstGroup,
						(int) (maxGroups - firstGroup));
			else
				records[index][i].loadPage(db, firstGroup, pageSize);
		}
	}

	private void writeBack(int index, int i) {
		assert Util.debug(DebugFacility.DATABASE, "Writing "
				+ ((tags[index][i] << indexBits) | index));
		records[index][i].writeBack(db);
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
				if (records[index][i].isDirty())
					writeBack(index, i);
		db.flush();
	}

	@Override
	public void initialize(String uri) {
		if (conf != null)
			db.initialize(uri, conf);
		else {
			db.initialize(uri);
			conf = db.getConfiguration();
		}
		int totalBytes = conf.getInteger("gamesman.db.cacheSize", 67108864);
		int pageBytes = conf.getInteger("gamesman.db.pageSize", 16384);
		pageSize = pageBytes / conf.recordGroupByteLength;
		offsetBits = (int) (Math.log(pageSize) / Math.log(2));
		pageSize = 1 << offsetBits;
		nWayAssociative = conf.getInteger("gamesman.db.nWayAssociative", 4);
		pageBytes = pageSize * conf.recordGroupByteLength;
		indices = totalBytes / (pageBytes * nWayAssociative);
		indexBits = (int) (Math.log(indices) / Math.log(2));
		indices = 1 << indexBits;
		records = new Page[indices][nWayAssociative];
		tags = new long[indices][nWayAssociative];
		used = new long[indices][nWayAssociative];
		current = new long[indices];
		Arrays.fill(current, 0);
		for (long[] u : used)
			Arrays.fill(u, 0);
		for (Page[] pages : records) {
			for (int i = 0; i < pages.length; i++) {
				pages[i] = new Page(conf);
			}
		}
		System.out.println("Using " + indices * nWayAssociative * pageBytes
				+ " bytes for cache");
		assert Util.debug(DebugFacility.DATABASE, "Pages contain " + pageSize
				+ " record groups");
		assert Util.debug(DebugFacility.DATABASE, "There are " + indices
				+ " indices each " + nWayAssociative + "-way associative");
		assert Util.debug(DebugFacility.DATABASE, nWayAssociative * indices
				+ " pages");
		maxGroups = (conf.getHasher().numHashes() + conf.recordsPerGroup - 1)
				/ conf.recordsPerGroup;
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void seek(long loc) {
		throw new UnsupportedOperationException();
	}
}
