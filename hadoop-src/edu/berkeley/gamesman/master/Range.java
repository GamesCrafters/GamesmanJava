package edu.berkeley.gamesman.master;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class Range implements WritableComparable<Range> {
	long firstRecord;
	long numRecords;

        public Range(long first, long numRecords) {
            this.firstRecord = first;
            this.numRecords = numRecords;
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

        public long getLength() {
            return numRecords;
        }

}
