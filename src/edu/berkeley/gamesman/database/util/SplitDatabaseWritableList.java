package edu.berkeley.gamesman.database.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import java.util.ArrayList;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Util;

/**
 * SplitDatabaseWritableList is the list of all databases for a given tier,
 * and is a copy of the input iterator given to reduce().
 * 
 * @see SplitDatabaseWritable
 * @author Patrick Horn
 */
public class SplitDatabaseWritableList extends ArrayList<SplitDatabaseWritable> {
	private static final long serialVersionUID = 1L;
	
	private Configuration conf;

	/** @return the configuration used to create the SplitDatabase. */
	public Configuration getConf() {
		return conf;
	}

	/**
	 * @param conf Set the configuration to be written to the SplitDatabase.
	 */
	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * Default constructor for deserializing.
	 */
	public SplitDatabaseWritableList() {
	}

	public void readFields(DataInput in) throws IOException {
		try {
			int conflen = in.readInt();
			if (conflen > 0) {
				byte[] confArray = new byte[conflen];
				in.readFully(confArray);
				if (conf == null) {
					try {
						conf = Configuration.load(confArray);
					} catch (ClassNotFoundException e) {
						Util.fatalError("Failed to load configuration for SplitSolidDatabase",e);
					}
				}
			}
			while (true) {
				SplitDatabaseWritable sdw = new SplitDatabaseWritable();
				sdw.readFields(in);
				add(sdw);
			}
		} catch(EOFException e) {
		}
	}
	public void write(DataOutput out) throws IOException {
		if (conf != null) {
			byte[] storedConf = conf.store();
			out.writeInt(storedConf.length);
			out.write(storedConf);
		} else {
			out.writeInt(0);
		}
		for (SplitDatabaseWritable sdw : this) {
			sdw.write(out);
		}
	}
}
