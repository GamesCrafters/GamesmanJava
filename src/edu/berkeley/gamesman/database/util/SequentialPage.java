package edu.berkeley.gamesman.database.util;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;

/**
 * A page that remembers the last record group accessed. If stepping through a
 * database sequentially, it only needs to extract the records from each group
 * once
 * 
 * @author dnspies
 */
public class SequentialPage extends Page {
	Record[] lastGroup;

	int lastGroupNum;

	boolean lastDirty;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public SequentialPage(Configuration conf) {
		super(conf);
		lastGroup = new Record[conf.recordsPerGroup];
		lastGroupNum = -1;
		lastDirty = false;
		for (int i = 0; i < conf.recordsPerGroup; i++)
			lastGroup[i] = conf.getGame().newRecord();
	}

	@Override
	public void get(int groupNum, int recordNum, Record rec) {
		if (lastGroupNum != groupNum) {
			if (conf.recordGroupUsesLong) {
				if (lastDirty)
					setGroup(lastGroupNum, RecordGroup.longRecordGroup(conf,
							lastGroup, 0));
				lastGroupNum = groupNum;
				RecordGroup.getRecords(conf, getLongGroup(groupNum), lastGroup,
						0);
			} else {
				if (lastDirty)
					setGroup(lastGroupNum, RecordGroup.bigIntRecordGroup(conf,
							lastGroup, 0));
				lastGroupNum = groupNum;
				RecordGroup.getRecords(conf, getBigIntGroup(groupNum),
						lastGroup, 0);
			}
			lastDirty = false;
		}
		rec.set(lastGroup[recordNum]);
	}

	@Override
	public void set(int groupNum, int recordNum, Record rec) {
		if (lastGroupNum != groupNum) {
			if (conf.recordGroupUsesLong) {
				if (lastDirty)
					setGroup(lastGroupNum, RecordGroup.longRecordGroup(conf,
							lastGroup, 0));
				lastGroupNum = groupNum;
				RecordGroup.getRecords(conf, getLongGroup(groupNum), lastGroup,
						0);
			} else {
				if (lastDirty)
					setGroup(lastGroupNum, RecordGroup.bigIntRecordGroup(conf,
							lastGroup, 0));
				lastGroupNum = groupNum;
				RecordGroup.getRecords(conf, getBigIntGroup(groupNum),
						lastGroup, 0);
			}
		}
		lastGroup[recordNum].set(rec);
		lastDirty = true;
		dirty = true;
	}

	@Override
	public void writeBack(Database db, DatabaseHandle dh) {
		if (lastDirty) {
			if (conf.recordGroupUsesLong)
				setGroup(lastGroupNum, RecordGroup.longRecordGroup(conf,
						lastGroup, 0));
			else
				setGroup(lastGroupNum, RecordGroup.bigIntRecordGroup(conf,
						lastGroup, 0));
			lastDirty = false;
		}
		super.writeBack(db, dh);
	}

	@Override
	public void loadPage(Database db, DatabaseHandle dh, long firstGroup,
			int numGroups) {
		lastGroupNum = -1;
		lastDirty = false;
		super.loadPage(db, dh, firstGroup, numGroups);
	}
}
