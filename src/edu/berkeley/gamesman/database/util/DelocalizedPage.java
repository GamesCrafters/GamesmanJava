package edu.berkeley.gamesman.database.util;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

public class DelocalizedPage {
	private long[] longGroups;

	public int numGroups;

	private BigInteger[] bigIntGroups;

	public long firstGroup;

	private boolean dirty = false;

	private final Configuration conf;

	private final class LongPageGroupIterator implements LongIterator {

		private int i = 0;

		public boolean hasNext() {
			return i < numGroups;
		}

		public long next() {
			return longGroups[i++];
		}
	}

	private final class BigIntPageGroupIterator implements Iterator<BigInteger> {

		private int i = 0;

		public boolean hasNext() {
			return i < numGroups;
		}

		public BigInteger next() {
			return bigIntGroups[i++];
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public DelocalizedPage(Configuration conf) {
		numGroups = 0;
		this.conf = conf;
	}

	public void get(int offset, int recordNum, Record rec) {
		if (conf.recordGroupUsesLong)
			RecordGroup.getRecord(conf, longGroups[offset], recordNum, rec);
		else
			RecordGroup.getRecord(conf, bigIntGroups[offset], recordNum, rec);
	}

	public void set(int offset, int recordNum, Record rec) {
		if (conf.recordGroupUsesLong)
			longGroups[offset] = RecordGroup.setRecord(conf,
					longGroups[offset], recordNum, rec);
		else
			bigIntGroups[offset] = RecordGroup.setRecord(conf,
					bigIntGroups[offset], recordNum, rec);
		dirty = true;
	}

	public void setGroup(int offset, long group) {
		longGroups[offset] = group;
		dirty = true;
	}

	public void setGroup(int offset, BigInteger group) {
		bigIntGroups[offset] = group;
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
		this.firstGroup = firstGroup;
		this.numGroups = numGroups;
		synchronized (db) {
			if (conf.recordGroupUsesLong) {
				if (longGroups == null || longGroups.length < numGroups)
					longGroups = new long[numGroups];
				LongIterator it = db.getLongRecordGroups(firstGroup
						* conf.recordGroupByteLength, numGroups);
				for (int off = 0; off < numGroups; off++)
					setGroup(off, it.next());
			} else {
				if (bigIntGroups == null || bigIntGroups.length < numGroups)
					bigIntGroups = new BigInteger[numGroups];
				Iterator<BigInteger> it = db.getBigIntRecordGroups(firstGroup
						* conf.recordGroupByteLength, numGroups);
				for (int off = 0; off < numGroups; off++)
					setGroup(off, it.next());
			}
		}
		dirty = false;
	}

	public void writeBack(Database db) {
		synchronized (db) {
			if (conf.recordGroupUsesLong)
				db.putRecordGroups(firstGroup * conf.recordGroupByteLength,
						longIterator(), numGroups);
			else
				db.putRecordGroups(firstGroup * conf.recordGroupByteLength,
						bigIntIterator(), numGroups);
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

	public void getRecord(long hashGroup, int num, Record r) {
		get((int) (hashGroup - firstGroup), num, r);
	}
}
