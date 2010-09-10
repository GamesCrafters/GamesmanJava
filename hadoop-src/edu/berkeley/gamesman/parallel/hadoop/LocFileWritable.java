package edu.berkeley.gamesman.parallel.hadoop;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

public class LocFileWritable implements Writable, Comparable<LocFileWritable> {
	public final LongWritable loc;
	public final FileStatus file;

	public LocFileWritable() {
		loc = new LongWritable();
		file = new FileStatus();
	}

	public LocFileWritable(long loc, FileStatus file) {
		this(new LongWritable(loc), file);
	}

	public LocFileWritable(LongWritable loc, FileStatus file) {
		this.loc = loc;
		this.file = file;
	}

	public void readFields(DataInput in) throws IOException {
		loc.readFields(in);
		file.readFields(in);
	}

	public void write(DataOutput out) throws IOException {
		loc.write(out);
		file.write(out);
	}

	// Note: this class has a natural ordering that is inconsistent with equals.
	public int compareTo(LocFileWritable o) {
		return loc.compareTo(o.loc);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LocFileWritable))
			return false;
		LocFileWritable other = (LocFileWritable) o;
		return loc.equals(other.loc) && file.equals(other.file);
	}
}
