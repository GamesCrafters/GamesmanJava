package edu.berkeley.gamesman.database;

import edu.berkeley.gamesman.core.Configuration;

public class DartboardCache extends DatabaseWrapper {

	MemoryDatabase[] slots;

	public DartboardCache(Database db, String uri, Configuration conf,
			long[] firstRecords, long[] lastRecords) {
		super(db, uri, conf, true, firstFirst(firstRecords), maxNum(
				firstRecords, lastRecords));
		slots = new MemoryDatabase[firstRecords.length];
		for (int i = 0; i < firstRecords.length; i++) {
			slots[i] = new MemoryDatabase(db, null, conf, false, true);
			slots[i].setRange(firstRecords[i],
					(int) (lastRecords[i] - firstRecords[i]));
		}
	}

	private static long maxNum(long[] firstRecords, long[] lastRecords) {
		long max = Long.MIN_VALUE;
		for (int i = 0; i < firstRecords.length; i++)
			if (lastRecords[i] > max)
				max = lastRecords[i];
		return max + 1 - firstFirst(firstRecords);
	}

	private static long firstFirst(long[] firstRecords) {
		long min = Long.MAX_VALUE;
		for (long l : firstRecords)
			if (l >= 0 && l < min)
				min = l;
		return min;
	}

	@Override
	public void close() {
		for (MemoryDatabase slot : slots)
			slot.close();
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

	private static class DartCacheHandle extends DatabaseHandle {
		protected DartCacheHandle(int recordGroupByteLength) {
			super(recordGroupByteLength);
		}

		int whichDb;
	}

	@Override
	public DatabaseHandle getHandle() {
		DartCacheHandle result = new DartCacheHandle(recordGroupByteLength);
		result.innerHandle = new DatabaseHandle(recordGroupByteLength);
		return result;
	}

	@Override
	protected void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		DartCacheHandle dc = (DartCacheHandle) dh;
		for (int i = 0; i < slots.length; i++) {
			if (slots[i].containsRecord(toFirstRecord(byteIndex) + firstNum)
					&& slots[i].containsRecord(toFirstRecord(byteIndex
							+ numBytes
							- (lastNum > 0 ? recordGroupByteLength : 0))
							+ lastNum - 1)) {
				dc.whichDb = i;
				slots[i].prepareRange(dc.innerHandle, byteIndex, firstNum,
						numBytes, lastNum);
				break;
			}
		}
	}

	@Override
	protected int getBytes(DatabaseHandle dh, byte[] arr, int off, int maxLen,
			boolean overwriteEdgesOk) {
		DartCacheHandle dc = (DartCacheHandle) dh;
		return slots[dc.whichDb].getBytes(dc.innerHandle, arr, off, maxLen,
				overwriteEdgesOk);
	}

	@Override
	public long getSize() {
		long total = 0L;
		for (MemoryDatabase slot : slots)
			total += slot.getSize();
		return total;
	}

}
