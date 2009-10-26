package edu.berkeley.gamesman.hadoop.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import java.util.List;
import java.util.ArrayList;

public class SplitDatabaseWritableList implements Writable{
	private List<SplitDatabaseWritable> list;
	int tier;

	public SplitDatabaseWritableList() {
		list = new ArrayList<SplitDatabaseWritable>();
	}

	public void setTier(int tier) {
		this.tier = tier;
	}
	public void addDatabase(SplitDatabaseWritable w) {
		list.add(w);
	}
	public void readFields(DataInput in) throws IOException {
		tier = in.readInt();
		try {
			while (true) {
				SplitDatabaseWritable sdw = new SplitDatabaseWritable(tier);
				sdw.readFields(in);
				list.add(sdw);
			}
		} catch(EOFException e) {
		}
	}
	public void write(DataOutput out) throws IOException {
		out.writeInt(tier);
		for (SplitDatabaseWritable sdw : list) {
			sdw.write(out);
		}
	}
}
