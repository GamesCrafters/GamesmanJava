package edu.berkeley.gamesman.database;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * <p>
 * A Database is the abstract superclass of all data storage methods used in
 * Gamesman. Each particular Database is responsible for the persistent storage
 * of Records derived from solving games.
 * </p>
 * <p>
 * Every database (with the exception of the RemoteDatabase and
 * DatabaseWrappers) should be associated with a file whose first 18 bytes
 * contain special header information telling what sort of first-level
 * compression the database uses and what range of the game is stored in that
 * database. This is followed by the configuration object used to create that
 * database. To implement this, before writing anything to the FileOutputStream,
 * call store(fos, uri) where fos is the outputstream. When reading from the
 * database, make sure to call skipHeader(fis) where fis is the FileInputStream.
 * </p>
 * <p>
 * To read from or write to a database, you'll need a DatabaseHandle for each
 * thread. DatabaseHandles are used by databases to handle threading issues
 * since it's possible many threads will be operating on the database at the
 * same time. When writing to a database, make sure to call closeHandle when
 * you're done.
 * </p>
 * <p>
 * To instantiate a database, use the static methods. They'll find the
 * appropriate child class from the given information and instantiate using that
 * class's constructor. All classes extending Database must have a constructor
 * which takes no additional arguments or the static methods won't be able to
 * create instances of that database. After calling super, make sure to set
 * firstRecord = firstRecord() and numRecords = numRecords() before using them
 * since 0 and -1 respectively indicate the entire game. A null header or
 * configuration means fetch them from the file (uri) and a null uri means fetch
 * it from the configuration. In general, don't depend on the arguments to the
 * constructor, use the database methods to obtain information about how the
 * database was constructed.
 * </p>
 * <p>
 * In general, databases store records in the following way. The game passes the
 * long index and the long record value to the database. The database reads the
 * other records in the same record group. It combines those records with the
 * one passed and then stores the group to the database. totalStates is the
 * number of possible values a record can have. A record group is a
 * base-totalStates number. The number of digits in the record group is
 * recordsPerGroup and the number of bytes required to store that record group
 * is recordGroupByteLength.
 * </p>
 * <p>
 * While the above information is important for understanding how databases
 * work, you generally should not have to deal with that. Nothing dealing with
 * bytes or record groups is visible outside the database package or Database
 * classes. Furthermore, the superclass Database handles all the calculations
 * dealing with converting between records and bytes. If you want to extend
 * Database, you must override the getBytes and putBytes methods (which are
 * fairly straightforward). For slightly more control, you may have those
 * methods throw UnsupportedOperation in which case you'll override the other
 * getBytes and putBytes and prepareRange. In this case, to deal with all the
 * nasty edge cases, do the following:<br/>
 * <code>
 * protected void prepareRange(DatabaseHandle dh, long byteIndex, int firstNum, long numBytes, int lastNum) {<br />
 * &nbsp;&nbsp;super.prepareRange(dh,byteIndex,firstNum,numBytes,lastNum);<br />
 * &nbsp;&nbsp;//Your code<br />
 * }<br />
 * <br />
 * protected int getBytes(final DatabaseHandle dh, final byte[] arr, int off,<br />
 * &nbsp;&nbsp;final int maxLen, final boolean overwriteEdgesOk){<br />
 * &nbsp;&nbsp;if(!overwriteEdgesOk){<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;return super.getBytes(dh, arr, off, maxLen, false);<br />
 * &nbsp;&nbsp;}<br />
 * &nbsp;&nbsp;final int numBytes = Math.min(maxLen,dh.lastByteIndex-dh.location);<br />
 * &nbsp;&nbsp;//Your code, numBytes is the number of bytes to read.<br />
 * &nbsp;&nbsp;dh.location+=numBytes;<br />
 * &nbsp;&nbsp;return numBytes;<br />
 * }<br />
 * <br />
 * protected int putBytes(final DatabaseHandle dh, final byte[] arr, int off,<br />
 * &nbsp;&nbsp;final int maxLen, final boolean edgesAreCorrect){<br />
 * &nbsp;&nbsp;if(!edgesAreCorrect){<br />
 * &nbsp;&nbsp;&nbsp;&nbsp;return super.putBytes(dh, arr, off, maxLen, false);<br />
 * &nbsp;&nbsp;}<br />
 * &nbsp;&nbsp;final int numBytes = Math.min(maxLen,dh.lastByteIndex-dh.location);<br />
 * &nbsp;&nbsp;//Your code, numBytes is the number of bytes to read.<br />
 * &nbsp;&nbsp;dh.location+=numBytes;<br />
 * &nbsp;&nbsp;return numBytes;<br />
 * }</code><br />
 * When overwriteEdgesOk/edgesAreCorrect is false, the Database versions of
 * these methods split the request into chunks in which these variables are true
 * and then calls getBytes and putBytes with them set to true. So by writing
 * your methods in this way, you're designating that when the edges are not
 * already handled, the Database class will handle it and when they are handled,
 * you'll deal with actually fetching the bytes. You can safely assume that when
 * prepareRange is called, getBytes/putBytes will eventually be called over the
 * entire range prepared. Furthermore, closeHandle will eventually be called on
 * all handles used for writing (but may or may not be called on handles used
 * for reading)
 * </p>
 * <p>
 * To obtain the actual records requested (in prepareRange), use:<br />
 * <code>
 * long firstRecord = toFirstRecord(byteIndex)+firstNum;<br />
 * long numRecords = toLastRecord(byteIndex + numBytes) - (lastNum == 0 ? 0 : (recordsPerGroup - lastNum)) - firstRecord;
 * </code><br />
 * You may subclass DatabaseHandle in order to store this information or add
 * anything else you need. You may even choose to subclass DatabaseHandle
 * without overriding the lower-level versions of getBytes/putBytes (but you'll
 * probably still want to override prepareRange in that case). The argument to
 * the DatabaseHandle super constructor should always be recordGroupByteLength
 * unless you choose to handle record group edge cases yourself (this is not
 * recommended) in which case it should be null.
 * </p>
 * 
 * @author dnspies
 */
public abstract class Database {

	protected final Configuration conf;

	protected final boolean solve;

	private final long numRecords;

	protected final long numContainedRecords;

	private final long firstRecord;

	protected final long firstContainedRecord;

	protected final long totalStates;

	protected final BigInteger bigIntTotalStates;

	protected final BigInteger[] multipliers;

	protected final long[] longMultipliers;

	protected final int recordsPerGroup;

	protected final int recordGroupByteLength;

	protected final int recordGroupByteBits;

	protected final boolean superCompress;

	protected final boolean recordGroupUsesLong;

	/**
	 * All databases should override this constructor without adding any
	 * additional parameters. In general, your constructor should not use the
	 * arguments provided, use the database methods and visible variables to
	 * obtain that information
	 */
	protected Database(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords, DatabaseHeader header) {
		byte[] dbInfo = null;
		if (header == null || conf == null)
			if (!solve)
				try {
					FileInputStream fis = new FileInputStream(uri);
					dbInfo = new byte[18];
					fis.read(dbInfo);
					if (conf == null)
						conf = Configuration.load(fis);
					fis.close();
					if (header == null)
						header = new DatabaseHeader(dbInfo);
				} catch (ClassNotFoundException e) {
					throw new Error(e);
				} catch (IOException e) {
					throw new Error(e);
				}
		if (numRecords == -1) {
			if (header != null && header.numRecords != -1) {
				firstRecord = header.firstRecord;
				numRecords = header.numRecords;
			} else {
				firstRecord = 0;
				numRecords = conf.getGame().numHashes();
			}
		}
		this.conf = conf;
		this.solve = solve;
		totalStates = conf.getGame().recordStates();
		bigIntTotalStates = BigInteger.valueOf(totalStates);
		if (header != null) {
			recordGroupByteBits = header.recordGroupByteBits;
			recordGroupByteLength = header.recordGroupByteLength;
			recordsPerGroup = header.recordsPerGroup;
			superCompress = header.superCompress;
			firstContainedRecord = header.firstRecord;
			this.firstRecord = Math.max(firstRecord, firstContainedRecord);
			numContainedRecords = header.numRecords;
			this.numRecords = Math.max(
					Math.min(firstRecord + numRecords, firstContainedRecord
							+ numContainedRecords)
							- this.firstRecord, 0);
		} else {
			double requiredCompression = Double.parseDouble(conf.getProperty(
					"record.compression", "0")) / 100;
			header = new DatabaseHeader(requiredCompression, totalStates,
					firstRecord, numRecords);
			superCompress = header.superCompress;
			recordsPerGroup = header.recordsPerGroup;
			recordGroupByteLength = header.recordGroupByteLength;
			recordGroupByteBits = header.recordGroupByteBits;
			firstContainedRecord = this.firstRecord = firstRecord;
			numContainedRecords = this.numRecords = numRecords;
		}
		if (recordGroupByteLength < 8) {
			recordGroupUsesLong = true;
			multipliers = null;
			longMultipliers = new long[recordsPerGroup + 1];
			long longMultiplier = 1;
			for (int i = 0; i <= recordsPerGroup; i++) {
				longMultipliers[i] = longMultiplier;
				longMultiplier *= totalStates;
			}
		} else {
			recordGroupUsesLong = false;
			longMultipliers = null;
			multipliers = new BigInteger[recordsPerGroup + 1];
			BigInteger multiplier = BigInteger.ONE;
			for (int i = 0; i <= recordsPerGroup; i++) {
				multipliers[i] = multiplier;
				multiplier = multiplier.multiply(bigIntTotalStates);
			}
		}
		assert Util.debug(DebugFacility.DATABASE, recordsPerGroup
				+ " records per group\n" + recordGroupByteLength
				+ " bytes per group");
	}

	/**
	 * Ensure that all threads reading from this database have access to the
	 * same information
	 */
	public void flush() {

	}

	/**
	 * Close this Database, flush to disk, and release all associated resources.
	 * This object should not be used again after making this call.
	 */
	public abstract void close();

	/**
	 * Retrieve the Configuration associated with this Database.
	 * 
	 * @return the Configuration stored in the database
	 */
	public final Configuration getConfiguration() {
		return conf;
	}

	/**
	 * Read the Nth Record from the Database as a long. To convert the long to a
	 * Record, use the game's recordFromLong method. Remember, a database only
	 * stores the minimum amount of information necessary. Often for information
	 * from the database to be of any use, you'll need to know the game state as
	 * well.
	 * 
	 * @param dh
	 *            A handle for this database. Handles should not be used
	 *            simultaneously by multiple threads
	 * 
	 * @param recordIndex
	 *            The record number
	 * @return The record as a long
	 */
	public long getRecord(DatabaseHandle dh, long recordIndex) {
		long byteIndex = toByte(recordIndex);
		int num = toNum(recordIndex);
		if (recordGroupUsesLong)
			return getRecord(
					getRecordsAsLongGroup(dh, byteIndex, num, num + 1), num);
		else
			return getRecord(
					getRecordsAsBigIntGroup(dh, byteIndex, num, num + 1), num);
	}

	/**
	 * Stores a particular value at the Nth position in the database. To store a
	 * Record, use the game's getRecord method.
	 * 
	 * @param dh
	 *            A handle for this database
	 * 
	 * @param recordIndex
	 *            The record number
	 * @param r
	 *            The Record value to store
	 */
	public void putRecord(DatabaseHandle dh, long recordIndex, long r) {
		long byteIndex = toByte(recordIndex);
		int num = toNum(recordIndex);
		if (recordGroupUsesLong)
			putRecordsAsGroup(dh, byteIndex, num, num + 1,
					setRecord(0L, num, r));
		else
			putRecordsAsGroup(dh, byteIndex, num, num + 1,
					setRecord(BigInteger.ZERO, num, r));
	}

	/**
	 * Returns a record group as a long
	 * 
	 * @param loc
	 *            The index of the byte the group begins on
	 * @return The group beginning at byte-index loc
	 */
	protected long getLongRecordGroup(DatabaseHandle dh, long loc) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		getBytes(dh, loc, groupBytes, 0, recordGroupByteLength);
		long v = longRecordGroup(groupBytes, 0);
		dh.releaseBytes(groupBytes);
		return v;
	}

	/**
	 * Returns a record group as a BigInteger
	 * 
	 * @param loc
	 *            The index of the byte the group begins on
	 * @return The group beginning at loc
	 */
	protected BigInteger getBigIntRecordGroup(DatabaseHandle dh, long loc) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		getBytes(dh, loc, groupBytes, 0, recordGroupByteLength);
		BigInteger v = bigIntRecordGroup(groupBytes, 0);
		dh.releaseBytes(groupBytes);
		return v;
	}

	/**
	 * Places a record group as a long in the database
	 * 
	 * @param loc
	 *            The index of the byte the group begins on
	 * @param rg
	 *            The record group to store
	 */
	protected void putRecordGroup(DatabaseHandle dh, long loc, long rg) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		toUnsignedByteArray(rg, groupBytes, 0);
		putBytes(dh, loc, groupBytes, 0, recordGroupByteLength);
		dh.releaseBytes(groupBytes);
	}

	/**
	 * Places a record group as a BigInteger in the database
	 * 
	 * @param loc
	 *            The index of the byte the group begins on
	 * @param rg
	 *            The record group to store
	 */
	protected void putRecordGroup(DatabaseHandle dh, long loc, BigInteger rg) {
		byte[] groups = dh.getRecordGroupBytes();
		toUnsignedByteArray(rg, groups, 0);
		putBytes(dh, loc, groups, 0, recordGroupByteLength);
		dh.releaseBytes(groups);
	}

	/**
	 * Calls prepareRange immediately followed by putBytes on that range.
	 * 
	 * @param dh
	 *            A database handle for this thread.
	 * @param byteIndex
	 *            The index at which to begin writing bytes (should be
	 *            group-aligned if recordNum!=0)
	 * @param recordNum
	 *            The number of records to skip in the first group
	 * @param arr
	 *            An array containing the bytes to write
	 * @param off
	 *            The offset into the array to begin reading
	 * @param numBytes
	 *            The number of bytes to write from the array
	 *            (byteIndex+numBytes should be group-aligned if lastNum!=0)
	 * @param lastNum
	 *            The number of records to use from the last group or all of
	 *            them if lastNum=0
	 * @param edgesAreCorrect
	 *            true if the database does not need to be careful not to write
	 *            the edges (firstNum, lastNum) into the database (sorry about
	 *            the double negative)
	 */
	protected void putRecordsAsBytes(DatabaseHandle dh, long byteIndex,
			int recordNum, byte[] arr, int off, int numBytes, int lastNum,
			boolean edgesAreCorrect) {
		prepareRange(dh, byteIndex, recordNum, numBytes, lastNum);
		putBytes(dh, arr, off, numBytes, edgesAreCorrect);
	}

	/**
	 * Puts records from a single group into the database. This is equivalent to
	 * putRecordsAsBytes with numBytes = recordGroupByteLength and using the
	 * least significant recordGroupByteLength bytes of rg instead of arr
	 * 
	 * @param dh
	 *            A database handle for this thread.
	 * @param byteIndex
	 *            The index at which to begin writing bytes (should be
	 *            group-aligned)
	 * @param firstNum
	 *            The number of records to skip in the first group
	 * @param lastNum
	 *            The number of records to use from the last group. Note that
	 *            unlike putRecordsAsBytes, this value should be equal to
	 *            recordsPerGroup where appropriate rather than zero
	 * @param rg
	 *            The record group to write bytes from
	 */
	protected void putRecordsAsGroup(DatabaseHandle dh, long byteIndex,
			int firstNum, int lastNum, long rg) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		toUnsignedByteArray(rg, groupBytes, 0);
		putRecordsAsBytes(dh, byteIndex, firstNum, groupBytes, 0,
				recordGroupByteLength,
				lastNum == recordsPerGroup ? 0 : lastNum, false);
		dh.releaseBytes(groupBytes);
	}

	/**
	 * Puts records from a single group into the database. This is equivalent to
	 * putRecordsAsBytes with numBytes = recordGroupByteLength and using the
	 * least significant recordGroupByteLength bytes of rg instead of arr
	 * 
	 * @param dh
	 *            A database handle for this thread.
	 * @param byteIndex
	 *            The index at which to begin writing bytes (should be
	 *            group-aligned)
	 * @param firstNum
	 *            The number of records to skip in the group
	 * @param lastNum
	 *            The record-digit to stop using from the group. Note that
	 *            unlike putRecordsAsBytes, this value should be equal to
	 *            recordsPerGroup where appropriate rather than zero
	 * @param rg
	 *            The record group to write bytes from
	 */
	protected void putRecordsAsGroup(DatabaseHandle dh, long byteIndex,
			int firstNum, int lastNum, BigInteger rg) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		toUnsignedByteArray(rg, groupBytes, 0);
		putRecordsAsBytes(dh, byteIndex, firstNum, groupBytes, 0,
				recordGroupByteLength,
				lastNum == recordsPerGroup ? 0 : lastNum, false);
		dh.releaseBytes(groupBytes);
	}

	/**
	 * Combines two record groups into one by taking all the records before num
	 * from one of them and all the records after num for the other. For
	 * instance, if totalStates = 10 then splice(54678,1491,3) returns 54491.
	 * 
	 * @param group1
	 *            The first group
	 * @param group2
	 *            The second group
	 * @param num
	 *            The cut-off point
	 * @return The group that results from combining them.
	 */
	protected final long splice(long group1, long group2, int num) {
		if (superCompress)
			return group1 % longMultipliers[num]
					+ (group2 - group2 % longMultipliers[num]);
		else if (num == 0)
			return group2;
		else
			return group1;
	}

	/**
	 * Combines two record groups into one by taking all the records before num
	 * from one of them and all the records after num for the other. For
	 * instance, if totalStates = 10 then splice(54678,1491,3) returns 54491.
	 * 
	 * @param group1
	 *            The first group
	 * @param group2
	 *            The second group
	 * @param num
	 *            The cut-off point
	 * @return The group that results from combining them.
	 */
	protected final BigInteger splice(BigInteger group1, BigInteger group2,
			int num) {
		if (superCompress)
			return group1.mod(multipliers[num]).add(
					group2.subtract(group2.mod(multipliers[num])));
		else if (num == 0)
			return group2;
		else
			return group1;
	}

	/**
	 * Seek to this location and write len bytes from an array into the
	 * database.
	 * 
	 * @param loc
	 *            The byte-location to seek to
	 * @param arr
	 *            An array to read from
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to write
	 */
	protected abstract void putBytes(DatabaseHandle dh, long loc, byte[] arr,
			int off, int len);

	/**
	 * Calls prepareRange immediately followed by getBytes on that range.
	 * 
	 * @param dh
	 *            A database handle for this thread.
	 * @param byteIndex
	 *            The index at which to begin reading bytes (should be
	 *            group-aligned if recordNum!=0)
	 * @param recordNum
	 *            The number of records to skip in the first group
	 * @param arr
	 *            An array containing the bytes to read
	 * @param off
	 *            The offset into the array to begin reading
	 * @param numBytes
	 *            The number of bytes to read into the array (byteIndex+numBytes
	 *            should be group-aligned if lastNum!=0)
	 * @param lastNum
	 *            The number of records to use from the last group or all of
	 *            them if lastNum=0
	 * @param overwriteEdgesOk
	 *            true if the database does not need to be careful not to read
	 *            the edges (firstNum, lastNum) into the array (sorry about the
	 *            double negative)
	 */
	protected void getRecordsAsBytes(DatabaseHandle dh, long byteIndex,
			int recordNum, byte[] arr, int off, int numBytes, int lastNum,
			boolean overwriteEdgesOk) {
		prepareRange(dh, byteIndex, recordNum, numBytes, lastNum);
		getBytes(dh, arr, off, numBytes, overwriteEdgesOk);
	}

	/**
	 * Reads records from a single group as a long. This is equivalent to
	 * getRecordsAsBytes with numBytes = recordGroupByteLength and using the
	 * least significant recordGroupByteLength bytes of the returned group
	 * instead of arr
	 * 
	 * @param dh
	 *            A database handle for this thread.
	 * @param byteIndex
	 *            The index at which to begin reading bytes (should be
	 *            group-aligned)
	 * @param firstNum
	 *            The number of records to skip in the group
	 * @param lastNum
	 *            The number of record-digit to stop using from the group. Note
	 *            that unlike getRecordsAsBytes, this value should be equal to
	 *            recordsPerGroup where appropriate rather than zero
	 * @return The record group at byteIndex only being careful to make sure
	 *         that records firstNum through lastNum are correct
	 */
	protected long getRecordsAsLongGroup(DatabaseHandle dh, long byteIndex,
			int firstNum, int lastNum) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		getRecordsAsBytes(dh, byteIndex, firstNum, groupBytes, 0,
				recordGroupByteLength,
				lastNum == recordsPerGroup ? 0 : lastNum, true);
		long group = longRecordGroup(groupBytes, 0);
		dh.releaseBytes(groupBytes);
		return group;
	}

	/**
	 * Reads records from a single group as a BigInteger. This is equivalent to
	 * getRecordsAsBytes with numBytes = recordGroupByteLength and using the
	 * least significant recordGroupByteLength bytes of the returned group
	 * instead of arr
	 * 
	 * @param dh
	 *            A database handle for this thread.
	 * @param byteIndex
	 *            The index at which to begin reading bytes (should be
	 *            group-aligned)
	 * @param firstNum
	 *            The number of records to skip in the group
	 * @param lastNum
	 *            The number of record-digit to stop using from the group. Note
	 *            that unlike getRecordsAsBytes, this value should be equal to
	 *            recordsPerGroup where appropriate rather than zero
	 * @return The record group at byteIndex only being careful to make sure
	 *         that records firstNum through lastNum are correct
	 */
	protected BigInteger getRecordsAsBigIntGroup(DatabaseHandle dh,
			long byteIndex, int firstNum, int lastNum) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		getRecordsAsBytes(dh, byteIndex, firstNum, groupBytes, 0,
				recordGroupByteLength,
				lastNum == recordsPerGroup ? 0 : lastNum, true);
		BigInteger group = bigIntRecordGroup(groupBytes, 0);
		dh.releaseBytes(groupBytes);
		return group;
	}

	/**
	 * Seek to this location and read len bytes from the database into an array
	 * 
	 * @param loc
	 *            The location to seek to
	 * @param arr
	 *            The array to write to
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to read
	 */
	protected abstract void getBytes(DatabaseHandle dh, long loc, byte[] arr,
			int off, int len);

	/**
	 * Prepare a given range of records for reading/writing (one or the other,
	 * not interchangeably).
	 * 
	 * @param dh
	 *            A database handle for this thread
	 * @param byteIndex
	 *            The byte index at which to begin reading/writing (should be
	 *            group-aligned if firstNum!=0)
	 * @param firstNum
	 *            The number of records to ignore in the first group
	 * @param numBytes
	 *            The total number of bytes to read/write (byteIndex + numBytes
	 *            should be group-aligned if lastNum!=0)
	 * @param lastNum
	 *            The number of records to care about from the last group or all
	 *            of them if lastNum = 0
	 */
	protected void prepareRange(DatabaseHandle dh, long byteIndex,
			int firstNum, long numBytes, int lastNum) {
		dh.byteIndex = byteIndex;
		dh.firstNum = firstNum;
		dh.lastByteIndex = numBytes + byteIndex;
		dh.lastNum = lastNum;
		dh.location = byteIndex;
	}

	/**
	 * Writes up to maxLen bytes from the array into the database. Call
	 * prepareRange before calling this method.
	 * 
	 * @param dh
	 *            A database handle for this thread
	 * @param arr
	 *            An array to write from
	 * @param off
	 *            The offset into the array
	 * @param maxLen
	 *            The maximum number of bytes to write (will be less if
	 *            prepareRange was only called for some smaller number of bytes)
	 * @param edgesAreCorrect
	 *            Whether the database needs to be careful not to overwrite the
	 *            edges of the prepared range. If edgesAreCorrect is false,
	 *            putBytes will split the call up into multiple calls (as well
	 *            as calls to getBytes) and combine the database's edges with
	 *            the values in the array.
	 */
	protected int putBytes(DatabaseHandle dh, byte[] arr, int off, int maxLen,
			boolean edgesAreCorrect) {
		final int numBytes;
		if (edgesAreCorrect) {
			numBytes = (int) Math.min(dh.lastByteIndex - dh.location, maxLen);
			putBytes(dh, dh.location, arr, off, numBytes);
			dh.location += numBytes;
			return numBytes;
		} else if (!superCompress || (dh.firstNum == 0 && dh.lastNum == 0)) {
			return putBytes(dh, arr, off, maxLen, true);
		}
		numBytes = (int) Math.min(dh.lastByteIndex - dh.location, maxLen);
		if (dh.innerHandle == null)
			dh.innerHandle = getHandle();
		int remainBytes = numBytes;
		final long beforeBytes = dh.location - dh.byteIndex;
		long afterBytes = dh.lastByteIndex - (dh.location + numBytes);
		if (beforeBytes < recordGroupByteLength && dh.firstNum > 0) {
			int iByteNum = (int) beforeBytes;
			if (dh.lastByteIndex - dh.byteIndex == recordGroupByteLength
					&& dh.lastNum > 0) {
				if (dh.firstGroup == null) {
					dh.firstGroup = dh.getRecordGroupBytes();
					getBytes(dh.innerHandle, dh.byteIndex, dh.firstGroup, 0,
							recordGroupByteLength);
					// Although two separate calls to getRecordsAsBytes (for
					// either edge) might look nicer, in practice that would
					// usually be redundant and less efficient
				}
				if (recordGroupUsesLong) {
					long group1;
					long group3;
					group1 = group3 = longRecordGroup(dh.firstGroup, 0);
					long group2 = longRecordGroup(arr, off - iByteNum);
					long resultGroup = splice(group1, group2, dh.firstNum);
					resultGroup = splice(resultGroup, group3, dh.lastNum);
					toUnsignedByteArray(resultGroup, iByteNum, dh.firstGroup,
							iByteNum, numBytes);
				} else {
					BigInteger group1;
					BigInteger group3;
					group1 = group3 = bigIntRecordGroup(dh.firstGroup, 0);
					BigInteger group2 = bigIntRecordGroup(arr, off - iByteNum);
					BigInteger resultGroup = splice(group1, group2, dh.firstNum);
					resultGroup = splice(resultGroup, group3, dh.lastNum);
					toUnsignedByteArray(resultGroup, iByteNum, dh.firstGroup,
							iByteNum, numBytes);
				}
				putBytes(dh, dh.firstGroup, iByteNum, numBytes, true);
				if (dh.location == dh.lastByteIndex) {
					dh.releaseBytes(dh.firstGroup);
					dh.firstGroup = null;
				}
				return numBytes;
			}
			if (dh.firstGroup == null) {
				dh.firstGroup = dh.getRecordGroupBytes();
				getRecordsAsBytes(dh.innerHandle, dh.byteIndex, 0,
						dh.firstGroup, 0, recordGroupByteLength, dh.firstNum,
						true);
			}
			int bytesInGroupStill = Math.min(recordGroupByteLength - iByteNum,
					remainBytes);
			if (recordGroupUsesLong) {
				long group1 = longRecordGroup(dh.firstGroup, 0);
				long group2 = longRecordGroup(arr, off - iByteNum);
				long resultGroup = splice(group1, group2, dh.firstNum);
				toUnsignedByteArray(resultGroup, iByteNum, dh.firstGroup,
						iByteNum, bytesInGroupStill);
			} else {
				BigInteger group1 = bigIntRecordGroup(dh.firstGroup, 0);
				BigInteger group2 = bigIntRecordGroup(arr, off - iByteNum);
				BigInteger resultGroup = splice(group1, group2, dh.firstNum);
				toUnsignedByteArray(resultGroup, iByteNum, dh.firstGroup,
						iByteNum, bytesInGroupStill);
			}
			putBytes(dh, dh.firstGroup, iByteNum, bytesInGroupStill, true);
			if (dh.location == dh.byteIndex + recordGroupByteLength
					|| dh.location == dh.lastByteIndex) {
				dh.releaseBytes(dh.firstGroup);
				dh.firstGroup = null;
			}
			remainBytes -= bytesInGroupStill;
			off += bytesInGroupStill;
		}
		final int middleBytes;
		if (afterBytes < recordGroupByteLength && dh.lastNum > 0)
			middleBytes = remainBytes
					- (recordGroupByteLength - (int) afterBytes);
		else
			middleBytes = remainBytes;
		if (middleBytes > 0) {
			putBytes(dh, arr, off, middleBytes, true);
			remainBytes -= middleBytes;
			off += middleBytes;
		}
		if (remainBytes > 0) {
			if (dh.lastGroup == null) {
				dh.lastGroup = dh.getRecordGroupBytes();
				getRecordsAsBytes(dh.innerHandle, dh.lastByteIndex
						- recordGroupByteLength, dh.lastNum, dh.lastGroup, 0,
						recordGroupByteLength, 0, true);
			}
			int groupByte = recordGroupByteLength
					- (remainBytes + (int) afterBytes);
			if (recordGroupUsesLong) {
				long group1 = longRecordGroup(arr, off - groupByte);
				long group2 = longRecordGroup(dh.lastGroup, 0);
				long resultGroup = splice(group1, group2, dh.lastNum);
				toUnsignedByteArray(resultGroup, groupByte, dh.lastGroup,
						groupByte, remainBytes);
			} else {
				BigInteger group1 = bigIntRecordGroup(arr, off - groupByte);
				BigInteger group2 = bigIntRecordGroup(dh.lastGroup, 0);
				BigInteger resultGroup = splice(group1, group2, dh.lastNum);
				toUnsignedByteArray(resultGroup, groupByte, dh.lastGroup,
						groupByte, remainBytes);
			}
			putBytes(dh, dh.lastGroup, groupByte, remainBytes, true);
			if (afterBytes == 0) {
				dh.releaseBytes(dh.lastGroup);
				dh.lastGroup = null;
			}
		}
		return numBytes;
	}

	/**
	 * Reads up to maxLen bytes from the database into the array. Call
	 * prepareRange before calling this method.
	 * 
	 * @param dh
	 *            A database handle for this thread
	 * @param arr
	 *            An array to write to
	 * @param off
	 *            The offset into the array
	 * @param maxLen
	 *            The maximum number of bytes to read (will be less if
	 *            prepareRange was only called for some smaller number of bytes)
	 * @param overwriteEdgesOk
	 *            Whether the database needs to be careful not to overwrite the
	 *            edges of the prepared range. If overwriteEdgesOk is false,
	 *            putBytes will split the call up into multiple calls and
	 *            combine the array's edges with the values in the database.
	 */
	protected int getBytes(final DatabaseHandle dh, final byte[] arr, int off,
			final int maxLen, final boolean overwriteEdgesOk) {
		final int numBytes;
		if (overwriteEdgesOk) {
			numBytes = (int) Math.min(dh.lastByteIndex - dh.location, maxLen);
			getBytes(dh, dh.location, arr, off, numBytes);
			dh.location += numBytes;
			return numBytes;
		} else if (!superCompress || (dh.firstNum == 0 && dh.lastNum == 0)) {
			return getBytes(dh, arr, off, maxLen, true);
		}
		numBytes = (int) Math.min(dh.lastByteIndex - dh.location, maxLen);
		int remainBytes = numBytes;
		long byteLoc = dh.location - dh.byteIndex;
		long afterBytes = dh.lastByteIndex - (dh.location + numBytes);
		if (byteLoc < recordGroupByteLength && dh.firstNum > 0) {
			byte[] firstGroup = dh.getRecordGroupBytes();
			int iByteNum = (int) byteLoc;
			if (dh.lastByteIndex - dh.byteIndex == recordGroupByteLength
					&& dh.lastNum > 0) {
				getBytes(dh, firstGroup, iByteNum, numBytes, true);
				if (recordGroupUsesLong) {
					long group1;
					long group3;
					group1 = group3 = longRecordGroup(arr, off - iByteNum);
					long group2 = longRecordGroup(firstGroup, 0);
					long resultGroup = splice(group1, group2, dh.firstNum);
					resultGroup = splice(resultGroup, group3, dh.lastNum);
					toUnsignedByteArray(resultGroup, iByteNum, arr, off,
							numBytes);
				} else {
					BigInteger group1;
					BigInteger group3;
					group1 = group3 = bigIntRecordGroup(arr, off - iByteNum);
					BigInteger group2 = bigIntRecordGroup(firstGroup, 0);
					BigInteger resultGroup = splice(group1, group2, dh.firstNum);
					resultGroup = splice(resultGroup, group3, dh.lastNum);
					toUnsignedByteArray(resultGroup, iByteNum, arr, off,
							numBytes);
				}
				dh.releaseBytes(firstGroup);
				return numBytes;
			}
			int bytesInGroupStill = Math.min(recordGroupByteLength - iByteNum,
					remainBytes);
			getBytes(dh, firstGroup, iByteNum, bytesInGroupStill, true);
			if (recordGroupUsesLong) {
				long group1 = longRecordGroup(arr, off - iByteNum);
				long group2 = longRecordGroup(firstGroup, 0);
				long resultGroup = splice(group1, group2, dh.firstNum);
				toUnsignedByteArray(resultGroup, iByteNum, arr, off,
						bytesInGroupStill);
			} else {
				BigInteger group1 = bigIntRecordGroup(arr, off - iByteNum);
				BigInteger group2 = bigIntRecordGroup(firstGroup, 0);
				BigInteger resultGroup = splice(group1, group2, dh.firstNum);
				toUnsignedByteArray(resultGroup, iByteNum, arr, off,
						bytesInGroupStill);
			}
			dh.releaseBytes(firstGroup);
			byteLoc += bytesInGroupStill;
			remainBytes -= bytesInGroupStill;
			off += bytesInGroupStill;
		}
		final int middleBytes;
		if (afterBytes < recordGroupByteLength && dh.lastNum > 0)
			middleBytes = remainBytes
					- (recordGroupByteLength - (int) afterBytes);
		else
			middleBytes = remainBytes;
		if (middleBytes > 0) {
			getBytes(dh, arr, off, middleBytes, true);
			remainBytes -= middleBytes;
			off += middleBytes;
			byteLoc += middleBytes;
		}
		if (remainBytes > 0) {
			byte[] lastGroup = dh.getRecordGroupBytes();
			int groupByte = recordGroupByteLength
					- (remainBytes + (int) afterBytes);
			getBytes(dh, lastGroup, groupByte, remainBytes, true);
			if (recordGroupUsesLong) {
				long group1 = longRecordGroup(lastGroup, 0);
				long group2 = longRecordGroup(arr, off - groupByte);
				long resultGroup = splice(group1, group2, dh.lastNum);
				toUnsignedByteArray(resultGroup, groupByte, arr, off,
						remainBytes);
			} else {
				BigInteger group1 = bigIntRecordGroup(lastGroup, 0);
				BigInteger group2 = bigIntRecordGroup(arr, off - groupByte);
				BigInteger resultGroup = splice(group1, group2, dh.lastNum);
				toUnsignedByteArray(resultGroup, groupByte, arr, off,
						remainBytes);
			}
			dh.releaseBytes(lastGroup);
		}
		return numBytes;
	}

	/**
	 * Fills a portion of the database with the passed record.
	 * 
	 * @param r
	 *            The record
	 * @param offset
	 *            The byte offset into the database (should be group-aligned)
	 * @param len
	 *            The number of bytes to fill (should be group-aligned)
	 */
	public void fill(long r, long offset, long len) {
		long[] recs = new long[recordsPerGroup];
		for (int i = 0; i < recordsPerGroup; i++)
			recs[i] = r;
		DatabaseHandle dh = getHandle();
		prepareRange(dh, offset, 0, len, 0);
		int maxBytes = 1024 - 1024 % recordGroupByteLength;
		byte[] groups = new byte[maxBytes];
		while (len > 0) {
			int groupsLength = (int) Math.min(len, maxBytes);
			int numGroups = groupsLength / recordGroupByteLength;
			groupsLength = numGroups * recordGroupByteLength;
			int onByte = 0;
			if (recordGroupUsesLong) {
				long recordGroup = longRecordGroup(recs, 0);
				for (int i = 0; i < numGroups; i++) {
					toUnsignedByteArray(recordGroup, groups, onByte);
					onByte += recordGroupByteLength;
				}
			} else {
				BigInteger recordGroup = bigIntRecordGroup(recs, 0);
				for (int i = 0; i < numGroups; i++) {
					toUnsignedByteArray(recordGroup, groups, onByte);
					onByte += recordGroupByteLength;
				}

			}
			putBytes(dh, groups, 0, groupsLength, true);
			len -= groupsLength;
		}
		closeHandle(dh);
	}

	/**
	 * @return The number of records in this database
	 */
	public long numRecords() {
		return numRecords;
	}

	/**
	 * @return The index of the first record in this database (Will be zero if
	 *         this database stores the entire game)
	 */
	public long firstRecord() {
		return firstRecord;
	}

	/**
	 * Returns whether the record at this index is contained in this database.
	 * Formally:<br />
	 * hash >= firstRecord() && hash < firstRecord() + numRecords();
	 * 
	 * @param hash
	 *            The index of the record
	 * @return Whether this database should contain that record.
	 */
	public final boolean containsRecord(long hash) {
		return hash >= firstRecord() && hash < firstRecord() + numRecords();
	}

	/**
	 * Provides a handle for any thread wishing to access this database. Recycle
	 * handles when possible for faster solve-time. If this handle will be used
	 * for writing to the database, make sure to call closeHandle when you're
	 * done with it.
	 * 
	 * @return A new handle for this database.
	 */
	public DatabaseHandle getHandle() {
		return new DatabaseHandle(recordGroupByteLength);
	}

	/**
	 * Closes a database handle used for writing. It is not necessary to call
	 * this method on handles which were only used for reading. Do not use the
	 * handle again after calling this method.
	 * 
	 * @param dh
	 *            The handle.
	 */
	public void closeHandle(DatabaseHandle dh) {
	}

	/**
	 * Given a record index, returns the byte index of the group in which it is
	 * stored
	 * 
	 * @param recordIndex
	 *            The record index
	 * @return The byte index
	 */
	protected final long toByte(long recordIndex) {
		if (superCompress)
			if (recordGroupByteLength > 1)
				return recordIndex / recordsPerGroup * recordGroupByteLength;
			else
				return recordIndex / recordsPerGroup;
		else
			return recordIndex << recordGroupByteBits;
	}

	/**
	 * Given a byte index, returns the first record index of the group which
	 * contains that byte index
	 * 
	 * @param byteIndex
	 *            The byte index
	 * @return The record index
	 */
	protected final long toFirstRecord(long byteIndex) {
		if (superCompress)
			if (recordGroupByteLength > 1)
				return byteIndex / recordGroupByteLength * recordsPerGroup;
			else
				return byteIndex * recordsPerGroup;
		else
			return byteIndex >> recordGroupByteBits;
	}

	/**
	 * Given a byte index, returns the first record index of the next group
	 * after the previous byte index. DOES NOT RETURN THE LAST RECORD INDEX OF A
	 * GROUP (although the method name might lead some to think so)! More
	 * formally returns:<br />
	 * toFirstRecord(byteIndex + recordGroupByteLength - 1);
	 * 
	 * @param byteIndex
	 *            The byteIndex
	 * @return The recordIndex
	 */
	protected long toLastRecord(long byteIndex) {
		return toFirstRecord(byteIndex + recordGroupByteLength - 1);
	}

	/**
	 * Returns the "digit" within a group at which a particular recordIndex is
	 * stored. More formally:<br />
	 * recordIndex % recordsPerGroup
	 * 
	 * @param recordIndex
	 *            The record index
	 * @return The digit (or num) of the record
	 */
	protected final int toNum(long recordIndex) {
		if (superCompress)
			return (int) (recordIndex % recordsPerGroup);
		else
			return 0;
	}

	/**
	 * Given a record index, returns the first byte index of the next group
	 * after the previous record. DOES NOT RETURN THE LAST BYTE INDEX OF A GROUP
	 * (although the method name might lead some to think so)! More formally:<br />
	 * toByte(lastRecord + recordsPerGroup - 1)
	 * 
	 * @param lastRecord
	 *            The record index
	 * @return The byte index
	 */
	protected final long lastByte(long lastRecord) {
		return toByte(lastRecord + recordsPerGroup - 1);
	}

	/**
	 * Returns the total number of bytes required for storing the given range of
	 * records. More formally:<br />
	 * lastByte(firstRecord + numRecords) - toByte(firstRecord)
	 * 
	 * @param firstRecord
	 *            The first record of the range
	 * @param numRecords
	 *            The number of records in the range
	 * @return The number of bytes required to store those records
	 */
	protected final long numBytes(long firstRecord, long numRecords) {
		return lastByte(firstRecord + numRecords) - toByte(firstRecord);
	}

	/**
	 * Creates a record group from an array of bytes (reads
	 * recordGroupByteLength bytes into a long)
	 * 
	 * @param values
	 *            The array of bytes
	 * @param offset
	 *            The offset into the array
	 * @return The record group
	 */
	protected final long longRecordGroup(byte[] values, int offset) {
		long longValues = 0;
		for (int i = 0; i < recordGroupByteLength; i++) {
			longValues <<= 8;
			if (offset >= 0 && offset < values.length)
				longValues |= (values[offset++] & 255L);
		}
		return longValues;
	}

	/**
	 * Creates a record group from an array of bytes (reads
	 * recordGroupByteLength bytes into a BigInteger)
	 * 
	 * @param values
	 *            The array of bytes
	 * @param offset
	 *            The offset into the array
	 * @return The record group
	 */
	protected final BigInteger bigIntRecordGroup(byte[] values, int offset) {
		byte[] bigIntByte = new byte[recordGroupByteLength];
		for (int i = 0; i < recordGroupByteLength; i++) {
			if (offset >= 0 && offset < values.length)
				bigIntByte[i] = values[offset++];
			else
				bigIntByte[i] = 0;
		}
		return new BigInteger(1, bigIntByte);
	}

	/**
	 * Creates a record group from an array of records
	 * 
	 * @param recs
	 *            An array of records
	 * @param offset
	 *            The first record to read from in the array
	 * @return A record group from the next recordsPerGroup records
	 */
	protected final long longRecordGroup(long[] recs, int offset) {
		if (superCompress) {
			long longValues = 0;
			for (int i = offset + recordsPerGroup - 1; i >= offset; i--) {
				if (longValues > 0)
					longValues *= totalStates;
				longValues += recs[i];
			}
			return longValues;
		} else
			return recs[0];
	}

	/**
	 * Creates a record group from an array of records
	 * 
	 * @param recs
	 *            An array of records
	 * @param offset
	 *            The first record to read from in the array
	 * @return A record group from the next recordsPerGroup records
	 */
	protected final BigInteger bigIntRecordGroup(long[] recs, int offset) {
		if (superCompress) {
			BigInteger bigIntValues = BigInteger.ZERO;
			for (int i = offset + recordsPerGroup - 1; i >= offset; i--) {
				if (bigIntValues.compareTo(BigInteger.ZERO) > 0)
					bigIntValues = bigIntValues.multiply(bigIntTotalStates);
				bigIntValues = bigIntValues.add(BigInteger.valueOf(recs[i]));
			}
			return bigIntValues;
		} else
			return BigInteger.valueOf(recs[0]);
	}

	/**
	 * Stores recordsPerGroup records from a group in an array
	 * 
	 * @param recordGroup
	 *            The group to read from
	 * @param recs
	 *            The array to store in
	 * @param offset
	 *            The offset into the array to begin storing records
	 */
	protected final void getRecords(long recordGroup, long[] recs, int offset) {
		if (superCompress) {
			for (int i = 0; i < recordsPerGroup; i++) {
				long mod = recordGroup % totalStates;
				recordGroup /= totalStates;
				recs[offset++] = mod;
			}
		} else
			recs[0] = recordGroup;
	}

	/**
	 * Stores recordsPerGroup records from a group in an array
	 * 
	 * @param recordGroup
	 *            The group to read from
	 * @param recs
	 *            The array to store in
	 * @param offset
	 *            The offset into the array to begin storing records
	 */
	protected final void getRecords(BigInteger recordGroup, long[] recs,
			int offset) {
		if (superCompress) {
			for (int i = 0; i < recordsPerGroup; i++) {
				BigInteger[] results = recordGroup
						.divideAndRemainder(bigIntTotalStates);
				recordGroup = results[0];
				recs[offset++] = results[1].longValue();
			}
		} else
			recs[0] = recordGroup.longValue();
	}

	/**
	 * Returns this group with record num set to be r
	 * 
	 * @param recordGroup
	 *            The group to alter
	 * @param num
	 *            The digit to change
	 * @param r
	 *            The record to change record num to
	 * @return The altered record group
	 */
	protected final long setRecord(long recordGroup, int num, long r) {
		if (superCompress) {
			if (recordGroup == 0) {
				if (num == 0)
					return r;
				else {
					return r * longMultipliers[num];
				}
			}
			return (num < recordsPerGroup - 1 ? recordGroup - recordGroup
					% longMultipliers[num + 1] : 0)
					+ (num > 0 ? (recordGroup % longMultipliers[num] + r
							* longMultipliers[num]) : r);
		} else
			return r;
	}

	/**
	 * Returns this group with record num set to be r
	 * 
	 * @param recordGroup
	 *            The group to alter
	 * @param num
	 *            The digit to change
	 * @param r
	 *            The record to change record num to
	 * @return The altered record group
	 */
	protected final BigInteger setRecord(BigInteger recordGroup, int num, long r) {
		if (superCompress) {
			if (recordGroup.equals(BigInteger.ZERO)) {
				if (num == 0)
					return BigInteger.valueOf(r);
				else {
					return BigInteger.valueOf(r).multiply(multipliers[num]);
				}
			}
			return (num < recordsPerGroup - 1 ? recordGroup
					.subtract(recordGroup.mod(multipliers[num + 1]))
					: BigInteger.ZERO).add(num > 0 ? (recordGroup
					.mod(multipliers[num]).add(BigInteger.valueOf(r).multiply(
					multipliers[num]))) : BigInteger.valueOf(r));
		} else
			return BigInteger.valueOf(r);
	}

	/**
	 * Returns the nth record in this record group (Starting from least
	 * significant)
	 * 
	 * @param recordGroup
	 *            The group containing the record
	 * @param num
	 *            The digit the record is at
	 * @return The record
	 */
	protected final long getRecord(long recordGroup, int num) {
		if (superCompress) {
			if (num == 0)
				return recordGroup % totalStates;
			else if (num == recordsPerGroup - 1)
				return recordGroup / longMultipliers[num];
			else
				return recordGroup / longMultipliers[num] % totalStates;
		} else
			return recordGroup;
	}

	/**
	 * Returns the nth record in this record group (Starting from least
	 * significant)
	 * 
	 * @param recordGroup
	 *            The group containing the record
	 * @param num
	 *            The digit the record is at
	 * @return The record
	 */
	protected final long getRecord(BigInteger recordGroup, int num) {
		if (superCompress)
			if (num == 0)
				return recordGroup.mod(bigIntTotalStates).longValue();
			else if (num == recordsPerGroup - 1)
				return recordGroup.divide(multipliers[num]).longValue();
			else
				return recordGroup.divide(multipliers[num])
						.mod(bigIntTotalStates).longValue();
		else
			return recordGroup.longValue();
	}

	/**
	 * Stores a record group in a byte array (len = recordGroupByteLength)
	 * 
	 * @param recordGroup
	 *            The record group to store
	 * @param byteArray
	 *            The array of bytes to store in
	 * @param offset
	 *            The offset to start at
	 */
	protected final void toUnsignedByteArray(long recordGroup,
			byte[] byteArray, int offset) {
		toUnsignedByteArray(recordGroup, 0, byteArray, offset,
				recordGroupByteLength);
	}

	/**
	 * Stores part of a record group in a byte array
	 * 
	 * @param recordGroup
	 *            The record group to store
	 * @param byteNum
	 *            The first byte of the group to store
	 * @param byteArray
	 *            The array of bytes to store in
	 * @param offset
	 *            The offset to start at
	 * @param numBytes
	 *            The number of bytes from the group to store
	 */
	private final void toUnsignedByteArray(long recordGroup, int byteNum,
			byte[] byteArray, int offset, int numBytes) {
		int stopAt = offset + byteNum;
		int startAt = stopAt + numBytes;
		for (int i = offset + recordGroupByteLength - 1; i >= startAt; i--) {
			recordGroup >>>= 8;
		}
		for (int i = startAt - 1; i >= stopAt; i--) {
			byteArray[i] = (byte) recordGroup;
			recordGroup >>>= 8;
		}
	}

	/**
	 * Stores a record group in a byte array (len = recordGroupByteLength)
	 * 
	 * @param recordGroup
	 *            The record group to store
	 * @param byteArray
	 *            The array of bytes to store in
	 * @param offset
	 *            The offset to start at
	 */
	protected final void toUnsignedByteArray(BigInteger recordGroup,
			byte[] byteArray, int offset) {
		toUnsignedByteArray(recordGroup, 0, byteArray, offset,
				recordGroupByteLength);
	}

	/**
	 * Stores part of a record group in a byte array
	 * 
	 * @param recordGroup
	 *            The record group to store
	 * @param byteOff
	 *            The first byte of the group to store
	 * @param byteArray
	 *            The array of bytes to store in
	 * @param offset
	 *            The offset to start at
	 * @param numBytes
	 *            The number of bytes from the group to store
	 */
	protected final void toUnsignedByteArray(BigInteger recordGroup,
			int byteOff, byte[] byteArray, int offset, int numBytes) {
		byte[] bigIntArray = recordGroup.toByteArray();
		int initialZeros = recordGroupByteLength - (bigIntArray.length - 1);
		initialZeros -= byteOff;
		for (int i = 0; i < initialZeros; i++) {
			byteArray[offset++] = 0;
		}
		int start = 1;
		if (initialZeros < 0)
			start -= initialZeros;
		else
			numBytes -= initialZeros;
		int last = start + numBytes;
		for (int i = start; i < last; i++) {
			byteArray[offset++] = bigIntArray[i];
		}
	}

	/**
	 * Given a range of records, splits them up into at most numSplits
	 * group-aligned chunks ensuring that the smallest possible chunk size is
	 * minGroup
	 * 
	 * @param firstRecord
	 *            The first record of the range
	 * @param numRecords
	 *            The number of records in the range
	 * @param numSplits
	 *            The desired number of splits (may be less)
	 * @param minGroup
	 *            The minimum allowed chunk size
	 * @return An array of length n+1 starting with firstRecord and each of the
	 *         offsets in between and ending with numRecords
	 */
	public long[] splitRange(long firstRecord, long numRecords, int numSplits,
			int minGroup) {
		return Util.groupAlignedTasks(numSplits, firstRecord, numRecords,
				recordsPerGroup, minGroup);
	}

	/**
	 * @param firstHash
	 *            The first hash of the range
	 * @param numHashes
	 *            The number of hashes in the range
	 * @return The amount of memory required to hold the record range
	 */
	public final long requiredMem(long firstHash, long numHashes) {
		return numBytes(firstHash, numHashes);
	}

	/**
	 * Opens an existing database for reading
	 * 
	 * @param uri
	 *            The file name (or user\@host:path:filename for remote
	 *            databases)
	 * @return The database opened
	 */
	public static Database openDatabase(String uri) {
		return openDatabase(uri, 0, -1);
	}

	/**
	 * Opens a database according to the provided configuration information
	 * 
	 * @param conf
	 *            The configuration object for opening the database
	 * @param solve
	 *            Whether we're solving this database or just reading it
	 * @return The database opened
	 */
	public static Database openDatabase(Configuration conf, boolean solve) {
		return openDatabase(null, conf, solve, null);
	}

	/**
	 * Opens a database for reading the indicated records only (Databases which
	 * do not support this will open as usual)
	 * 
	 * @param uri
	 *            The file name (or user\@host:path:filename for remote
	 *            databases)
	 * @param firstRecord
	 *            The hash of the first record this database will store
	 * @param numRecords
	 *            The number of records this database will store
	 * @return The database opened
	 */
	public static Database openDatabase(String uri, long firstRecord,
			long numRecords) {
		return openDatabase(uri, null, false, firstRecord, numRecords);
	}

	/**
	 * Opens a database of a given type for reading the indicated records only
	 * (Databases which do not support this will open as usual)
	 * 
	 * @param dbType
	 *            The class of the database to be opened
	 * @param uri
	 *            The file name (or user\@host:path:filename for remote
	 *            databases)
	 * @param firstRecord
	 *            The hash of the first record this database will store
	 * @param numRecords
	 *            The number of records this database will store
	 * @return The database opened
	 */
	public static Database openDatabase(String dbType, String uri,
			long firstRecord, long numRecords) {
		return openDatabase(dbType, uri, null, false, firstRecord, numRecords);
	}

	/**
	 * Opens the specified record range in a database
	 * 
	 * @param uri
	 *            The file name (or user\@host:path:filename for remote
	 *            databases)
	 * @param conf
	 *            The configuration object for opening the database
	 * @param solve
	 *            Whether we're solving this database or just reading it
	 * @param firstRecord
	 *            The hash of the first record this database will store
	 * @param numRecords
	 *            The number of records this database will store
	 * @return The database opened
	 */
	public static Database openDatabase(String uri, Configuration conf,
			boolean solve, long firstRecord, long numRecords) {
		return openDatabase(null, uri, conf, solve, firstRecord, numRecords);
	}

	/**
	 * Opens a database pre-providing all the necessary header information so it
	 * doesn't have to be looked up
	 * 
	 * @param uri
	 *            The file name (or user\@host:path:filename for remote
	 *            databases)
	 * @param conf
	 *            The configuration object for opening the database
	 * @param solve
	 *            Whether we're solving this database or just reading it
	 * @param header
	 *            The four "header" data stored in the database header
	 * @return The database opened
	 */
	public static Database openDatabase(String uri, Configuration conf,
			boolean solve, DatabaseHeader header) {
		return openDatabase(null, uri, conf, solve, header);
	}

	/**
	 * Opens a database of a given class pre-providing all the necessary header
	 * information so it doesn't have to be looked up
	 * 
	 * @param dbType
	 *            The class of the database to be opened
	 * @param uri
	 *            The file name (or user\@host:path:filename for remote
	 *            databases)
	 * @param conf
	 *            The configuration object for opening the database
	 * @param solve
	 *            Whether we're solving this database or just reading it
	 * @param header
	 *            The four "header" data stored in the database header
	 * @return The database opened
	 */
	public static Database openDatabase(String dbType, String uri,
			Configuration conf, boolean solve, DatabaseHeader header) {
		return openDatabase(dbType, uri, conf, solve, 0, -1, header);
	}

	/**
	 * Opens a given range of records from a database of a given class.
	 * Databases which do not support this will open as usual.
	 * 
	 * @param dbType
	 *            The class of the database to be opened
	 * @param uri
	 *            The file name (or user\@host:path:filename for remote
	 *            databases)
	 * @param conf
	 *            The configuration object for opening the database
	 * @param solve
	 *            Whether we're solving this database or just reading it
	 * @param firstRecord
	 *            The hash of the first record this database will store
	 * @param numRecords
	 *            The number of records this database will store
	 * @return The database opened
	 */
	public static Database openDatabase(String dbType, String uri,
			Configuration conf, boolean solve, long firstRecord, long numRecords) {
		return openDatabase(dbType, uri, conf, solve, firstRecord, numRecords,
				null);
	}

	/**
	 * @param dbType
	 *            The class of the database to be opened
	 * @param uri
	 *            The file name (or user\@host:path:filename for remote
	 *            databases)
	 * @param conf
	 *            The configuration object for opening the database
	 * @param solve
	 *            Whether we're solving this database or just reading it
	 * @param firstRecord
	 *            The hash of the first record this database will store
	 * @param numRecords
	 *            The number of records this database will store
	 * @param header
	 *            The four "header" data stored in the database header
	 * @return The database opened
	 */
	private static Database openDatabase(String dbType, String uri,
			Configuration conf, boolean solve, long firstRecord,
			long numRecords, DatabaseHeader header) {
		String[] dbClasses;
		if (uri == null)
			uri = conf.getProperty("gamesman.db.uri");
		if (uri.contains(":")) {
			String[] hostFile = uri.split(":");
			String host = hostFile[0];
			String path = hostFile[1];
			String file = hostFile[2];
			if (!file.startsWith("/") && !file.startsWith(path))
				file = path + "/" + file;
			String user = null;
			if (host.contains("@")) {
				String[] userHost = host.split("@");
				user = userHost[0];
				host = userHost[1];
			}
			if (conf == null) {
				Pair<DatabaseHeader, Configuration> p = RemoteDatabase
						.remoteHeaderConf(user, host, file);
				if (header == null)
					header = p.car;
				conf = p.cdr;
			} else if (header == null) {
				header = RemoteDatabase.remoteHeader(user, host, file);
			}
			if (dbType == null)
				dbType = conf.getProperty("gamesman.database");
			dbClasses = dbType.split(":");
			dbClasses[dbClasses.length - 1] = RemoteDatabase.class.getName();
			try {
				conf.db = new RemoteDatabase(uri, conf, solve, firstRecord,
						numRecords, header, user, host, path, file);
				for (int i = dbClasses.length - 2; i >= 0; i--) {
					Class<? extends DatabaseWrapper> wrapperClass = Class
							.forName(dbClasses[i]).asSubclass(
									DatabaseWrapper.class);
					conf.db = wrapperClass.getConstructor(Database.class,
							String.class, Configuration.class, Boolean.TYPE,
							Long.TYPE, Long.TYPE).newInstance(conf.db, uri,
							conf, solve, firstRecord, numRecords);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			if (conf == null) {
				try {
					FileInputStream fis = new FileInputStream(uri);
					skipFully(fis, 18);
					conf = Configuration.load(fis);
					fis.close();
				} catch (ClassNotFoundException e) {
					throw new Error(e);
				} catch (IOException e) {
					throw new Error(e);
				}
			}
			if (dbType == null)
				dbType = conf.getProperty("gamesman.database");
			dbClasses = dbType.split(":");
			String packageName = null;
			for (int i = 0; i < dbClasses.length; i++) {
				if (!dbClasses[i].startsWith("edu.berkeley.gamesman")) {
					if (packageName == null)
						packageName = Database.class.getPackage().getName()
								+ ".";
					dbClasses[i] = packageName + dbClasses[i];
				}
			}
			try {
				Class<? extends Database> dbClass = Class.forName(
						dbClasses[dbClasses.length - 1]).asSubclass(
						Database.class);
				conf.db = dbClass.getConstructor(String.class,
						Configuration.class, Boolean.TYPE, Long.TYPE,
						Long.TYPE, DatabaseHeader.class).newInstance(uri, conf,
						solve, firstRecord, numRecords, header);
				for (int i = dbClasses.length - 2; i >= 0; i--) {
					Class<? extends DatabaseWrapper> wrapperClass = Class
							.forName(dbClasses[i]).asSubclass(
									DatabaseWrapper.class);
					long lieNumRecords = numRecords;
					if (dbClasses[i] == TierCutDatabase.class.getName()) {
						lieNumRecords = TierCutDatabase.getNumRecords(
								firstRecord, numRecords,
								(TierGame) conf.getGame());
					}
					conf.db = wrapperClass.getConstructor(Database.class,
							String.class, Configuration.class, Boolean.TYPE,
							Long.TYPE, Long.TYPE).newInstance(conf.db, uri,
							conf, solve, firstRecord, lieNumRecords);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.getCause().printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return conf.db;
	}

	/**
	 * Skip the indicated number of bytes or skip to the end of the stream
	 * 
	 * @param is
	 *            The input stream
	 * @param len
	 *            The number of bytes
	 * @throws IOException
	 *             If skipping throws an IOException
	 */
	public static void skipFully(InputStream is, long len) throws IOException {
		while (len > 0) {
			long bytesSkipped = is.skip(len);
			if (bytesSkipped < 0)
				break;
			else {
				len -= bytesSkipped;
			}
		}
	}

	/**
	 * Stores the header and configuration in the output stream
	 * 
	 * @param os
	 *            The stream to store in
	 * @param uri
	 *            The name of this database
	 * @throws IOException
	 *             If an IOException is thrown while writing
	 */
	protected final void store(OutputStream os, String uri) throws IOException {
		storeInfo(os);
		conf.store(os, this.getClass().getName(), uri);
	}

	/**
	 * Skip all the header information in the input stream
	 * 
	 * @param is
	 *            The input stream
	 * @throws IOException
	 *             If an IOException is thrown while skipping
	 */
	protected final void skipHeader(InputStream is) throws IOException {
		skipInfo(is);
		Configuration.skipConf(is);
	}

	/**
	 * Stores only the Header object, not the configuration
	 * 
	 * @param os
	 *            The stream to store to
	 * @throws IOException
	 *             If an IOException is thrown while writing
	 */
	protected final void storeInfo(OutputStream os) throws IOException {
		for (int i = 56; i >= 0; i -= 8)
			os.write((int) (firstRecord() >>> i));
		for (int i = 56; i >= 0; i -= 8)
			os.write((int) (numRecords() >>> i));
		if (superCompress) {
			os.write(recordsPerGroup >>> 2);
			os.write(((recordsPerGroup & 3) << 6) | recordGroupByteLength);
		} else {
			os.write(-1);
			os.write(recordGroupByteBits);
		}
	}

	/**
	 * Skips only the Header object, not the configuration
	 * 
	 * @param is
	 *            The stream to skip
	 * @throws IOException
	 *             If an IOException is thrown while skipping
	 */
	private void skipInfo(InputStream is) throws IOException {
		byte[] b = new byte[18];
		readFully(is, b, 0, 18);
	}

	/**
	 * Fully read the indicated number of bytes or until reaching the end of the
	 * stream
	 * 
	 * @param is
	 *            The input stream
	 * @param arr
	 *            The array to read into
	 * @param off
	 *            The offset into arr to start at
	 * @param len
	 *            The number of bytes
	 * @throws IOException
	 *             If reading throws an IOException
	 */

	public static final void readFully(InputStream is, byte[] arr, int off,
			int len) throws IOException {
		while (len > 0) {
			int bytesRead = is.read(arr, off, len);
			if (bytesRead < 0) {
				throw new EOFException();
			} else {
				off += bytesRead;
				len -= bytesRead;
			}
		}
	}

	/**
	 * @return The header object for this database
	 */
	public final DatabaseHeader getHeader() {
		if (superCompress)
			return new DatabaseHeader(firstRecord(), numRecords(),
					recordsPerGroup, recordGroupByteLength);
		else
			return new DatabaseHeader(firstRecord(), numRecords(),
					recordGroupByteBits);
	}

	/**
	 * @param dbFirstRecord
	 *            The index of the first record
	 * @param dbNumRecords
	 *            The number of records
	 * @return A copy of the header object for this database with firstRecord
	 *         and numRecords adjusted
	 */
	public final DatabaseHeader getHeader(long dbFirstRecord, long dbNumRecords) {
		if (superCompress)
			return new DatabaseHeader(dbFirstRecord, dbNumRecords,
					recordsPerGroup, recordGroupByteLength);
		else
			return new DatabaseHeader(dbFirstRecord, dbNumRecords,
					recordGroupByteBits);
	}

	/**
	 * @param firstHash
	 *            The hash of the first record in the range
	 * @param availableMemory
	 *            The number of bytes available
	 * @return The number of records which can safely be stored in
	 *         availableMemory bytes
	 */
	public final long recordsForMem(long firstHash, int availableMemory) {
		return toFirstRecord(toByte(firstHash) + availableMemory) - firstHash;
	}

	/**
	 * @return The number of bytes this database takes up on disk (including on
	 *         separate disks or in other files)
	 */
	public abstract long getSize();
}
