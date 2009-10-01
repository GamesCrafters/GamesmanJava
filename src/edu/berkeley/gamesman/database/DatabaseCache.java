package edu.berkeley.gamesman.database;

import java.util.Arrays;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.util.LocalizedPage;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * Caches records obtained from another database passed as an argument
 * 
 * @author dnspies
 */
public class DatabaseCache extends Database {

	private LocalizedPage[][] records;

	private int indexBits, indices;

	private int offsetBits, pageSize;

	private int nWayAssociative;

	private long[][] tags, used;

	private long[] current;

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
			assert Util.debug(DebugFacility.DATABASE, "Loading "
					+ ((tag << indexBits) | index));
			records[index][i].loadPage(db,
					((tag << indexBits) | index) << offsetBits);
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
		synchronized (db) {
			db.flush();
		}
	}

	@Override
	public long getLongRecordGroup(long loc) {
		throw new UnsupportedOperationException(
				"DatabaseCache does not operate on the group level");
	}

	@Override
	public BigInteger getBigIntRecordGroup(long loc) {
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
		int totalBytes = conf.getInteger("gamesman.db.cacheSize", 67108864);
		int pageBytes = conf.getInteger("gamesman.db.pageSize", 16384);
		pageSize = LocalizedPage.numGroups(conf, pageBytes);
		offsetBits = (int) (Math.log(pageSize) / Math.log(2));
		pageSize = 1 << offsetBits;
		nWayAssociative = conf.getInteger("gamesman.db.nWayAssociative", 4);
		pageBytes = LocalizedPage.byteSize(conf, pageSize);
		indices = totalBytes / (pageBytes * nWayAssociative);
		indexBits = (int) (Math.log(indices) / Math.log(2));
		indices = 1 << indexBits;
		records = new LocalizedPage[indices][nWayAssociative];
		tags = new long[indices][nWayAssociative];
		used = new long[indices][nWayAssociative];
		current = new long[indices];
		Arrays.fill(current, 0);
		for (long[] u : used)
			Arrays.fill(u, 0);
		for (LocalizedPage[] pages : records) {
			for (int i = 0; i < pages.length; i++) {
				pages[i] = new LocalizedPage(conf, pageSize, nWayAssociative);
			}
		}
		int bytesUsed = 48; // Size of this class
		bytesUsed += 3 * (12 + indices * 4 + 7) / 8 * 8;
		bytesUsed += indices * (12 + nWayAssociative * 4 + 7) / 8 * 8;
		bytesUsed += indices * nWayAssociative * LocalizedPage.byteSize(conf, pageSize);
		bytesUsed += 2 * indices * (12 + nWayAssociative * 8 + 7) / 8 * 8;
		bytesUsed += (12 + indices * 8 + 7) / 8 * 8;
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
	public void putRecordGroup(long loc, long rg) {
		throw new UnsupportedOperationException(
				"DatabaseCache does not operate on the group level");
	}

	@Override
	public void putRecordGroup(long loc, BigInteger rg) {
		throw new UnsupportedOperationException(
				"DatabaseCache does not operate on the group level");
	}
}
