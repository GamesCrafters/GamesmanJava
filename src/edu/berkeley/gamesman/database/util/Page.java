package edu.berkeley.gamesman.database.util;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

public class Page {
	private final long[] longGroups;

	private final BigInteger[] bigIntGroups;

	private final Record[][] rawRecords;

	private long[] used;

	private long lastUsed = 0;

	private boolean[] rawChanged;

	private int[] lastIndex;

	private long firstGroup;

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

	public Page(Configuration conf, int pageSize, int groupsRemembered) {
		this.conf = conf;
		if (conf.recordGroupUsesLong) {
			longGroups = new long[pageSize];
			bigIntGroups = null;
		} else {
			bigIntGroups = new BigInteger[pageSize];
			longGroups = null;
		}
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
		for (int i = 0; i < rawRecords.length; i++)
			if (lastIndex[i] == offset)
				RecordGroup.getRecords(conf, group, rawRecords[i], 0);
		dirty = true;
	}

	public void setGroup(int offset, BigInteger group) {
		bigIntGroups[offset] = group;
		for (int i = 0; i < rawRecords.length; i++)
			if (lastIndex[i] == offset)
				RecordGroup.getRecords(conf, group, rawRecords[i], 0);
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

	public synchronized void loadPage(Database db, long firstGroup) {
		synchronized (db) {
			this.firstGroup = firstGroup;
			if (conf.recordGroupUsesLong) {
				LongIterator it = db.getLongRecordGroups(firstGroup
						* conf.recordGroupByteLength, longGroups.length);
				for (int off = 0; off < longGroups.length; off++)
					setGroup(off, it.next());
			} else {
				Iterator<BigInteger> it = db.getBigIntRecordGroups(firstGroup
						* conf.recordGroupByteLength, bigIntGroups.length);
				for (int off = 0; off < bigIntGroups.length; off++)
					setGroup(off, it.next());
			}
			dirty = false;
			for (int i = 0; i < rawRecords.length; i++) {
				used[i] = 0;
				lastIndex[i] = -1;
				rawChanged[i] = false;
			}
		}
	}

	public synchronized void writeBack(Database db) {
		for (int i = 0; i < rawRecords.length; i++) {
			if (rawChanged[i])
				if (conf.recordGroupUsesLong)
					longGroups[i] = RecordGroup.longRecordGroup(conf,
							rawRecords[i], 0);
				else
					bigIntGroups[i] = RecordGroup.bigIntRecordGroup(conf,
							rawRecords[i], 0);
		}
		synchronized (db) {
			if (conf.recordGroupUsesLong)
				db.putRecordGroups(firstGroup * conf.recordGroupByteLength,
						longIterator(), longGroups.length);
			else
				db.putRecordGroups(firstGroup * conf.recordGroupByteLength,
						bigIntIterator(), bigIntGroups.length);
		}
		dirty = false;
	}

	public static int byteSize(Configuration conf, int numGroups) {
		int myBytes = 48;
		myBytes += (12 + conf.recordsPerGroup * 4 + 7) / 8 * 8;
		myBytes += conf.recordsPerGroup * Record.byteSize(conf);
		if (conf.recordGroupUsesLong)
			myBytes += (12 + 8 * numGroups + 7) / 8 * 8;
		else {
			myBytes += (12 + 4 * numGroups + 7) / 8 * 8;
			myBytes += (32 + (12 + conf.recordGroupByteLength + 8) / 8 * 8)
					* numGroups;
		}
		return myBytes;
	}

	public static int numGroups(Configuration conf, int numBytes) {
		int divider = numBytes;
		divider -= 48;
		divider -= (12 + conf.recordsPerGroup * 4 + 7) / 8 * 8;
		divider -= conf.recordsPerGroup * Record.byteSize(conf);
		if (conf.recordGroupUsesLong) {
			divider -= 16;
			return (divider / 8 * 8 - 3) / 8;
		} else {
			divider -= 16;
			return divider
					/ ((32 + (12 + conf.recordGroupByteLength + 8) / 4 + 1) * 8);
		}
	}
}
