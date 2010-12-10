package edu.berkeley.gamesman.database;

import java.math.BigInteger;

import edu.berkeley.gamesman.core.Configuration;

/**
 * Contains specific information about a database that's not in the
 * configuration object. Specifically:<br />
 * firstRecord: The index of first record this database contains.<br />
 * numRecords: The number of records contained in this database.<br />
 * superCompress: Whether or not this database uses base-n compression<br />
 * recordsPerGroup: If superCompress, the number of records stored in one record
 * group, otherwise 1<br />
 * recordGroupByteLength: The number of bytes used to store a record group (or a
 * record if not superCompress). If not superCompress, this will be a power of 2<br />
 * recordGroupByteBits: If superCompress, -1, otherwise
 * log_2(recordGroupByteLength)<br />
 * All this information is stored in the first 18 bytes of any database header.
 * 
 * @author dnspies
 */
public final class DatabaseHeader {
	/**
	 * The index of first record this database contains.<br />
	 */
	public final long firstRecord;

	/**
	 * The number of records contained in this database.
	 */
	public final long numRecords;

	/**
	 * If superCompress, the number of records stored in one record group,
	 * otherwise 1
	 */
	final int recordsPerGroup;

	/**
	 * The number of bytes used to store a record group (or a record if not
	 * superCompress). If not superCompress, this will be a power of 2
	 */
	final int recordGroupByteLength;

	/**
	 * If superCompress, -1, otherwise log_2(recordGroupByteLength)
	 */
	final int recordGroupByteBits;

	/**
	 * Whether or not this database uses base-n compression
	 */
	final boolean superCompress;

	/**
	 * Construct a database header with superCompress = true
	 * 
	 * @param firstRecord
	 *            The index of the first record in this database
	 * @param numRecords
	 *            The number of records in this database
	 * @param recordsPerGroup
	 *            The number of records in a record group
	 * @param recordGroupByteLength
	 *            The number of bytes in a record group. Make sure
	 *            recordsPerGroup*log_2(totalStates)<=8*recordGroupByteLength
	 */
	DatabaseHeader(long firstRecord, long numRecords, int recordsPerGroup,
			int recordGroupByteLength) {
		superCompress = true;
		recordGroupByteBits = -1;
		this.firstRecord = firstRecord;
		this.numRecords = numRecords;
		this.recordsPerGroup = recordsPerGroup;
		this.recordGroupByteLength = recordGroupByteLength;
	}

	/**
	 * Construct a database header with superCompress = true
	 * 
	 * @param firstRecord
	 *            The index of the first record in this database
	 * @param numRecords
	 *            The number of records in this database
	 * @param recordGroupByteBits
	 *            log_2(recordGroupByteLength)
	 */
	DatabaseHeader(long firstRecord, long numRecords, int recordGroupByteBits) {
		superCompress = false;
		this.recordGroupByteBits = recordGroupByteBits;
		this.firstRecord = firstRecord;
		this.numRecords = numRecords;
		this.recordsPerGroup = 1;
		this.recordGroupByteLength = 1 << recordGroupByteBits;
	}

	/**
	 * Construct a database header from the initial 18 bytes of a database.
	 * 
	 * @param dbInfo
	 *            The 18 byte header
	 */
	public DatabaseHeader(byte[] dbInfo) {
		long firstContainedRecord = 0;
		for (int i = 0; i < 8; i++) {
			firstContainedRecord <<= 8;
			firstContainedRecord |= (dbInfo[i] & 255);
		}
		this.firstRecord = firstContainedRecord;
		long numContainedRecords = 0;
		for (int i = 8; i < 16; i++) {
			numContainedRecords <<= 8;
			numContainedRecords |= (dbInfo[i] & 255);
		}
		this.numRecords = numContainedRecords;
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

	public DatabaseHeader(Configuration conf, long firstRecord, long numRecords) {
		recordGroupByteBits = conf.getInteger("recordGroupByteBits", -1);
		if (recordGroupByteBits < 0) {
			superCompress = true;
			this.firstRecord = firstRecord;
			this.numRecords = numRecords;
			this.recordsPerGroup = conf.getInteger("recordsPerGroup", -1);
			this.recordGroupByteLength = conf.getInteger(
					"recordGroupByteLength", -1);
		} else {
			superCompress = false;
			this.firstRecord = firstRecord;
			this.numRecords = numRecords;
			this.recordsPerGroup = 1;
			this.recordGroupByteLength = 1 << recordGroupByteBits;
		}
	}

	public DatabaseHeader(double requiredCompression, long totalStates,
			long firstRecord, long numRecords) {
		this.firstRecord = firstRecord;
		this.numRecords = numRecords;
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
					compression = (recordGuess * log2 / 8) / (bitLength >> 3);
				}
			}
			recordsPerGroup = recordGuess;
			recordGroupByteLength = (BigInteger.valueOf(totalStates)
					.pow(recordsPerGroup).bitLength() + 7) >> 3;
			recordGroupByteBits = -1;
		}
	}

	/**
	 * The first 8 bytes is firstRecord. The next 8 bytes is numRecords.<br />
	 * If superCompress, the next 10 bits are recordsPerGroup and the last 6
	 * bits are recordGroupByteLength<br />
	 * Otherwise, the next byte is -1 and the next byte is recordGroupByteBits
	 * 
	 * @return The 18 bytes representing this header
	 */
	public byte[] toBytes() {
		int c = 0;
		byte[] b = new byte[18];
		for (int i = 56; i >= 0; i -= 8)
			b[c++] = (byte) (firstRecord >>> i);
		for (int i = 56; i >= 0; i -= 8)
			b[c++] = (byte) (numRecords >>> i);
		if (superCompress) {
			b[c++] = (byte) (recordsPerGroup >>> 2);
			b[c++] = (byte) (((recordsPerGroup & 3) << 6) | recordGroupByteLength);
		} else {
			b[c++] = -1;
			b[c++] = (byte) recordGroupByteBits;
		}
		return b;
	}

	/**
	 * Clones this header, but with different firstRecord and numRecords values
	 * 
	 * @param firstRecord
	 *            The new firstRecord
	 * @param numRecords
	 *            The new numRecords
	 * @return The new header
	 */
	public final DatabaseHeader getHeader(long firstRecord, long numRecords) {
		if (superCompress)
			return new DatabaseHeader(firstRecord, numRecords, recordsPerGroup,
					recordGroupByteLength);
		else
			return new DatabaseHeader(firstRecord, numRecords,
					recordGroupByteBits);
	}

	public static void setHeaderInfo(Configuration gamesmanConf,
			float recordCompression, long recordStates) {
		DatabaseHeader tempHeader = new DatabaseHeader(recordCompression,
				recordStates, 0, -1);
		gamesmanConf.setProperty("recordGroupByteBits",
				Integer.toString(tempHeader.recordGroupByteBits));
		gamesmanConf.setProperty("recordsPerGroup",
				Integer.toString(tempHeader.recordsPerGroup));
		gamesmanConf.setProperty("recordGroupByteLength",
				Integer.toString(tempHeader.recordGroupByteLength));

	}
}
