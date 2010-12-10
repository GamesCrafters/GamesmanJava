package edu.berkeley.gamesman.master;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.Writable;

public class RangeFile implements Writable {
	public Range myRange;
	public FileStatus myFile;

	public RangeFile() {
		myRange = new Range();
		myFile = new FileStatus();
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
}
