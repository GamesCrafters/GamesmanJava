package edu.berkeley.gamesman.parallel;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.WritableComparable;

public class RangeFile implements WritableComparable<RangeFile> {
	public Range myRange;
	public FileStatus myFile;

	public RangeFile() {
		this(new Range(), new FileStatus());
	}

	public RangeFile(Range key, FileStatus file) {
		myRange = key;
		myFile = file;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		myRange.write(out);
		myFile.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		myRange.readFields(in);
		myFile.readFields(in);
	}

	public void set(Range range, FileStatus file) {
		myRange = range;
		myFile = file;
	}

	@Override
	public int compareTo(RangeFile o) {
		return myRange.compareTo(o.myRange);
	}
}
