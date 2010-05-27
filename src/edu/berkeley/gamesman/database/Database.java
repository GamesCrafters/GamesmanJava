package edu.berkeley.gamesman.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;

/**
 * A Database is the abstract superclass of all data storage methods used in
 * Gamesman. Each particular Database is responsible for the persistent storage
 * of Records derived from solving games.
 */
public abstract class Database {

	protected final Configuration conf;

	protected final boolean solve;

	private final long numRecords;

	protected final long numContainedRecords;

	private final long firstRecord;

	protected final long firstContainedRecord;

	protected long location;

	protected final DatabaseHandle myHandle;

	protected final long totalStates;

	protected final BigInteger bigIntTotalStates;

	protected final BigInteger[] multipliers;

	protected final long[] longMultipliers;

	protected final int recordsPerGroup;

	protected final int recordGroupByteLength;

	private final int recordGroupByteBits;

	private final boolean superCompress;

	protected final boolean recordGroupUsesLong;

	protected Database(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords, Database innerDb) {
		byte[] dbInfo = null;
		if (numRecords == -1) {
			firstRecord = 0;
			numRecords = conf.getGame().numHashes();
		}
		if (innerDb == null)
			if (!solve)
				try {
					FileInputStream fis = new FileInputStream(uri);
					dbInfo = new byte[18];
					fis.read(dbInfo);
					if (conf == null)
						conf = Configuration.load(fis);
					fis.close();
				} catch (ClassNotFoundException e) {
					throw new Error(e);
				} catch (IOException e) {
					throw new Error(e);
				}
		this.conf = conf;
		this.solve = solve;
		totalStates = conf.getGame().recordStates();
		bigIntTotalStates = BigInteger.valueOf(totalStates);
		if (innerDb != null) {
			recordGroupByteBits = innerDb.recordGroupByteBits;
			recordGroupByteLength = innerDb.recordGroupByteLength;
			recordsPerGroup = innerDb.recordsPerGroup;
			superCompress = innerDb.superCompress;
			firstContainedRecord = innerDb.firstRecord;
			this.firstRecord = Math.max(firstRecord, firstContainedRecord);
			numContainedRecords = innerDb.numRecords;
			this.numRecords = Math.min(firstRecord + numRecords,
					firstContainedRecord + numContainedRecords)
					- this.firstRecord;
			recordGroupUsesLong = innerDb.recordGroupUsesLong;
			multipliers = innerDb.multipliers;
			longMultipliers = innerDb.longMultipliers;
		} else {
			if (solve) {
				double requiredCompression = Double.parseDouble(conf
						.getProperty("record.compression", "0")) / 100;
				double compression;
				if (requiredCompression == 0D) {
					superCompress = false;
					int bits = (int) (Math.log(totalStates) / Math.log(2));
					if ((1 << bits) < totalStates)
						++bits;
					int recordGroupByteLength = (bits + 7) >> 3;
					int recordGroupByteBits = 0;
					recordGroupByteLength >>= 1;
					while (recordGroupByteLength > 0) {
						recordGroupByteBits++;
						recordGroupByteLength >>= 1;
					}
					this.recordGroupByteBits = recordGroupByteBits;
					this.recordGroupByteLength = 1 << recordGroupByteBits;
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
							compression = (recordGuess * log2 / 8)
									/ (bitLength >> 3);
						}
					}
					recordsPerGroup = recordGuess;
					recordGroupByteLength = (bigIntTotalStates.pow(
							recordsPerGroup).bitLength() + 7) >> 3;
					recordGroupByteBits = -1;
				}
				firstContainedRecord = this.firstRecord = firstRecord;
				numContainedRecords = this.numRecords = numRecords;
			} else {
				long firstContainedRecord = 0;
				for (int i = 0; i < 8; i++) {
					firstContainedRecord <<= 8;
					firstContainedRecord |= (dbInfo[i] & 255);
				}
				this.firstContainedRecord = firstContainedRecord;
				this.firstRecord = Math.max(firstRecord, firstContainedRecord);
				long numContainedRecords = 0;
				for (int i = 8; i < 16; i++) {
					numContainedRecords <<= 8;
					numContainedRecords |= (dbInfo[i] & 255);
				}
				this.numContainedRecords = numContainedRecords;
				this.numRecords = Math.min(firstContainedRecord
						+ numContainedRecords, firstRecord + numRecords)
						- this.firstRecord;
				int firstByte = dbInfo[16];
				int secondByte = 255 & dbInfo[17];
				if (firstByte < 0) {
					superCompress = false;
					recordsPerGroup = 1;
					recordGroupByteBits = secondByte;
					recordGroupByteLength = 1 << secondByte;
				} else {
					superCompress = true;
					recordsPerGroup = (firstByte << 2) | (secondByte >> 6);
					recordGroupByteLength = secondByte & 63;
					recordGroupByteBits = -1;
				}
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
		}
		myHandle = getHandle();
		assert Util.debug(DebugFacility.DATABASE, recordsPerGroup
				+ " records per group\n" + recordGroupByteLength
				+ " bytes per group");

	}

	/**
	 * Initialize a Database given a URI and a Configuration. This method may
	 * either open an existing database or create a new one. If a new one is
	 * created, the Configuration should be stored. If config is null, it will
	 * use whatever is in the database.
	 * 
	 * @param uri
	 *            The URI that the Database is associated with
	 * @param conf
	 *            The Configuration that is relevant
	 * @param solve
	 *            true for solving, false for playing
	 */
	public Database(String uri, Configuration conf, boolean solve,
			long firstRecord, long numRecords) {
		this(uri, conf, solve, firstRecord, numRecords, null);
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
	public final void close() {
		closeDatabase();
		closeHandle(myHandle);
	}

	protected abstract void closeDatabase();

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
			byte[] arr, int off, int numRecords, boolean overwriteEdgesOk) {
		long byteIndex = toByte(recordIndex);
		if (overwriteEdgesOk) {
			long lastByte = lastByte(recordIndex + numRecords);
			putBytes(dh, byteIndex, arr, off, (int) (lastByte - byteIndex));
			return;
		}
		long lastRecord = recordIndex + numRecords;
		long lastByte = toByte(lastRecord);
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
		putRecordsAsBytes(dh, recordIndex, groupBytes, 0, numRecords, false);
		dh.releaseBytes(groupBytes);
	}

	protected void putRecordsAsGroup(DatabaseHandle dh, long recordIndex,
			int numRecords, BigInteger rg) {
		byte[] groupBytes = dh.getRecordGroupBytes();
		toUnsignedByteArray(rg, groupBytes, 0);
		putRecordsAsBytes(dh, recordIndex, groupBytes, 0, numRecords, false);
		dh.releaseBytes(groupBytes);
	}

	protected final long splice(long group1, long group2, int num) {
		if (superCompress)
			return group1 % longMultipliers[num]
					+ (group2 - group2 % longMultipliers[num]);
		else if (num == 0)
			return group2;
		else
			return group1;
	}

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
	 * @return The index of the first byte in this database (Will be zero if
	 *         this database stores the entire game)
	 */
	public long firstRecord() {
		return firstRecord;
	}

	public final boolean containsRecord(long hash) {
		return hash >= firstRecord() && hash < firstRecord() + numRecords();
	}

	public DatabaseHandle getHandle() {
		return new DatabaseHandle(recordGroupByteLength);
	}

	public void closeHandle(DatabaseHandle dh) {
	}

	public DatabaseHandle getHandle(long recordStart, long numRecords) {
		return getHandle();
	}

	protected final long toByte(long recordIndex) {
		if (superCompress)
			return recordIndex / recordsPerGroup * recordGroupByteLength;
		else
			return recordIndex << recordGroupByteBits;
	}

	protected final long toFirstRecord(long byteIndex) {
		if (superCompress)
			return byteIndex / recordGroupByteLength * recordsPerGroup;
		else
			return byteIndex >> recordGroupByteBits;
	}

	protected long toLastRecord(long byteIndex) {
		return toFirstRecord(byteIndex + recordGroupByteLength - 1);
	}

	protected final int toNum(long recordIndex) {
		if (superCompress)
			return (int) (recordIndex % recordsPerGroup);
		else
			return 0;
	}

	protected final long lastByte(long lastRecord) {
		return toByte(lastRecord + recordsPerGroup - 1);
	}

	protected final long numBytes(long firstRecord, long numRecords) {
		return lastByte(firstRecord + numRecords) - toByte(firstRecord);
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

	protected final long longRecordGroup(long[] recs, int offset) {
		if (superCompress) {
			long longValues = 0;
			for (int i = 0; i < recordsPerGroup; i++)
				longValues += recs[offset++] * longMultipliers[i];
			return longValues;
		} else
			return recs[0];
	}

	protected final BigInteger bigIntRecordGroup(long[] recs, int offset) {
		if (superCompress) {
			BigInteger values = BigInteger.ZERO;
			for (int i = 0; i < recordsPerGroup; i++)
				values = values.add(BigInteger.valueOf(recs[offset++])
						.multiply(multipliers[i]));
			return values;
		} else
			return BigInteger.valueOf(recs[0]);
	}

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

	protected final void getRecords(BigInteger recordGroup, long[] recs,
			int offset) {
		if (superCompress) {
			for (int i = 0; i < recordsPerGroup; i++) {
				long mod = recordGroup.mod(bigIntTotalStates).longValue();
				recordGroup = recordGroup.divide(bigIntTotalStates);
				recs[offset++] = mod;
			}
		} else
			recs[0] = recordGroup.longValue();
	}

	protected final long setRecord(long recordGroup, int num, long r) {
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

	protected final BigInteger setRecord(BigInteger recordGroup, int num, long r) {
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

	protected final long getRecord(long recordGroup, int num) {
		if (superCompress)
			return recordGroup / longMultipliers[num] % totalStates;
		else
			return recordGroup;
	}

	protected final long getRecord(BigInteger recordGroup, int num) {
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

	public static Database openDatabase(String uri, boolean solve) {
		return openDatabase(uri, null, solve);
	}

	public static Database openDatabase(String uri, boolean solve,
			long firstRecord, long numRecords) {
		return openDatabase(uri, null, solve, firstRecord, numRecords);
	}

	/**
	 * @param solve
	 *            true for solving, false for playing
	 * @return the Database used to store this particular solve
	 */
	public static Database openDatabase(String uri, Configuration conf,
			boolean solve) {
		return openDatabase(uri, conf, solve, 0, -1);
	}

	/**
	 * @param solve
	 *            true for solving, false for playing
	 * @param firstRecord
	 *            The index of the first record this database contains
	 * @param numRecords
	 *            The number of records in this database
	 * @return the Database used to store this particular solve Could not load
	 *         the database class
	 */
	public static Database openDatabase(String uri, Configuration conf,
			boolean solve, long firstRecord, long numRecords) {
		if (conf == null)
			try {
				FileInputStream fis = new FileInputStream(uri);
				fis.skip(18);
				conf = Configuration.load(fis);
				fis.close();
			} catch (ClassNotFoundException e) {
				throw new Error(e);
			} catch (IOException e) {
				throw new Error(e);
			}
		if (uri != null)
			conf.setProperty("gamesman.db.uri", uri);
		String[] dbType = conf.getProperty("gamesman.database").split(":");
		try {
			Class<? extends Database> dbClass = Class.forName(
					"edu.berkeley.gamesman.database."
							+ dbType[dbType.length - 1]).asSubclass(
					Database.class);
			conf.db = dbClass.getConstructor(String.class, Configuration.class,
					Boolean.TYPE, Long.TYPE, Long.TYPE).newInstance(
					conf.getProperty("gamesman.db.uri"), conf, solve,
					firstRecord, numRecords);
			for (int i = dbType.length - 2; i >= 0; i--) {
				Class<? extends DatabaseWrapper> wrapperClass = Class.forName(
						"edu.berkeley.gamesman.database." + dbType[i])
						.asSubclass(DatabaseWrapper.class);
				conf.db = wrapperClass.getConstructor(Database.class,
						String.class, Configuration.class, Boolean.TYPE,
						Long.TYPE, Long.TYPE).newInstance(conf.db,
						conf.getProperty("gamesman.db.uri"), conf, solve,
						firstRecord, numRecords);
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
		return conf.db;
	}

	protected final void storeNone(OutputStream os) throws IOException {
		storeInfo(os);
		Configuration.storeNone(os);
	}

	protected final void store(OutputStream os) throws IOException {
		storeInfo(os);
		conf.store(os);
	}

	protected final void skipHeader(InputStream is) throws IOException {
		skipInfo(is);
		Configuration.skipConf(is);
	}

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

	private void skipInfo(InputStream is) throws IOException {
		byte[] b = new byte[18];
		int len = 18;
		while (len > 0)
			len -= is.read(b, 0, len);
	}
}
