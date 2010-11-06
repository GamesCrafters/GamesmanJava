package edu.berkeley.gamesman.database;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import edu.berkeley.gamesman.core.Configuration;

public class RangeCache extends DatabaseWrapper {

	private final MemoryDatabase[] slots;
	private final long[] searchable;

	public RangeCache(Database db, String uri, Configuration conf,
			long[] firstRecords, long[] lastRecords) {
		super(db, uri, conf, true, firstFirst(firstRecords), maxNum(
				firstRecords, lastRecords));
		LinkedList<long[]> ranges = new LinkedList<long[]>();
		for (int i = 0; i < firstRecords.length; i++) {
			if (firstRecords[i] < 0L)
				continue;
			Iterator<long[]> iter = ranges.iterator();
			while (iter.hasNext()) {
				long[] testRange = iter.next();
				if (firstRecords[i] <= testRange[1]
						&& lastRecords[i] >= testRange[0]) {
					firstRecords[i] = Math.min(firstRecords[i], testRange[0]);
					lastRecords[i] = Math.max(lastRecords[i], testRange[1]);
					iter.remove();
				}
			}
			ranges.add(new long[] { firstRecords[i], lastRecords[i] });
		}
		Collections.sort(ranges, new Comparator<long[]>() {

			@Override
			public int compare(long[] o1, long[] o2) {
				if (o1[0] > o2[0])
					return 1;
				else if (o1[0] < o2[0])
					return -1;
				else
					return 0;
			}

		});
		slots = new MemoryDatabase[ranges.size()];
		searchable = new long[ranges.size()];
		Iterator<long[]> iter = ranges.iterator();
		for (int i = 0; i < slots.length; i++) {
			long[] next = iter.next();
			slots[i] = new MemoryDatabase(db, null, conf, false, next[0],
					next[1] - next[0] + 1);
			searchable[i] = next[0];
		}
	}

	private static long maxNum(long[] firstRecords, long[] lastRecords) {
		long max = -1L;
		for (int i = 0; i < firstRecords.length; i++)
			if (lastRecords[i] > max)
				max = lastRecords[i];
		if (max < 0)
			return 0L;
		return max + 1 - firstFirst(firstRecords);
	}

	private static long firstFirst(long[] firstRecords) {
		long min = Long.MAX_VALUE;
		for (long l : firstRecords)
			if (l >= 0 && l < min)
				min = l;
		if (min == Long.MAX_VALUE)
			return 0L;
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

	private class DartCacheHandle extends DatabaseHandle {
		DartCacheHandle(int recordGroupByteLength) {
			super(recordGroupByteLength);
			for (int i = 0; i < slots.length; i++) {
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
		super.prepareRange(dh, byteIndex, firstNum, numBytes, lastNum);
		DartCacheHandle dc = (DartCacheHandle) dh;
		long firstRecord = toFirstRecord(byteIndex) + firstNum;
		dc.whichDb = Arrays.binarySearch(searchable, firstRecord);
		if (dc.whichDb < 0)
			dc.whichDb = -dc.whichDb - 2;
		slots[dc.whichDb].prepareRange(dc.handles[dc.whichDb], byteIndex,
				firstNum, numBytes, lastNum);
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
		for (MemoryDatabase slot : slots)
			total += slot.getSize();
		return total;
	}

}
