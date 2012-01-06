package edu.berkeley.gamesman.parallel.loopytier;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.parallel.RangeFile;

/**
 * @author Eric This class provides a modular way to modify an existing DB in
 *         hadoop.
 */
public class HadoopDBModifier {
	private Random rand = new Random();

	private GZippedFileDatabase readDB = null;
	private GZippedFileDatabase writeDB = null;

	private DatabaseHandle readHandle;
	private DatabaseHandle writeHandle;

	private LocalFileSystem lfs;
	private FileSystem fs;

	private String hdfsDBPathString;

	private Path hdfsDBPath;
	private Path localDBReadPath;
	private Path localDBWritePath;

	private long lastRead = Long.MIN_VALUE; // no record has this value
	private boolean changesObserved = false;

	/**
	 * @param rangeFile
	 *            the range file corresponding to this database
	 * @param lfs
	 *            the local file system to use
	 * @param fs
	 *            the hdfs file system to use
	 * @param conf
	 *            the gamesman configuration to use with the DBs
	 * @throws IOException
	 *             if something goes horribly wrong
	 */
	public HadoopDBModifier(RangeFile rangeFile, LocalFileSystem lfs,
			FileSystem fs, Configuration conf) throws IOException {
		this.lfs = lfs;
		this.fs = fs;

		hdfsDBPathString = rangeFile.myFile.toString();

		String localDBReadPathString = hdfsDBPathString + "_local";
		String localDBWritePathString = localDBReadPathString + "_write";

		hdfsDBPath = new Path(hdfsDBPathString);

		localDBReadPath = new Path(localDBReadPathString);
		localDBWritePath = new Path(localDBWritePathString);

		long numRecords = rangeFile.myRange.numRecords;
		long firstRecord = rangeFile.myRange.firstRecord;

		fs.copyToLocalFile(hdfsDBPath, localDBReadPath);

		readDB = new GZippedFileDatabase(localDBReadPathString, conf,
				firstRecord, numRecords, true, false);

		readHandle = readDB.getHandle(true);
		readDB.prepareReadRange(readHandle, firstRecord, numRecords);

		writeDB = new GZippedFileDatabase(localDBWritePathString, conf,
				firstRecord, numRecords, false, true);

		writeHandle = writeDB.getHandle(false);
		writeDB.prepareWriteRange(writeHandle, firstRecord, numRecords);
	}

	/**
	 * @return the next record read from the database
	 * @throws IOException
	 *             if something goes horribly wrong
	 */
	public long readNextRecord() throws IOException {
		lastRead = readDB.readNextRecord(readHandle);
		return lastRead;
	}

	/**
	 * @param record
	 *            the record to write into the database
	 * @throws IOException
	 *             if something goes horribly wrong
	 */
	public void writeNextRecord(long record) throws IOException {
		writeDB.writeNextRecord(writeHandle, record);
		if (record != lastRead) {
			changesObserved = true;
		}
	}

	/**
	 * Closes both DBs and handles clean up. Will copy the written DB back to
	 * hdfs if changes are made or accesses do not follow the pattern
	 * read->write->read->write etc
	 * 
	 * @throws IOException
	 *             if something goes horribly wrong
	 */
	public void closeAndClean() throws IOException {
		readDB.close();
		lfs.delete(localDBReadPath, true);

		writeDB.close();

		if (changesObserved) {
			Path tempPath = new Path(hdfsDBPathString + "_" + rand.nextLong());
			// use a random long to prevent collisions in the expensive copy
			// step
			fs.moveFromLocalFile(localDBWritePath, tempPath);
			// copy the written database to hdfs
			fs.rename(tempPath, hdfsDBPath);
			// rename so our file system has the same files it did before
		} else {
			lfs.delete(localDBWritePath, true);
		}
	}
}
