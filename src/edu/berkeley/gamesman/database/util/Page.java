package edu.berkeley.gamesman.database.util;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;

public class Page {
	GroupHolder[] groups;

	private static final class GroupHolder {
		private RecordGroup rg;

		private Record[] r;

		private boolean called;

		private boolean changed;

		public GroupHolder(Configuration conf) {
			rg = new RecordGroup(conf);
			r = new Record[conf.recordsPerGroup];
			for (int i = 0; i < conf.recordsPerGroup; i++)
				r[i] = conf.getGame().newRecord();
		}

		public void get(int recordNum, Record record) {
			if (!called) {
				called = true;
				rg.getRecords(r, 0);
			}
			record.set(r[recordNum]);
		}

		public void set(int recordNum, Record record) {
			if (!called) {
				called = true;
				rg.getRecords(r, 0);
			}
			changed = true;
			r[recordNum].set(record);
		}

		public void setGroup(RecordGroup group) {
			rg.set(group);
			called = false;
			changed = false;
		}

		public RecordGroup getGroup() {
			if (changed) {
				rg.set(r, 0);
				changed = false;
			}
			return rg;
		}

		private static int byteSize(Configuration conf) {
			return 40 + RecordGroup.byteSize(conf) + (conf.recordsPerGroup + 1)
					/ 2 * 8 + conf.recordsPerGroup * Record.byteSize(conf);
		}
	}

	private final class PageGroupIterator implements Iterator<RecordGroup> {

		private int i = 0;

		public boolean hasNext() {
			return i < groups.length;
		}

		public RecordGroup next() {
			return groups[i++].getGroup();
		}

		public void remove() {
			throw new UnsupportedOperationException("remove not supported here");
		}
	}

	public Page(Configuration conf, int pageSize) {
		groups = new GroupHolder[pageSize];
		for (int i = 0; i < pageSize; i++)
			groups[i] = new GroupHolder(conf);
	}

	public void get(int offset, int recordNum, Record rec) {
		groups[offset].get(recordNum, rec);
	}

	public void set(int offset, int recordNum, Record rec) {
		groups[offset].set(recordNum, rec);
	}

	public void setGroup(int offset, RecordGroup group) {
		groups[offset].setGroup(group);
	}

	public Iterator<RecordGroup> iterator() {
		return new PageGroupIterator();
	}

	public static int groupHolderByteSize(Configuration conf) {
		return GroupHolder.byteSize(conf);
	};
}
