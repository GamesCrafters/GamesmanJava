package edu.berkeley.gamesman.database.util;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;

/**
 * A page that keeps track of the individual records extracted from the last few
 * record groups. This way, if all the records asked for are right next to each
 * other, less calculations need to be made
 * 
 * @author dnspies
 */
public class LocalizedPage extends Page {
	private final Record[][] rawRecords;

	private long[] used;

	private long lastUsed = 0L;

	private boolean[] rawChanged;

	private int[] lastIndex;

	/**
	 * @param conf
	 *            The configuration object
	 * @param groupsRemembered
	 *            The number of groups to remember the records from
	 */
	public LocalizedPage(Configuration conf, int groupsRemembered) {
		super(conf);
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

	@Override
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
					setGroup(lastIndex[i], RecordGroup.longRecordGroup(conf,
							rawRecords[i], 0));
					rawChanged[i] = false;
				}
				lastIndex[i] = offset;
				RecordGroup.getRecords(conf, getLongGroup(offset),
						rawRecords[i], 0);
			} else {
				if (rawChanged[i]) {
					setGroup(lastIndex[i], RecordGroup.bigIntRecordGroup(conf,
							rawRecords[i], 0));
					rawChanged[i] = false;
				}
				lastIndex[i] = offset;
				RecordGroup.getRecords(conf, getBigIntGroup(offset),
						rawRecords[i], 0);
			}
		}
		used[i] = ++lastUsed;
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
					setGroup(lastIndex[i], RecordGroup.longRecordGroup(conf,
							rawRecords[i], 0));
					rawChanged[i] = false;
				}
				lastIndex[i] = offset;
				RecordGroup.getRecords(conf, getLongGroup(offset),
						rawRecords[i], 0);
			} else {
				if (rawChanged[i]) {
					setGroup(lastIndex[i], RecordGroup.bigIntRecordGroup(conf,
							rawRecords[i], 0));
					rawChanged[i] = false;
				}
				lastIndex[i] = offset;
				RecordGroup.getRecords(conf, getBigIntGroup(offset),
						rawRecords[i], 0);
			}
		}
		used[i] = ++lastUsed;
		rawRecords[i][recordNum].set(rec);
		rawChanged[i] = true;
	}

	public void loadPage(Database db, long firstGroup, int numGroups) {
		super.loadPage(db, firstGroup, numGroups);
		for (int i = 0; i < rawRecords.length; i++) {
			used[i] = 0L;
			lastIndex[i] = -1;
			rawChanged[i] = false;
		}
	}

	public void writeBack(Database db) {
		for (int i = 0; i < rawRecords.length; i++) {
			if (rawChanged[i]) {
				if (conf.recordGroupUsesLong)
					setGroup(lastIndex[i], RecordGroup.longRecordGroup(conf,
							rawRecords[i], 0));
				else
					setGroup(lastIndex[i], RecordGroup.bigIntRecordGroup(conf,
							rawRecords[i], 0));
				rawChanged[i] = false;
			}
		}
		super.writeBack(db);
	}
}
