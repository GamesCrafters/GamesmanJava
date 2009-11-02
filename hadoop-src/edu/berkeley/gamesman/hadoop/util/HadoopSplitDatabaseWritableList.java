package edu.berkeley.gamesman.hadoop.util;

import org.apache.hadoop.io.Writable;
import edu.berkeley.gamesman.database.util.SplitDatabaseWritableList;

/** Hadoop version of SplitDatabaseWritable */
public class HadoopSplitDatabaseWritableList extends SplitDatabaseWritableList implements Writable {

	/**
	 * Default constructor for deserializing.
	 */
	public HadoopSplitDatabaseWritableList() {
		super();
	}

	private static final long serialVersionUID = 1L;
}
