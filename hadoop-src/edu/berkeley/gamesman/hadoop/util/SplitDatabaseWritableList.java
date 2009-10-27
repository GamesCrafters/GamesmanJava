package edu.berkeley.gamesman.hadoop.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

import java.util.List;
import java.util.ArrayList;

/**
 * SplitDatabaseWritableList is the list of all databases for a given tier,
 * and is a copy of the input iterator given to reduce().
 * 
 * @see SplitDatabaseOutputFormat
 * @see SplitDatabaseWritable
 * @author Patrick Horn
 */
public class SplitDatabaseWritableList implements Writable{
	private List<SplitDatabaseWritable> list;
	int tier;

	/**
	 * Constructor, takes the tier number.
	 * @param tier The tier that this database list represents.
	 */
	public SplitDatabaseWritableList(int tier) {
		list = new ArrayList<SplitDatabaseWritable>();
		this.tier = tier;
	}

	/**
	 * @param w Another database to add to set. Doesn't need to be in order.
	 */
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
