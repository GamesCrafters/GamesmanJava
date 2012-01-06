package edu.berkeley.gamesman.parallel;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

/**
 * A range of hashes in the game
 * 
 * @author dnspies
 * 
 */
public class Range implements WritableComparable<Range> {
	/**
	 * The first element of the range
	 */
	public long firstRecord;
	/**
	 * The number of elements in the range
	 */
	public long numRecords;

	/**
	 * Constructs a range
	 * 
	 * @param first
	 *            The first element of the range
	 * @param numRecords
	 *            The number of elements in the range
	 */
	public Range(long first, long numRecords) {
		this.firstRecord = first;
		this.numRecords = numRecords;
	}

	/**
	 * Empty constructor for writable comparable
	 */
	public Range() {
		// All Writable classes must include an empty constructor
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(firstRecord);
		out.writeLong(numRecords);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		firstRecord = in.readLong();
		numRecords = in.readLong();
	}

	@Override
	public int compareTo(Range o) {
		if (firstRecord > o.firstRecord)
			return 1;
		else if (firstRecord < o.firstRecord)
			return -1;
		else if (numRecords > o.numRecords)
			return 1;
		else if (numRecords < o.numRecords)
			return -1;
		else
			return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Range) {
			Range o = (Range) other;
			return firstRecord == o.firstRecord && numRecords == o.numRecords;
		} else
			return false;
	}

	/**
	 * Sets this range to match r
	 * 
	 * @param r
	 *            The range to match
	 */
	public void set(Range r) {
		firstRecord = r.firstRecord;
		numRecords = r.numRecords;
	}
}
