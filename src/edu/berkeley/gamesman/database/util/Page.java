package edu.berkeley.gamesman.database.util;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * A page for caching RecordGroups
 * 
 * @author dnspies
 */
public class Page {
	private byte[] groups;

	/**
	 * The number of groups this page contains
	 */
	public int numGroups = 0;

	/**
	 * The first group contained by this page
	 */
	public long firstGroup;

	private boolean dirty = false;

	protected final Configuration conf;

	/**
	 * @param conf
	 *            The configuration object
	 */
	public Page(Configuration conf) {
		numGroups = 0;
		this.conf = conf;
	}

	/**
	 * Retrieves a record from this Page and stores it in rec
	 * 
	 * @param groupNum
	 *            The index into this page of the desired group
	 * @param recordNum
	 *            The index into the group of the desired record
	 * @param rec
	 *            A record to store the result in
	 */
	public void get(int groupNum, int recordNum, Record rec) {
		if (conf.recordGroupUsesLong)
			RecordGroup.getRecord(conf, getLongGroup(groupNum), recordNum, rec);
		else
			RecordGroup.getRecord(conf, getBigIntGroup(groupNum), recordNum,
					rec);
	}

	/**
	 * Changes a record on this page to rec
	 * 
	 * @param groupNum
	 *            The index into this page of the desired group
	 * @param recordNum
	 *            The index into the group of the desired record
	 * @param rec
	 *            The record
	 */
	public void set(int groupNum, int recordNum, Record rec) {
		if (conf.recordGroupUsesLong)
			setGroup(groupNum, RecordGroup.setRecord(conf,
					getLongGroup(groupNum), recordNum, rec));
		else
			setGroup(groupNum, RecordGroup.setRecord(conf,
					getBigIntGroup(groupNum), recordNum, rec));
		dirty = true;
	}

	/**
	 * Changes a group on this page to group
	 * 
	 * @param groupNum
	 *            The index into this page of the desired group
	 * @param group
	 *            The group
	 */
	public void setGroup(int groupNum, long group) {
		RecordGroup.toUnsignedByteArray(conf, group, groups, groupNum
				* conf.recordGroupByteLength);
		dirty = true;
	}

	/**
	 * Changes a group on this page to group
	 * 
	 * @param groupNum
	 *            The index into this page of the desired group
	 * @param group
	 *            The group
	 */
	public void setGroup(int groupNum, BigInteger group) {
		RecordGroup.toUnsignedByteArray(conf, group, groups, groupNum
				* conf.recordGroupByteLength);
		dirty = true;
	}

	/**
	 * Returns a group from this Page
	 * 
	 * @param groupNum
	 *            The index into this page of the desired group
	 * @return The group
	 */
	public long getLongGroup(int groupNum) {
		return RecordGroup.longRecordGroup(conf, groups, groupNum
				* conf.recordGroupByteLength);
	}

	/**
	 * Returns a group from this Page
	 * 
	 * @param groupNum
	 *            The index into this page of the desired group
	 * @return The group
	 */
	public BigInteger getBigIntGroup(int groupNum) {
		return RecordGroup.bigIntRecordGroup(conf, groups, groupNum
				* conf.recordGroupByteLength);
	}

	/**
	 * @return Whether values on this page have been changed
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Reloads this page from a position in the database
	 * 
	 * @param db
	 *            The database
	 * @param firstGroup
	 *            The first group to load
	 * @param numGroups
	 *            The number of groups to load
	 */
	public void loadPage(Database db, long firstGroup, int numGroups) {
		this.firstGroup = firstGroup;
		this.numGroups = numGroups;
		int arrSize = numGroups * conf.recordGroupByteLength;
		if (groups == null || groups.length < arrSize)
			groups = new byte[arrSize];
		db
				.getBytes(firstGroup * conf.recordGroupByteLength, groups, 0,
						arrSize);
		dirty = false;
	}

	/**
	 * Writes this page back to the database
	 * 
	 * @param db
	 *            The database
	 */
	public void writeBack(Database db) {
		int arrSize = numGroups * conf.recordGroupByteLength;
		db
				.putBytes(firstGroup * conf.recordGroupByteLength, groups, 0,
						arrSize);
		dirty = false;
	}

	/**
	 * @param hashGroup
	 *            The group
	 * @return whether this page contains the group at absolute index hashGroup
	 */
	public boolean containsGroup(long hashGroup) {
		long dif = hashGroup - firstGroup;
		return dif >= 0 && dif < numGroups;
	}

	/**
	 * Retrieves a record from this page and writes it to r
	 * 
	 * @param hashGroup
	 *            The absolute index of the group
	 * @param num
	 *            The index into the group
	 * @param r
	 *            A record to store the result in
	 */
	public void getRecord(long hashGroup, int num, Record r) {
		get((int) (hashGroup - firstGroup), num, r);
	}

	/**
	 * Writes r to a place in the page
	 * 
	 * @param hashGroup
	 *            The absolute index of the group
	 * @param num
	 *            The index into the group
	 * @param r
	 *            The record
	 */
	public void putRecord(long hashGroup, int num, Record r) {
		set((int) (hashGroup - firstGroup), num, r);
	}
}
