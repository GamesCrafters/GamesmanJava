package edu.berkeley.gamesman.database.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import java.util.ArrayList;

/**
 * SplitDatabaseWritableList is the list of all databases for a given tier,
 * and is a copy of the input iterator given to reduce().
 * 
 * @see SplitDatabaseWritable
 * @author Patrick Horn
 */
public class SplitDatabaseWritableList extends ArrayList<SplitDatabaseWritable> {
	private static final long serialVersionUID = 1L;

	int tier;

	/**
	 * Default constructor for deserializing.
	 */
	public SplitDatabaseWritableList() {
	}

	/**
	 * Constructor, takes the tier number.
	 * @param tier The tier that this database list represents.
	 */
	public SplitDatabaseWritableList(int tier) {
		this.tier = tier;
	}

	/**
	 * @param w Another database to add to set. Doesn't need to be in order.
	 */
	public void addDatabase(SplitDatabaseWritable w) {
		add(w);
	}

	public void readFields(DataInput in) throws IOException {
		tier = in.readInt();
		try {
			while (true) {
				SplitDatabaseWritable sdw = new SplitDatabaseWritable();
				sdw.readFields(in);
				add(sdw);
			}
		} catch(EOFException e) {
		}
	}
	public void write(DataOutput out) throws IOException {
		out.writeInt(tier);
		for (SplitDatabaseWritable sdw : this) {
			sdw.write(out);
		}
	}
}
