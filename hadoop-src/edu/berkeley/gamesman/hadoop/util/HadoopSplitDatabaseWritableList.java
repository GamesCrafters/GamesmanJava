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

	/**
	 * Constructor, takes the tier number.
	 * @param tier The tier that this database list represents.
	 */
	public HadoopSplitDatabaseWritableList(int tier) {
		super(tier);
	}

	private static final long serialVersionUID = 1L;
}
