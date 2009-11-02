package edu.berkeley.gamesman.database;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.database.util.SplitDatabaseWritable;
import edu.berkeley.gamesman.database.util.SplitDatabaseWritableList;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.hadoop.util.HadoopUtil;

import java.util.List;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * HadoopSplitDatabase contains a NavigableMap of read databases, sorted by
 * a range of hash values, so that it is possible to read from a previous
 * tier without combining all the mini-databases.
 * 
 * For writing databases, it uses beginWrite() and endWrite() in order to
 * create a mini-database only used for one thread of the solve.
 * 
 * @author Steven Schlansker
 */
public class HadoopSplitDatabase extends HadoopUtil.MapReduceDatabase {

	/** Default constructor. Must by followed by calls to setFilesystem(),
	 * setOutputDirectory(), setDelegate(), and initialize().
	 */
	public HadoopSplitDatabase() {
	}

	/**
	 * Equivalent to default constructor, followed by setFilesystem().
	 * @param fs FileSystem, if known already
	 */
	public HadoopSplitDatabase(FileSystem fs) {
		super(fs);
	}

	protected Database createReadDatabase() {
		return new HDFSSolidDatabase(fs);
	}
	
	protected SolidDatabase createWriteDatabase() {
		SolidDatabase db = new HDFSOutputDatabase(fs);
		db.storeConfiguration = false;
		return db;
	}

	protected List<SplitDatabaseWritable> readIndexFile(String name) {
		DataInputStream fd = null;
		SplitDatabaseWritableList list = null;
		try {
			fd = fs.open(new Path(new Path("file:///"), name));
			list = new SplitDatabaseWritableList();
			list.readFields(fd);
			if (conf == null && list.getConf() != null) {
				conf = list.getConf();
			}
		} catch (EOFException e) {
			// Nothing left in our list of databases, stop the loop.
		} catch (IOException e) {
			Util.fatalError("IOException in loading split database", e);
		} finally {
			if (fd != null) {
				try {
					fd.close();
				} catch (IOException e) {
				}
			}
		}
		return list;
	}

}
