package edu.berkeley.gamesman.database.util;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

public class LocalizedPage {
	private long[] longGroups;

	private BigInteger[] bigIntGroups;

	public int numGroups;

	private final Record[][] rawRecords;

	private long[] used;

	private long lastUsed = 0;

	private boolean[] rawChanged;

	private int[] lastIndex;

	public long firstGroup;

	private boolean dirty = false;

	private final Configuration conf;

	private final class LongPageGroupIterator implements LongIterator {

		private int i = 0;

		public boolean hasNext() {
			return i < longGroups.length;
		}

		public long next() {
			return longGroups[i++];
		}
	}

	private final class BigIntPageGroupIterator implements Iterator<BigInteger> {

		private int i = 0;

		public boolean hasNext() {
			return i < bigIntGroups.length;
		}

		public BigInteger next() {
			return bigIntGroups[i++];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public LocalizedPage(Configuration conf, int groupsRemembered) {
		this.conf = conf;
		numGroups = 0;
		rawRecords = new Record[groupsRemembered][conf.recordsPerGroup];
		used = new long[groupsRemembered];
		lastIndex = new int[groupsRemembered];
		rawChanged = new boolean[groupsRemembered];
		for (int n = 0; n < groupsRemembered; n++) {
			for (int i = 0; i < conf.recordsPerGroup; i++)
				rawRecords[n][i] = conf.getGame().newRecord();
			used[n] = 0L;
			lastIndex[n] = -1;
			rawChanged[n] = false;
		}
		firstGroup = -numGroups;
	}

	public void get(int offset, int recordNum, Record rec) {
		int i;
		long lowest = Long.MAX_VALUE;
		int lowUsed = 0;
		for (i = 0; i < rawRecords.length; i++) {
			if (lastIndex[i] == offset)
				break;
			else if (used[i] < lowest) {
				lowest = used[i];
				lowUsed = i;
			}
		}
		if (i == rawRecords.length) {
			i = lowUsed;
			if (conf.recordGroupUsesLong) {
				if (rawChanged[i]) {
					longGroups[lastIndex[i]] = RecordGroup.longRecordGroup(
							conf, rawRecords[i], 0);
					rawChanged[i] = false;
				}
				lastIndex[i] = offset;
				RecordGroup.getRecords(conf, longGroups[offset], rawRecords[i],
						0);
			} else {
				if (rawChanged[i]) {
					bigIntGroups[lastIndex[i]] = RecordGroup.bigIntRecordGroup(
							conf, rawRecords[i], 0);
					rawChanged[i] = false;
				}
				lastIndex[i] = offset;
				RecordGroup.getRecords(conf, bigIntGroups[offset],
						rawRecords[i], 0);
			}
		}
		used[i] = lastUsed++;
		rec.set(rawRecords[i][recordNum]);
	}

	public void set(int offset, int recordNum, Record rec) {
		int i;
		long lowest = Long.MAX_VALUE;
		int lowUsed = 0;
		for (i = 0; i < rawRecords.length; i++) {
			if (lastIndex[i] == offset)
				break;
			else if (used[i] < lowest) {
				lowest = used[i];
				lowUsed = i;
			}
		}
		if (i == rawRecords.length) {
			i = lowUsed;
			if (conf.recordGroupUsesLong) {
				if (rawChanged[i]) {
					longGroups[lastIndex[i]] = RecordGroup.longRecordGroup(
							conf, rawRecords[i], 0);
					rawChanged[i] = false;
				}
				lastIndex[i] = offset;
				RecordGroup.getRecords(conf, longGroups[offset], rawRecords[i],
						0);
			} else {
				if (rawChanged[i]) {
					bigIntGroups[lastIndex[i]] = RecordGroup.bigIntRecordGroup(
							conf, rawRecords[i], 0);
					rawChanged[i] = false;
				}
				lastIndex[i] = offset;
				RecordGroup.getRecords(conf, bigIntGroups[offset],
						rawRecords[i], 0);
			}
		}
		used[i] = lastUsed++;
		rawRecords[i][recordNum].set(rec);
		rawChanged[i] = true;
		dirty = true;
	}

	public void setGroup(int offset, long group) {
		longGroups[offset] = group;
		for (int i = 0; i < rawRecords.length; i++) {
			if (lastIndex[i] == offset) {
				RecordGroup.getRecords(conf, group, rawRecords[i], 0);
				rawChanged[i] = false;
			}
		}
		dirty = true;
	}

	public void setGroup(int offset, BigInteger group) {
		bigIntGroups[offset] = group;
		for (int i = 0; i < rawRecords.length; i++)
			if (lastIndex[i] == offset) {
				RecordGroup.getRecords(conf, group, rawRecords[i], 0);
				rawChanged[i] = false;
			}
		dirty = true;
	}

	public Iterator<BigInteger> bigIntIterator() {
		return new BigIntPageGroupIterator();
	}

	public LongIterator longIterator() {
		return new LongPageGroupIterator();
	}

	public boolean isDirty() {
		return dirty;
	}

	public void loadPage(Database db, long firstGroup, int numGroups) {
		this.numGroups = numGroups;
		this.firstGroup = firstGroup;
		synchronized (db) {
			if (conf.recordGroupUsesLong) {
				if (longGroups == null || longGroups.length < numGroups)
					longGroups = new long[numGroups];
				LongIterator it = db.getLongRecordGroups(firstGroup
						* conf.recordGroupByteLength, (int) Math.min(numGroups,
						conf.getHasher().numHashes() - firstGroup));
				for (int off = 0; off < numGroups; off++)
					setGroup(off, it.next());
			} else {
				if (bigIntGroups == null || bigIntGroups.length < numGroups) {
					bigIntGroups = new BigInteger[numGroups];
				}
				Iterator<BigInteger> it = db.getBigIntRecordGroups(firstGroup
						* conf.recordGroupByteLength, (int) Math.min(numGroups,
						conf.getHasher().numHashes() - firstGroup));
				for (int off = 0; off < numGroups; off++)
					setGroup(off, it.next());
			}
		}
		dirty = false;
		for (int i = 0; i < rawRecords.length; i++) {
			used[i] = 0;
			lastIndex[i] = -1;
			rawChanged[i] = false;
		}
	}

	public void writeBack(Database db) {
		for (int i = 0; i < rawRecords.length; i++) {
			if (rawChanged[i])
				if (conf.recordGroupUsesLong)
					longGroups[lastIndex[i]] = RecordGroup.longRecordGroup(
							conf, rawRecords[i], 0);
				else
					bigIntGroups[lastIndex[i]] = RecordGroup.bigIntRecordGroup(
							conf, rawRecords[i], 0);
		}
		synchronized (db) {
			if (conf.recordGroupUsesLong)
				db.putRecordGroups(firstGroup * conf.recordGroupByteLength,
						longIterator(), (int) Math.min(numGroups, conf
								.getHasher().numHashes()
								+ 1 - firstGroup));
			else
				db.putRecordGroups(firstGroup * conf.recordGroupByteLength,
						bigIntIterator(), (int) Math.min(numGroups, conf
								.getHasher().numHashes()
								+ 1 - firstGroup));
		}
		dirty = false;
	}

	public static int byteSize(Configuration conf, int numGroups) {
		int myBytes;
		if (conf.recordGroupUsesLong)
			myBytes = (12 + 8 * numGroups + 7) / 8 * 8;
		else {
			myBytes = (12 + 4 * numGroups + 7) / 8 * 8
					+ (32 + (12 + conf.recordGroupByteLength + 8) / 8 * 8)
					* numGroups;
		}
		return myBytes;
	}

	public static int numGroups(Configuration conf, int numBytes) {
		if (conf.recordGroupUsesLong) {
			return (numBytes / 8 * 8 - 19) / 8;
		} else {
			return numBytes
					/ ((32 + (12 + conf.recordGroupByteLength + 8) / 4 + 1) * 8);
		}
	}

	public boolean containsGroup(long hashGroup) {
		long dif = hashGroup - firstGroup;
		return dif >= 0 && dif < numGroups;
	}

	public void putRecord(long hashGroup, int num, Record r) {
		set((int) (hashGroup - firstGroup), num, r);
	}

	public void putRecord(long hash, Record r) {
		set((int) (hash / conf.recordsPerGroup - firstGroup),
				(int) (hash % conf.recordsPerGroup), r);
	}

	public void getRecord(long hashGroup, int num, Record r) {
		get((int) (hashGroup - firstGroup), num, r);
	}
}
