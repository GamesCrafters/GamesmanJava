package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * A Database is the abstract superclass of all data storage methods used in
 * Gamesman. Each particular Database is responsible for the persistent storage
 * of Records derived from solving games.
 * 
 * @author Steven Schlansker
 */
public abstract class Database {

	protected Configuration conf;

	protected boolean solve;

	private long numRecords = -1;

	private long firstRecord;

	private long location;

	protected DatabaseHandle myHandle;

	protected long totalStates;

	protected BigInteger bigIntTotalStates;

	protected BigInteger[] multipliers;

	protected long[] longMultipliers;

	protected int recordsPerGroup;

	protected int recordGroupByteLength;

	private int recordGroupByteBits;

	private boolean superCompress;

	protected boolean recordGroupUsesLong;

	/**
	 * Initialize a Database given a URI and a Configuration. This method may
	 * either open an existing database or create a new one. If a new one is
	 * created, the Configuration should be stored. If one is opened, the
	 * Configuration should be checked to ensure it matches that already stored.
	 * This method must be called exactly once before any other methods are
	 * called. The URI must be in the URI syntax, ex:
	 * file:///absolute/path/to/file.db or gdfp://server:port/dbname If config
	 * is null, it will use whatever is in the database. It is recommended that
	 * you pass in the configuration that you are expecting to ensure you don't
	 * load a db for a different game.
	 * 
	 * Note (By Alex Trofimov) I've updated the file Database to accept Relative
	 * URL, so instead of file:/// you can just put the filename, and it will
	 * create a file in the working directory (tested under windows & ubuntu).
	 * 
	 * @param uri
	 *            The URI that the Database is associated with
	 * @param config
	 *            The Configuration that is relevant
	 * @param solve
	 *            true for solving, false for playing
	 */
	public final void initialize(String uri, Configuration config, boolean solve) {
		conf = config;
		this.solve = solve;
		totalStates = conf.getGame().recordStates();
		double requiredCompression = Double.parseDouble(conf.getProperty(
				"record.compression", "0")) / 100;
		double compression;
		if (requiredCompression == 0D) {
			superCompress = false;
			int bits = (int) (Math.log(totalStates) / Math.log(2));
			if ((1 << bits) < totalStates)
				++bits;
			recordGroupByteLength = (bits + 7) >> 3;
			recordGroupByteBits = 0;
			recordGroupByteLength >>= 1;
			while (recordGroupByteLength > 0) {
				recordGroupByteBits++;
				recordGroupByteLength >>= 1;
			}
			recordGroupByteLength = 1 << recordGroupByteBits;
			recordsPerGroup = 1;
		} else {
			superCompress = true;
			int recordGuess;
			int bitLength;
			double log2;
			log2 = Math.log(totalStates) / Math.log(2);
			if (log2 > 8) {
				recordGuess = 1;
				bitLength = (int) Math.ceil(log2);
				compression = (log2 / 8) / ((bitLength + 7) >> 3);
				while (compression < requiredCompression) {
					recordGuess++;
					bitLength = (int) Math.ceil(recordGuess * log2);
					compression = (recordGuess * log2 / 8)
							/ ((bitLength + 7) >> 3);
				}
			} else {
				bitLength = 8;
				recordGuess = (int) (8D / log2);
				compression = recordGuess * log2 / 8;
				while (compression < requiredCompression) {
					bitLength += 8;
					recordGuess = (int) (bitLength / log2);
					compression = (recordGuess * log2 / 8) / (bitLength >> 3);
				}
			}
			recordsPerGroup = recordGuess;
			multipliers = new BigInteger[recordsPerGroup + 1];
			BigInteger multiplier = BigInteger.ONE;
			bigIntTotalStates = BigInteger.valueOf(totalStates);
			for (int i = 0; i <= recordsPerGroup; i++) {
				multipliers[i] = multiplier;
				multiplier = multiplier.multiply(bigIntTotalStates);
			}
			recordGroupByteLength = (bigIntTotalStates.pow(recordsPerGroup)
					.bitLength() + 7) >> 3;
			recordGroupByteBits = -1;
		}
		if (recordGroupByteLength < 8) {
			recordGroupUsesLong = true;
			longMultipliers = new long[recordsPerGroup + 1];
			long longMultiplier = 1;
			for (int i = 0; i <= recordsPerGroup; i++) {
				longMultipliers[i] = longMultiplier;
				longMultiplier *= totalStates;
			}
		} else {
			recordGroupUsesLong = false;
			longMultipliers = null;
		}
		if (numRecords == -1) {
			firstRecord = 0;
			numRecords = conf.getGame().numHashes();
		}
		initialize(uri, solve);
		assert Util.debug(DebugFacility.DATABASE, recordsPerGroup
				+ " records per group\n" + recordGroupByteLength
				+ " bytes per group");

	}

	/**
	 * Initializes as above, but when the confiration is already specified
	 * 
	 * @param uri
	 *            The URI that the Database is associated with
	 * @param solve
	 *            true for solving, false for playing
	 */
	protected abstract void initialize(String uri, boolean solve);

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
	 * Read the Nth Record from the Database as a long
	 * 
	 * @param dh
	 *            A handle for this database.
	 * 
	 * @param recordIndex
	 *            The record number
	 * @return The record as a long
	 */
	public long getRecord(DatabaseHandle dh, long recordIndex) {
		int num = toNum(recordIndex);
		if (recordGroupUsesLong)
			return getRecord(getRecordsAsLongGroup(dh, recordIndex, 1), num);
		else
			return getRecord(getRecordsAsBigIntGroup(dh, recordIndex, 1), num);
	}

	/**
	 * @param dh
	 *            A handle for this database
	 * 
	 * @param recordIndex
	 *            The record number
	 * @param r
	 *            The Record to store
	 */
	public void putRecord(DatabaseHandle dh, long recordIndex, long r) {
		int num = toNum(recordIndex);
		if (recordGroupUsesLong)
			putRecordsAsGroup(dh, recordIndex, 1, setRecord(0L, num, r));
		else
			putRecordsAsGroup(dh, recordIndex, 1, setRecord(BigInteger.ZERO,
					num, r));
	}

	/**
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

	protected void putRecordsAsBytes(DatabaseHandle dh, long recordIndex,
			byte[] arr, int off, int numRecords) {
		long lastRecord = recordIndex + numRecords;
		long lastByte = toByte(lastRecord);
		long byteIndex = toByte(recordIndex);
		int num = toNum(recordIndex);
		if (num > 0) {
			long firstRecord = recordIndex - num;
			byte[] edgeBytes = dh.getRecordGroupBytes();
			getRecordsAsBytes(dh, firstRecord, edgeBytes, 0, num, true);
			if (lastByte == byteIndex) {
				byte[] otherEdge = dh.getRecordGroupBytes();
				int otherNum = num + numRecords;
				getRecordsAsBytes(dh, lastRecord, otherEdge, 0, recordsPerGroup
						- otherNum, true);
				if (recordGroupUsesLong) {
					long group1 = longRecordGroup(edgeBytes, 0);
					long group2 = longRecordGroup(arr, off);
					long group3 = longRecordGroup(otherEdge, 0);
					long resultGroup = splice(group1, group2, num);
					resultGroup = splice(resultGroup, group3, otherNum);
					putRecordGroup(dh, byteIndex, resultGroup);
				} else {
					BigInteger group1 = bigIntRecordGroup(edgeBytes, 0);
					BigInteger group2 = bigIntRecordGroup(arr, off);
					BigInteger group3 = bigIntRecordGroup(otherEdge, 0);
					BigInteger resultGroup = splice(group1, group2, num);
					resultGroup = splice(resultGroup, group3, otherNum);
					putRecordGroup(dh, byteIndex, resultGroup);
				}
				dh.releaseBytes(edgeBytes);
				dh.releaseBytes(otherEdge);
				return;
			}
			if (recordGroupUsesLong) {
				long group1 = longRecordGroup(edgeBytes, 0);
				long group2 = longRecordGroup(arr, off);
				long resultGroup = splice(group1, group2, num);
				putRecordGroup(dh, byteIndex, resultGroup);
			} else {
				BigInteger group1 = bigIntRecordGroup(edgeBytes, off);
				BigInteger group2 = bigIntRecordGroup(arr, off);
				BigInteger resultGroup = splice(group1, group2, num);
				putRecordGroup(dh, byteIndex, resultGroup);
			}
			dh.releaseBytes(edgeBytes);
			recordIndex += recordsPerGroup - num;
			numRecords -= recordsPerGroup - num;
			byteIndex += recordGroupByteLength;
			off += recordGroupByteLength;
		}
		int numBytes = (int) (lastByte - byteIndex);
		putBytes(dh, byteIndex, arr, off, numBytes);
		num = toNum(numRecords);
		if (num > 0) {
			byteIndex = lastByte;
			recordIndex += numRecords - num;
			off += numBytes;
			byte[] edgeBytes = dh.getRecordGroupBytes();
			getRecordsAsBytes(dh, lastRecord, edgeBytes, 0, recordsPerGroup
					- num, true);
			if (recordGroupUsesLong) {
				long group1 = longRecordGroup(arr, off);
				long group2 = longRecordGroup(edgeBytes, 0);
				long resultGroup = splice(group1, group2, num);
				putRecordGroup(dh, byteIndex, resultGroup);
			} else {
				BigInteger group1 = bigIntRecordGroup(arr, off);
				BigInteger group2 = bigIntRecordGroup(edgeBytes, 0);
				BigInteger resultGroup = splice(group1, group2, num);
				putRecordGroup(dh, byteIndex, resultGroup);
			}
			dh.releaseBytes(edgeBytes);
		}
	}

	protected void putRecordsAsGroup(DatabaseHandle dh, long recordIndex,
			int numRecords, long rg) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		toUnsignedByteArray(rg, groupBytes, 0);
		putRecordsAsBytes(dh, recordIndex, groupBytes, 0, numRecords);
		dh.releaseBytes(groupBytes);
	}

	protected void putRecordsAsGroup(DatabaseHandle dh, long recordIndex,
			int numRecords, BigInteger rg) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		toUnsignedByteArray(rg, groupBytes, 0);
		putRecordsAsBytes(dh, recordIndex, groupBytes, 0, numRecords);
		dh.releaseBytes(groupBytes);
	}

	private long splice(long group1, long group2, int num) {
		if (superCompress)
			return group1 % longMultipliers[num]
					+ (group2 - group2 % longMultipliers[num]);
		else if (num == 0)
			return group2;
		else
			return group1;
	}

	private BigInteger splice(BigInteger group1, BigInteger group2, int num) {
		if (superCompress)
			return group1.mod(multipliers[num]).add(
					group2.subtract(group2.mod(multipliers[num])));
		else if (num == 0)
			return group2;
		else
			return group1;
	}

	/**
	 * Seek to this location and write len bytes from an array into the database
	 * 
	 * @param loc
	 *            The location to seek to
	 * @param arr
	 *            An array to read from
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to write
	 */
	protected abstract void putBytes(DatabaseHandle dh, long loc, byte[] arr,
			int off, int len);

	protected void getRecordsAsBytes(DatabaseHandle dh, long recordIndex,
			byte[] arr, int off, int numRecords, boolean overwriteEdgesOk) {
		long byteIndex = toByte(recordIndex);
		if (overwriteEdgesOk) {
			long lastByte = lastByte(recordIndex + numRecords);
			getBytes(dh, byteIndex, arr, off, (int) (lastByte - byteIndex));
			return;
		}
		long lastRecord = recordIndex + numRecords;
		long lastByte = toByte(lastRecord);
		int num = toNum(recordIndex);
		if (num > 0) {
			if (lastByte == byteIndex) {
				int otherNum = num + numRecords;
				if (recordGroupUsesLong) {
					long group1 = longRecordGroup(arr, off);
					long group2 = getLongRecordGroup(dh, byteIndex);
					long group3 = group1;
					long resultGroup = splice(group1, group2, num);
					resultGroup = splice(resultGroup, group3, otherNum);
					toUnsignedByteArray(resultGroup, arr, off);
				} else {
					BigInteger group1 = bigIntRecordGroup(arr, off);
					BigInteger group2 = getBigIntRecordGroup(dh, byteIndex);
					BigInteger group3 = group1;
					BigInteger resultGroup = splice(group1, group2, num);
					resultGroup = splice(resultGroup, group3, otherNum);
					toUnsignedByteArray(resultGroup, arr, off);
				}
				return;
			}
			if (recordGroupUsesLong) {
				long group1 = longRecordGroup(arr, off);
				long group2 = getLongRecordGroup(dh, byteIndex);
				long resultGroup = splice(group1, group2, num);
				toUnsignedByteArray(resultGroup, arr, off);
			} else {
				BigInteger group1 = bigIntRecordGroup(arr, off);
				BigInteger group2 = getBigIntRecordGroup(dh, byteIndex);
				BigInteger resultGroup = splice(group1, group2, num);
				toUnsignedByteArray(resultGroup, arr, off);
			}
			recordIndex += recordsPerGroup - num;
			numRecords -= recordsPerGroup - num;
			byteIndex += recordGroupByteLength;
			off += recordGroupByteLength;
		}
		int numBytes = (int) (lastByte - byteIndex);
		getBytes(dh, byteIndex, arr, off, numBytes);
		num = toNum(numRecords);
		if (num > 0) {
			byteIndex = lastByte;
			recordIndex += numRecords - num;
			off += numBytes;
			if (recordGroupUsesLong) {
				long group1 = getLongRecordGroup(dh, byteIndex);
				long group2 = longRecordGroup(arr, off);
				long resultGroup = splice(group1, group2, num);
				toUnsignedByteArray(resultGroup, arr, off);
			} else {
				BigInteger group1 = getBigIntRecordGroup(dh, byteIndex);
				BigInteger group2 = bigIntRecordGroup(arr, off);
				BigInteger resultGroup = splice(group1, group2, num);
				toUnsignedByteArray(resultGroup, arr, off);
			}
		}
	}

	protected long getRecordsAsLongGroup(DatabaseHandle dh, long recordIndex,
			int numRecords) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		getRecordsAsBytes(dh, recordIndex, groupBytes, 0, numRecords, true);
		long group = longRecordGroup(groupBytes, 0);
		dh.releaseBytes(groupBytes);
		return group;
	}

	protected BigInteger getRecordsAsBigIntGroup(DatabaseHandle dh,
			long recordIndex, int numRecords) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		getRecordsAsBytes(dh, recordIndex, groupBytes, 0, numRecords, true);
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
	 * Seek to this location in the database
	 * 
	 * @param loc
	 *            The location to seek to
	 */
	protected synchronized void seek(long loc) {
		location = loc;
	}

	/**
	 * Writes len bytes from the array into the database
	 * 
	 * @param arr
	 *            An array to read from
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to write
	 */
	protected synchronized void putBytes(byte[] arr, int off, int len) {
		if (myHandle == null)
			myHandle = getHandle();
		putBytes(myHandle, location, arr, off, len);
		location += len;
	}

	/**
	 * Reads len bytes from the database into an array
	 * 
	 * @param arr
	 *            An array to write to
	 * @param off
	 *            The offset into the array
	 * @param len
	 *            The number of bytes to read
	 */
	protected synchronized void getBytes(byte[] arr, int off, int len) {
		if (myHandle == null)
			myHandle = getHandle();
		getBytes(myHandle, location, arr, off, len);
		location += len;
	}

	/**
	 * Fills a portion of the database with the passed record.
	 * 
	 * @param r
	 *            The record
	 * @param offset
	 *            The byte offset into the database
	 * @param len
	 *            The number of bytes to fill
	 */
	public void fill(long r, long offset, long len) {
		long[] recs = new long[recordsPerGroup];
		for (int i = 0; i < recordsPerGroup; i++)
			recs[i] = r;
		seek(offset);
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
			putBytes(groups, 0, groupsLength);
			len -= groupsLength;
		}
	}

	/**
	 * @return The number of bytes used to store all the records (This does not
	 *         include the header size)
	 */
	public long numRecords() {
		return numRecords;
	}

	/**
	 * If this database only covers a particular range of hashes for a game,
	 * call this method before initialize if creating the database
	 * 
	 * @param firstRecord
	 *            The first byte this database contains
	 * @param numRecords
	 *            The total number of bytes contained
	 */
	public void setRange(long firstRecord, long numRecords) {
		this.firstRecord = firstRecord;
		this.numRecords = numRecords;
	}

	/**
	 * @return The index of the first byte in this database (Will be zero if
	 *         this database stores the entire game)
	 */
	public long firstRecord() {
		return firstRecord;
	}

	public DatabaseHandle getHandle() {
		return new DatabaseHandle(recordGroupByteLength);
	}

	public void closeHandle(DatabaseHandle dh) {
	}

	public DatabaseHandle getHandle(long recordStart, long numRecords) {
		return getHandle();
	}

	private final long toByte(long recordIndex) {
		if (superCompress)
			return recordIndex / recordsPerGroup * recordGroupByteLength;
		else
			return recordIndex << recordGroupByteBits;
	}

	private long toFirstRecord(long byteIndex) {
		if (superCompress)
			return byteIndex / recordGroupByteLength * recordsPerGroup;
		else
			return byteIndex >> recordGroupByteBits;
	}

	private final int toNum(long recordIndex) {
		if (superCompress)
			return (int) (recordIndex % recordsPerGroup);
		else
			return 0;
	}

	protected final long lastByte(long lastRecord) {
		return toByte(lastRecord + recordsPerGroup - 1);
	}

	protected final long numBytes(long firstRecord, long lastRecord) {
		return lastByte(lastRecord) - toByte(firstRecord);
	}

	protected final long longRecordGroup(byte[] values, int offset) {
		long longValues = 0;
		for (int i = 0; i < recordGroupByteLength; i++) {
			longValues <<= 8;
			longValues |= (values[offset++] & 255L);
		}
		return longValues;
	}

	protected final BigInteger bigIntRecordGroup(byte[] values, int offset) {
		byte[] bigIntByte = new byte[recordGroupByteLength];
		for (int i = 0; i < recordGroupByteLength; i++)
			bigIntByte[i] = values[offset++];
		return new BigInteger(1, bigIntByte);
	}

	private long longRecordGroup(long[] recs, int offset) {
		if (superCompress) {
			long longValues = 0;
			for (int i = 0; i < recordsPerGroup; i++)
				longValues += recs[offset++] * longMultipliers[i];
			return longValues;
		} else
			return recs[0];
	}

	private BigInteger bigIntRecordGroup(long[] recs, int offset) {
		if (superCompress) {
			BigInteger values = BigInteger.ZERO;
			for (int i = 0; i < recordsPerGroup; i++)
				values = values.add(BigInteger.valueOf(recs[offset++])
						.multiply(multipliers[i]));
			return values;
		} else
			return BigInteger.valueOf(recs[0]);
	}

	private void getRecords(long recordGroup, long[] recs, int offset) {
		if (superCompress) {
			for (int i = 0; i < recordsPerGroup; i++) {
				long mod = recordGroup % totalStates;
				recordGroup /= totalStates;
				recs[offset++] = mod;
			}
		} else
			recs[0] = recordGroup;
	}

	private void getRecords(BigInteger recordGroup, long[] recs, int offset) {
		if (superCompress) {
			for (int i = 0; i < recordsPerGroup; i++) {
				long mod = recordGroup.mod(bigIntTotalStates).longValue();
				recordGroup = recordGroup.divide(bigIntTotalStates);
				recs[offset++] = mod;
			}
		} else
			recs[0] = recordGroup.longValue();
	}

	private long setRecord(long recordGroup, int num, long r) {
		if (superCompress) {
			long multiplier = longMultipliers[num];
			long zeroOut = longMultipliers[num + 1];
			recordGroup = recordGroup
					- ((recordGroup % zeroOut) - (recordGroup % multiplier))
					+ (r * multiplier);
			return recordGroup;
		} else
			return r;
	}

	private BigInteger setRecord(BigInteger recordGroup, int num, long r) {
		if (superCompress) {
			BigInteger multiplier = multipliers[num];
			BigInteger zeroOut = multipliers[num + 1];
			recordGroup = recordGroup.subtract(
					recordGroup.mod(zeroOut).subtract(
							recordGroup.mod(multiplier))).add(
					BigInteger.valueOf(r).multiply(multiplier));
			return recordGroup;
		} else
			return BigInteger.valueOf(r);
	}

	private long getRecord(long recordGroup, int num) {
		if (superCompress)
			return recordGroup / longMultipliers[num] % totalStates;
		else
			return recordGroup;
	}

	private long getRecord(BigInteger recordGroup, int num) {
		if (superCompress)
			return recordGroup.divide(multipliers[num]).mod(bigIntTotalStates)
					.longValue();
		else
			return recordGroup.longValue();
	}

	protected final void toUnsignedByteArray(long recordGroup,
			byte[] byteArray, int offset) {
		for (int i = offset + recordGroupByteLength - 1; i >= offset; i--) {
			byteArray[i] = (byte) recordGroup;
			recordGroup >>>= 8;
		}
	}

	protected final void toUnsignedByteArray(BigInteger recordGroup,
			byte[] byteArray, int offset) {
		byte[] bigIntArray = recordGroup.toByteArray();
		int initialZeros = recordGroupByteLength - (bigIntArray.length - 1);
		for (int i = 0; i < initialZeros; i++) {
			byteArray[offset++] = 0;
		}
		for (int i = 1; i < bigIntArray.length; i++) {
			byteArray[offset++] = bigIntArray[i];
		}
	}

	public long[] splitRange(long firstRecord, long numRecords, int numSplits) {
		return Util.groupAlignedTasks(numSplits, firstRecord, numRecords,
				recordsPerGroup);
	}

	public final long requiredMem(long numHashes) {
		return toByte(numHashes);
	}
}
