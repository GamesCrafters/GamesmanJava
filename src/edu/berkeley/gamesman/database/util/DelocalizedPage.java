package edu.berkeley.gamesman.database.util;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.LongIterator;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

public class DelocalizedPage {
	private byte[] groups;

	public int numGroups = 0;

	public long firstGroup;

	private boolean dirty = false;

	private final Configuration conf;

	private final class LongPageGroupIterator implements LongIterator {

		private int i = 0;

		public boolean hasNext() {
			return i < numGroups;
		}

		public long next() {
			long result = RecordGroup.longRecordGroup(conf, groups, i);
			i += conf.recordGroupByteLength;
			return result;
		}
	}

	private final class BigIntPageGroupIterator implements Iterator<BigInteger> {

		private int i = 0;

		public boolean hasNext() {
			return i < numGroups;
		}

		public BigInteger next() {
			BigInteger result = RecordGroup.bigIntRecordGroup(conf, groups, i);
			i += conf.recordGroupByteLength;
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public DelocalizedPage(Configuration conf) {
		numGroups = 0;
		this.conf = conf;
	}

	public void get(int groupNum, int recordNum, Record rec) {
		if (conf.recordGroupUsesLong)
			RecordGroup.getRecord(conf, getLongGroup(groupNum), recordNum, rec);
		else
			RecordGroup.getRecord(conf, getBigIntGroup(groupNum), recordNum,
					rec);
	}

	public void set(int groupNum, int recordNum, Record rec) {
		if (conf.recordGroupUsesLong)
			setGroup(groupNum, RecordGroup.setRecord(conf,
					getLongGroup(groupNum), recordNum, rec));
		else
			setGroup(groupNum, RecordGroup.setRecord(conf,
					getBigIntGroup(groupNum), recordNum, rec));
		dirty = true;
	}

	public void setGroup(int groupNum, long group) {
		RecordGroup.toUnsignedByteArray(conf, group, groups, groupNum
				* conf.recordGroupByteLength);
		dirty = true;
	}

	public long getLongGroup(int groupNum) {
		return RecordGroup.longRecordGroup(conf, groups, groupNum
				* conf.recordGroupByteLength);
	}

	public BigInteger getBigIntGroup(int groupNum) {
		return RecordGroup.bigIntRecordGroup(conf, groups, groupNum
				* conf.recordGroupByteLength);
	}

	public void setGroup(int groupNum, BigInteger group) {
		RecordGroup.toUnsignedByteArray(conf, group, groups, groupNum
				* conf.recordGroupByteLength);
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
		int arrSize = numGroups * conf.recordGroupByteLength;
		if (groups == null || groups.length < arrSize)
			groups = new byte[arrSize];
		db.getBytes(firstGroup * conf.recordGroupByteLength, groups, 0,
						arrSize);
		dirty = false;
	}

	public void writeBack(Database db) {
		int arrSize = numGroups * conf.recordGroupByteLength;
		db.putBytes(firstGroup * conf.recordGroupByteLength, groups, 0,
						arrSize);
		dirty = false;
	}

	public boolean containsGroup(long hashGroup) {
		long dif = hashGroup - firstGroup;
		return dif >= 0 && dif < numGroups;
	}

	public void getRecord(long hashGroup, int num, Record r) {
		get((int) (hashGroup - firstGroup), num, r);
	}
}
