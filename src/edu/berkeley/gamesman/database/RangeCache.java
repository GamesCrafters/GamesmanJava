package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;

/**
 * A cache which simply contains separate ranges of records
 * 
 * @author dnspies
 */
public class RangeCache extends DatabaseWrapper {

	private final Pool<MemoryDatabase> slotPool = new Pool<MemoryDatabase>(
			new Factory<MemoryDatabase>() {

				public MemoryDatabase newObject() {
					MemoryDatabase md = new MemoryDatabase(db, null, conf,
							false, true);
					reset(md);
					return md;
				}

				public void reset(MemoryDatabase t) {
					t.ensureByteSize(memPerChild);
				}
			});
	private MemoryDatabase[] slots;
	private long numHashes;
	private final int memPerChild;
	private int tempChildNum = -1;

	/**
	 * @param db
	 *            The underlying database
	 * @param conf
	 *            The configuration object
	 * @param memPerChild
	 *            The maximum amount of memory each range is expected to take up
	 */
	public RangeCache(Database db, Configuration conf, int memPerChild) {
		super(db, null, conf, false, -1, 0);
		this.memPerChild = memPerChild;
	}

	/**
	 * @param db
	 *            A database for memory calculations
	 * @param firstRecords
	 *            The start points of each range
	 * @param lastRecords
	 *            The end points of each range
	 * @return The amount of memory required for a range cache with the provided
	 *         ranges
	 */
	public static long memFor(Database db, long[] firstRecords,
			long[] lastRecords) {
		long total = 0L;
		for (int i = 0; i < firstRecords.length; i++) {
			if (firstRecords[i] >= 0 && lastRecords[i] >= 0)
				total += db.requiredMem(firstRecords[i], lastRecords[i] + 1
						- firstRecords[i]);
		}
		return total;
	}

	@Override
	public void close() {
		for (MemoryDatabase slot : slots) {
			if (slot != null)
				slot.close();
		}
	}

	@Override
	protected void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		throw new Error("Cache is read only");
	}

	@Override
	protected void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		getRecordsAsBytes(dh, loc, 0, arr, off, len, 0, true);
	}

	private class DartCacheHandle extends DatabaseHandle {
		DartCacheHandle(int recordGroupByteLength) {
			super(recordGroupByteLength);
			for (int i = 0; i < slots.length; i++) {
				if (slots[i] == null)
					handles[i] = null;
				else
					handles[i] = slots[i].getHandle();
			}
		}

		int whichDb;
		DatabaseHandle[] handles = new DatabaseHandle[slots.length];
	}

	@Override
	public DatabaseHandle getHandle() {
		return new DartCacheHandle(recordGroupByteLength);
	}

	@Override
	protected void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		int childNum = tempChildNum;
		long firstRecord = toFirstRecord(byteIndex) + firstNum;
		long lastRecord = toFirstRecord(byteIndex + numBytes - 1)
				+ (lastNum == 0 ? recordsPerGroup : lastNum);
		if (childNum >= 0) {
			if (slots[childNum] == null) {
				throw new Error(childNum + " wasn't cached but " + firstRecord
						+ " - " + lastRecord + " is needed");
			}
			long firstSlotRecord = slots[childNum].firstRecord();
			long lastSlotRecord = firstSlotRecord
					+ slots[childNum].numRecords();
			if (!(firstRecord >= firstSlotRecord && lastRecord <= lastSlotRecord))
				throw new RuntimeException(childNum + " doesn't fit\n"
						+ firstRecord + " - " + lastRecord + " in "
						+ firstSlotRecord + " - " + lastSlotRecord);
		} else {
			for (int i = 0; i < slots.length; i++) {
				if (slots[i] == null)
					continue;
				long firstSlotRecord = slots[i].firstRecord();
				long lastSlotRecord = firstSlotRecord + slots[i].numRecords();
				if (firstRecord >= firstSlotRecord
						&& lastRecord <= lastSlotRecord) {
					childNum = i;
					break;
				}
			}
		}
		prepareRange(dh, byteIndex, firstNum, numBytes, lastNum, childNum);
	}

	protected void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum, int childNum) {
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		DartCacheHandle dc = (DartCacheHandle) dh;
		dc.whichDb = childNum;
		slots[childNum].prepareRange(dc.handles[childNum], byteIndex, firstNum,
				numBytes, lastNum);
	}

	@Override
	protected int getBytes(DatabaseHandle dh, byte[] arr, int off, int maxLen,
			boolean overwriteEdgesOk) {
		DartCacheHandle dc = (DartCacheHandle) dh;
		return slots[dc.whichDb].getBytes(dc.handles[dc.whichDb], arr, off,
				maxLen, overwriteEdgesOk);
	}

	@Override
	public long getSize() {
		long total = 0L;
		for (MemoryDatabase slot : slots) {
			if (slot != null)
				total += slot.getSize();
		}
		return total;
	}

	public long numHashes() {
		return numHashes;
	}

	/**
	 * @param firstRecords
	 *            The start points of each range
	 * @param lastRecords
	 *            The end points of each range
	 * @param numHashes
	 *            The number of hashes being solved which require this cache
	 */
	public void setRanges(long[] firstRecords, long[] lastRecords,
			long numHashes) {
		if (slots != null)
			for (MemoryDatabase slot : slots) {
				if (slot != null)
					slotPool.release(slot);
			}
		this.numHashes = numHashes;
		// Collections.sort(ranges, new Comparator<long[]>() {
		//
		// @Override
		// public int compare(long[] o1, long[] o2) {
		// if (o1[0] > o2[0])
		// return 1;
		// else if (o1[0] < o2[0])
		// return -1;
		// else if (o1[1] > o2[1])
		// return 1;
		// else if (o1[1] < o2[1])
		// return -1;
		// else
		// return 0;
		// }
		//
		// });
		// ListIterator<long[]> iter = ranges.listIterator();
		// long[] prev = null;
		// if (iter.hasNext())
		// prev = iter.next();
		// while (iter.hasNext()) {
		// long[] next = iter.next();
		// if (prev[1] >= next[1])
		// iter.remove();
		// else if (prev[0] == next[0]) {
		// iter.previous();
		// iter.previous();
		// iter.remove();
		// iter.next();
		// prev = next;
		// } else
		// prev = next;
		// }
		slots = new MemoryDatabase[firstRecords.length];
		for (int i = 0; i < slots.length; i++) {
			long firstRecord = firstRecords[i];
			long lastRecord = lastRecords[i];
			if (firstRecord < 0 || lastRecord < 0)
				slots[i] = null;
			else {
				slots[i] = slotPool.get();
				slots[i].setRange(firstRecord,
						(int) (lastRecord - firstRecord + 1));
			}
		}
	}

	public long getRecord(DatabaseHandle dh, long recordIndex, int childNum) {
		if (tempChildNum >= 0)
			throw new RuntimeException("In the middle of reading");
		tempChildNum = childNum;
		long result = super.getRecord(dh, recordIndex);
		tempChildNum = -1;
		return result;
	}
}
