package edu.berkeley.gamesman.database.util;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

public class DelocalizedPage {
	private final long[] longGroups;

	private final BigInteger[] bigIntGroups;

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

	public DelocalizedPage(Configuration conf, int pageSize) {
		this.conf = conf;
		if (conf.recordGroupUsesLong) {
			longGroups = new long[pageSize];
			bigIntGroups = null;
		} else {
			bigIntGroups = new BigInteger[pageSize];
			longGroups = null;
		}
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
		}
	}

	public synchronized void writeBack(Database db) {
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
}
