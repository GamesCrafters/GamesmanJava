package edu.berkeley.gamesman.core;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;

import edu.berkeley.gamesman.database.MemoryDatabase;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * @author dnspies Stores a small group of records in the most compressed
 *         possible format.
 */
public abstract class RecordGroup {

	/**
	 * @param conf
	 *            The configuration object.
	 * @param values
	 *            The byte representation of this RecordGroup
	 */
	public static long longRecordGroup(Configuration conf, byte[] values) {
		long longValues = 0;
		for (int i = 0; i < conf.recordGroupByteLength; i++) {
			longValues <<= 8;
			longValues |= (values[i] & 255L);
		}
		return longValues;
	}

	public static BigInteger bigIntRecordGroup(Configuration conf, byte[] values) {
		return new BigInteger(1, values);
	}

	/**
	 * @param conf
	 *            Creates a RecordGroup from the given configuration and records
	 * @param recs
	 *            The records array
	 * @param offset
	 *            The offset into the array. len = conf.recordsPerGroup
	 */
	public static long longRecordGroup(Configuration conf, Record[] recs,
			int offset) {
		long longValues = 0;
		for (int i = 0; i < conf.recordsPerGroup; i++)
			longValues += recs[offset++].getState() * conf.longMultipliers[i];
		return longValues;
	}

	public static BigInteger bigIntRecordGroup(Configuration conf,
			Record[] recs, int offset) {
		BigInteger values = BigInteger.ZERO;
		for (int i = 0; i < conf.recordsPerGroup; i++)
			values = values.add(BigInteger.valueOf(recs[offset++].getState())
					.multiply(conf.multipliers[i]));
		return values;
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordIterator
	 *            An iterator over the records to use to construct this group
	 */
	public static long longRecordGroup(Configuration conf,
			Iterator<Record> recordIterator) {
		long longValues = 0L;
		for (int i = 0; i < conf.recordsPerGroup; i++)
			longValues += recordIterator.next().getState()
					* conf.longMultipliers[i];
		return longValues;
	}

	public static BigInteger bigIntRecordGroup(Configuration conf,
			Iterator<Record> recordIterator) {
		BigInteger values = BigInteger.ZERO;
		for (int i = 0; i < conf.recordsPerGroup; i++) {
			values = values.add(BigInteger.valueOf(
					recordIterator.next().getState()).multiply(
					conf.multipliers[i]));
		}
		return values;
	}

	/**
	 * @param num
	 *            The index of the desired record
	 * @return The record
	 */
	public static Record getRecord(Configuration conf, long recordGroup, int num) {

		return conf.getGame().newRecord(
				recordGroup / conf.longMultipliers[num] % conf.totalStates);
	}

	public static Record getRecord(Configuration conf, BigInteger recordGroup,
			int num) {
		return conf.getGame().newRecord(
				recordGroup.divide(conf.multipliers[num]).mod(
						conf.bigIntTotalStates).longValue());
	}

	/**
	 * Sets the records in this group to Records from recs
	 * 
	 * @param recs
	 *            An array of records to use
	 * @param offset
	 *            The offset into the array
	 */
	public static void getRecords(Configuration conf, long recordGroup,
			Record[] recs, int offset) {
		for (int i = 0; i < conf.recordsPerGroup; i++) {
			long mod = recordGroup % conf.totalStates;
			recordGroup /= conf.totalStates;
			recs[offset++].set(mod);
		}
	}

	public static void getRecords(Configuration conf, BigInteger recordGroup,
			Record[] recs, int offset) {
		for (int i = 0; i < conf.recordsPerGroup; i++) {
			long mod = recordGroup.mod(conf.bigIntTotalStates).longValue();
			recordGroup = recordGroup.divide(conf.bigIntTotalStates);
			recs[offset++].set(mod);
		}
	}

	/**
	 * Changes a single record in the group.
	 * 
	 * @param num
	 *            The record to change
	 * @param r
	 *            The values to change it to
	 */
	public static long setRecord(Configuration conf, long recordGroup, int num,
			Record r) {
		long multiplier = conf.longMultipliers[num];
		long zeroOut = conf.longMultipliers[num + 1];
		recordGroup = recordGroup
				- ((recordGroup % zeroOut) - (recordGroup % multiplier))
				+ (r.getState() * multiplier);
		return recordGroup;
	}

	public static BigInteger setRecord(Configuration conf,
			BigInteger recordGroup, int num, Record r) {
		BigInteger multiplier = conf.multipliers[num];
		BigInteger zeroOut = conf.multipliers[num + 1];
		recordGroup = recordGroup.subtract(
				recordGroup.mod(zeroOut).subtract(recordGroup.mod(multiplier)))
				.add(BigInteger.valueOf(r.getState()).multiply(multiplier));
		return recordGroup;
	}

	/**
	 * @param num
	 *            The index of the desired record
	 * @param r
	 *            The Record to store in.
	 */
	public static void getRecord(Configuration conf, long recordGroup, int num,
			Record r) {
		r.set(recordGroup / conf.longMultipliers[num] % conf.totalStates);
	}

	public static void getRecord(Configuration conf, BigInteger recordGroup,
			int num, Record r) {
		r.set(recordGroup.divide(conf.multipliers[num]).mod(
				conf.bigIntTotalStates).longValue());
	}

	/**
	 * Outputs the bytes from this RecordGroup into output
	 * 
	 * @param output
	 *            A ByteBuffer to output to
	 */
	public static void outputUnsignedBytes(Configuration conf,
			long recordGroup, ByteBuffer output) {
		for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
			output.put((byte) (recordGroup >>> i));
	}

	public static void outputUnsignedBytes(Configuration conf,
			BigInteger recordGroup, ByteBuffer output) {
		recordGroup.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}

	/**
	 * Outputs the bytes from this RecordGroup into byteArray
	 * 
	 * @param byteArray
	 *            The byte array to output to
	 * @param offset
	 *            The offset into byteArray
	 */
	public static void toUnsignedByteArray(Configuration conf,
			long recordGroup, byte[] byteArray, int offset) {
		for (int i = offset + conf.recordGroupByteLength - 1; i >= offset; i--) {
			byteArray[i] = (byte) recordGroup;
			recordGroup >>>= 8;
		}
	}

	public static void toUnsignedByteArray(Configuration conf,
			BigInteger recordGroup, byte[] byteArray, int offset) {
		recordGroup.toUnsignedByteArray(byteArray, offset,
				conf.recordGroupByteLength);
	}

	/**
	 * Outputs the bytes from this RecordGroup into output
	 * 
	 * @param output
	 *            A DataOutput to output to
	 * @throws IOException
	 *             If output throws an IOException
	 */
	public static void outputUnsignedBytes(Configuration conf,
			long recordGroup, DataOutput output) throws IOException {
		for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
			output.write((int) (recordGroup >>> i));
	}

	public static void outputUnsignedBytes(Configuration conf,
			BigInteger recordGroup, DataOutput output) throws IOException {
		recordGroup.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}

	/**
	 * Outputs the bytes from this RecordGroup into output
	 * 
	 * @param output
	 *            A MemoryDatabase to output to
	 * @param offset
	 *            The offset into the MemoryDatabase
	 */
	public static void writeToUnsignedMemoryDatabase(Configuration conf,
			long recordGroup, MemoryDatabase output, long offset) {
		for (long i = offset + conf.recordGroupByteLength - 1; i >= offset; i--) {
			output.putByte(i, (byte) recordGroup);
			recordGroup >>>= 8;
		}
	}

	public static void writeToUnsignedMemoryDatabase(Configuration conf,
			BigInteger recordGroup, MemoryDatabase output, long offset) {
		recordGroup.writeToUnsignedMemoryDatabase(output, offset,
				conf.recordGroupByteLength);
	}

	/**
	 * Outputs the bytes from this RecordGroup into output
	 * 
	 * @param output
	 *            An OutputStream to output to
	 * @throws IOException
	 *             If output throws an IOException
	 */
	public static void outputUnsignedBytes(Configuration conf,
			long recordGroup, OutputStream output) throws IOException {
		for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
			output.write((int) (recordGroup >>> i));
	}

	public static void outputUnsignedBytes(Configuration conf,
			BigInteger recordGroup, OutputStream output) throws IOException {
		recordGroup.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}
}
