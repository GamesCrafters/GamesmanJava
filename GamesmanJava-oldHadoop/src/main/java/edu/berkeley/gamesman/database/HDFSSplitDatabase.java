package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;

/**
 * A SplitDatabase on HDFS (for reading only)
 * 
 * @author dnspies
 */
public class HDFSSplitDatabase extends SplitDatabase {

	/**
	 * @param uri
	 *            The file (on HDFS) containing the database headers for this
	 *            split database
	 * @param conf
	 *            The configuration object
	 * @param firstRecordIndex
	 *            The index of the first record contained in this database
	 * @param numRecords
	 *            The number of records contained in this database
	 * @param reading
	 *            Whether to open the database for reading
	 * @param writing
	 *            Whether to open the database for writing
	 * @throws IOException
	 *             If an IOException occurs while reading the header
	 * @throws ClassNotFoundException
	 *             If a ClassNotFoundException occurs while reading the
	 *             configuration for any underlying database
	 * @throws URISyntaxException 
	 */
	public HDFSSplitDatabase(String uri, Configuration conf,
			long firstRecordIndex, long numRecords, boolean reading,
			boolean writing) throws IOException, ClassNotFoundException, URISyntaxException {
		super(HDFSInfo.getHDFS(uri).open(new Path(uri)), conf,
				firstRecordIndex, numRecords, reading, writing, true);
		assert reading && !writing;
	}
}
