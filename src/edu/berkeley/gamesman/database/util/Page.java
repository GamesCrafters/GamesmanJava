package edu.berkeley.gamesman.database.util;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;

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

	protected boolean dirty = false;

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
	 */
	public long get(int groupNum, int recordNum) {
		if (conf.recordGroupUsesLong)
			return RecordGroup.getRecord(conf, getLongGroup(groupNum),
					recordNum);
		else
			return RecordGroup.getRecord(conf, getBigIntGroup(groupNum),
					recordNum);
	}

	/**
	 * Changes a record on this page to rec
	 * 
	 * @param groupNum
	 *            The index into this page of the desired group
	 * @param recordNum
	 *            The index into the group of the desired record
	 * @param r
	 *            The record
	 */
	public void set(int groupNum, int recordNum, long r) {
		if (conf.recordGroupUsesLong)
			setGroup(groupNum, RecordGroup.setRecord(conf,
					getLongGroup(groupNum), recordNum, r));
		else
			setGroup(groupNum, RecordGroup.setRecord(conf,
					getBigIntGroup(groupNum), recordNum, r));
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
	public void loadPage(Database db, DatabaseHandle dh, long firstGroup,
			int numGroups) {
		loadPage(firstGroup, numGroups);
		db.getBytes(dh, firstGroup * conf.recordGroupByteLength, groups, 0,
				numGroups * conf.recordGroupByteLength);
		dirty = false;
	}

	/**
	 * Writes this page back to the database
	 * 
	 * @param db
	 *            The database
	 */
	public void writeBack(Database db, DatabaseHandle dh) {
		int arrSize = numGroups * conf.recordGroupByteLength;
		db.putBytes(dh, firstGroup * conf.recordGroupByteLength, groups, 0,
				arrSize);
		dirty = false;
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
	public long getRecord(long hashGroup, int num) {
		return get((int) (hashGroup - firstGroup), num);
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
	public void putRecord(long hashGroup, int num, long r) {
		set((int) (hashGroup - firstGroup), num, r);
	}

	/**
	 * Extends this page to include all groups up through hashGroup
	 * 
	 * @param db
	 *            The database
	 * @param hashGroup
	 *            The group to extend up to (inclusive)
	 */
	public void extendUp(Database db, DatabaseHandle dh, long hashGroup) {
		long first = firstGroup + numGroups;
		int numAdd = (int) (hashGroup - first + 1);
		int neededGroups = numGroups + numAdd;
		int oldSize = numGroups * conf.recordGroupByteLength;
		int remainSize = numAdd * conf.recordGroupByteLength;
		ensureCapacity(neededGroups);
		db.getBytes(dh, first * conf.recordGroupByteLength, groups, oldSize,
				remainSize);
		numGroups = neededGroups;
	}

	/**
	 * Extends this page to include all groups down through hashGroup
	 * 
	 * @param db
	 *            The database
	 * @param hashGroup
	 *            The group to extend down to (inclusive)
	 */
	public void extendDown(Database db, DatabaseHandle dh, long hashGroup) {
		extendDown(db, dh, hashGroup, -1);
	}

	/**
	 * Extends this page to include all groups down through hashGroup
	 * 
	 * @param db
	 *            The database
	 * @param hashGroup
	 *            The group to extend down to (inclusive)
	 * @param neededGroups
	 *            The new minimum number of groups on the page
	 */
	public void extendDown(Database db, DatabaseHandle dh, long hashGroup,
			int neededGroups) {
		int numAdd = (int) (firstGroup - hashGroup);
		int totGroups = numGroups + numAdd;
		int oldSize = numGroups * conf.recordGroupByteLength;
		int remainSize = numAdd * conf.recordGroupByteLength;
		int arrSize;
		if (neededGroups < 0)
			arrSize = oldSize + remainSize;
		else
			arrSize = neededGroups * conf.recordGroupByteLength;
		if (groups.length < arrSize) {
			byte[] newGroups = new byte[arrSize];
			for (int i = 0; i < oldSize; i++) {
				newGroups[i + remainSize] = groups[i];
			}
			groups = newGroups;
		} else {
			for (int i = oldSize - 1; i >= 0; i--) {
				groups[i + remainSize] = groups[i];
			}
		}
		db.getBytes(dh, hashGroup * conf.recordGroupByteLength, groups, 0,
				remainSize);
		numGroups = totGroups;
		firstGroup = hashGroup;
	}

	/**
	 * Extends this page to include the other page
	 * 
	 * @param db
	 *            The database to draw the intervening records from
	 * @param p
	 *            The other page
	 */
	public void extendUp(Database db, DatabaseHandle dh, Page p) {
		int neededGroups = p.numGroups + (int) (p.firstGroup - firstGroup);
		ensureCapacity(neededGroups);
		extendUp(db, dh, p.firstGroup - 1L);
		int numAdd = p.numGroups;
		int totGroups = numGroups + numAdd;
		int oldSize = numGroups * conf.recordGroupByteLength;
		int remainSize = numAdd * conf.recordGroupByteLength;
		for (int i = 0; i < remainSize; i++)
			groups[i + oldSize] = p.groups[i];
		numGroups = totGroups;
	}

	/**
	 * @param neededGroups
	 *            The size
	 * @return Whether the page needed to be modified
	 */
	public boolean ensureCapacity(int neededGroups) {
		int arrSize = neededGroups * conf.recordGroupByteLength;
		if (neededGroups == 0)
			return false;
		else if (groups == null) {
			groups = new byte[arrSize];
			return true;
		} else if (groups.length < arrSize) {
			int oldSize = numGroups * conf.recordGroupByteLength;
			byte[] newGroups = new byte[arrSize];
			for (int i = 0; i < oldSize; i++) {
				newGroups[i] = groups[i];
			}
			groups = newGroups;
			return true;
		} else
			return false;
	}

	/**
	 * Loads zero groups into this page (Should only be used if every group on
	 * the page is going to be written)
	 * 
	 * @param firstGroup
	 *            The first group to load
	 * @param numGroups
	 *            The number of groups to load
	 */
	public void loadPage(long firstGroup, int numGroups) {
		this.firstGroup = firstGroup;
		this.numGroups = numGroups;
		ensureCapacity(numGroups);
		dirty = true;
	}
}
