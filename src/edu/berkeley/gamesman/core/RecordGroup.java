package edu.berkeley.gamesman.core;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;

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
	 * @param offset
	 *            The offset at which to start reading
	 * @return A long representing the created RecordGroup
	 */
	public static long longRecordGroup(Configuration conf, byte[] values,
			int offset) {
		long longValues = 0;
		for (int i = 0; i < conf.recordGroupByteLength; i++) {
			longValues <<= 8;
			longValues |= (values[offset++] & 255L);
		}
		return longValues;
	}

	/**
	 * @param conf
	 *            The configuration object.
	 * @param values
	 *            The byte representation of this RecordGroup
	 * @param offset
	 *            The offset at which to start reading
	 * @return A BigInteger representing the created RecordGroup
	 */
	public static BigInteger bigIntRecordGroup(Configuration conf,
			byte[] values, int offset) {
		byte[] bigIntByte = new byte[conf.recordGroupByteLength];
		for (int i = 0; i < conf.recordGroupByteLength; i++)
			bigIntByte[i] = values[offset++];
		return new BigInteger(1, bigIntByte);
	}

	/**
	 * @param conf
	 *            Creates a RecordGroup from the given configuration and records
	 * @param recs
	 *            The records array
	 * @param offset
	 *            The offset into the array. len = conf.recordsPerGroup
	 * @return A long representing the created RecordGroup
	 */
	public static long longRecordGroup(Configuration conf, Record[] recs,
			int offset) {
		if (conf.superCompress) {
			long longValues = 0;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				longValues += recs[offset++].getState()
						* conf.longMultipliers[i];
			return longValues;
		} else
			return recs[0].getState();
	}

	/**
	 * @param conf
	 *            Creates a RecordGroup from the given configuration and records
	 * @param recs
	 *            The records array
	 * @param offset
	 *            The offset into the array. len = conf.recordsPerGroup
	 * @return A BigInteger representing the created RecordGroup
	 */
	public static BigInteger bigIntRecordGroup(Configuration conf,
			Record[] recs, int offset) {
		if (conf.superCompress) {
			BigInteger values = BigInteger.ZERO;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				values = values.add(BigInteger.valueOf(
						recs[offset++].getState())
						.multiply(conf.multipliers[i]));
			return values;
		} else
			return BigInteger.valueOf(recs[0].getState());
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordIterator
	 *            An iterator over the records to use to construct this group
	 * @return A long representing the created RecordGroup
	 */
	public static long longRecordGroup(Configuration conf,
			Iterator<Record> recordIterator) {
		if (conf.superCompress) {
			long longValues = 0L;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				longValues += recordIterator.next().getState()
						* conf.longMultipliers[i];
			return longValues;
		} else
			return recordIterator.next().getState();
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordIterator
	 *            An iterator over the records to use to construct this group
	 * @return A BigInteger representing the created RecordGroup
	 */
	public static BigInteger bigIntRecordGroup(Configuration conf,
			Iterator<Record> recordIterator) {
		if (conf.superCompress) {
			BigInteger values = BigInteger.ZERO;
			for (int i = 0; i < conf.recordsPerGroup; i++) {
				values = values.add(BigInteger.valueOf(
						recordIterator.next().getState()).multiply(
						conf.multipliers[i]));
			}
			return values;
		} else
			return BigInteger.valueOf(recordIterator.next().getState());
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param num
	 *            The index of the desired record
	 * @return The record
	 */
	public static Record getRecord(Configuration conf, long recordGroup, int num) {
		if (conf.superCompress)
			return conf.getGame().newRecord(
					recordGroup / conf.longMultipliers[num] % conf.totalStates);
		else
			return conf.getGame().newRecord(recordGroup);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param num
	 *            The index of the desired record
	 * @return The record
	 */
	public static Record getRecord(Configuration conf, BigInteger recordGroup,
			int num) {
		if (conf.superCompress) {
			return conf.getGame().newRecord(
					recordGroup.divide(conf.multipliers[num]).mod(
							conf.bigIntTotalStates).longValue());
		} else
			return conf.getGame().newRecord(recordGroup.longValue());
	}

	/**
	 * Sets the records in this group to Records from recs
	 * 
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param recs
	 *            An array of records to use
	 * @param offset
	 *            The offset into the array
	 */
	public static void getRecords(Configuration conf, long recordGroup,
			Record[] recs, int offset) {
		if (conf.superCompress) {
			for (int i = 0; i < conf.recordsPerGroup; i++) {
				long mod = recordGroup % conf.totalStates;
				recordGroup /= conf.totalStates;
				recs[offset++].set(mod);
			}
		} else
			recs[0].set(recordGroup);
	}

	/**
	 * Sets the records in this group to Records from recs
	 * 
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param recs
	 *            An array of records to use
	 * @param offset
	 *            The offset into the array
	 */
	public static void getRecords(Configuration conf, BigInteger recordGroup,
			Record[] recs, int offset) {
		if (conf.superCompress) {
			for (int i = 0; i < conf.recordsPerGroup; i++) {
				long mod = recordGroup.mod(conf.bigIntTotalStates).longValue();
				recordGroup = recordGroup.divide(conf.bigIntTotalStates);
				recs[offset++].set(mod);
			}
		} else
			recs[0].set(recordGroup.longValue());
	}

	/**
	 * Changes a single record in the group.
	 * 
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param num
	 *            The record to change
	 * @param r
	 *            The values to change it to
	 * @return The resulting Record Group
	 */
	public static long setRecord(Configuration conf, long recordGroup, int num,
			Record r) {
		if (conf.superCompress) {
			long multiplier = conf.longMultipliers[num];
			long zeroOut = conf.longMultipliers[num + 1];
			recordGroup = recordGroup
					- ((recordGroup % zeroOut) - (recordGroup % multiplier))
					+ (r.getState() * multiplier);
			return recordGroup;
		} else
			return r.getState();
	}

	/**
	 * Changes a single record in the group.
	 * 
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param num
	 *            The record to change
	 * @param r
	 *            The values to change it to
	 * @return The resulting Record Group
	 */
	public static BigInteger setRecord(Configuration conf,
			BigInteger recordGroup, int num, Record r) {
		if (conf.superCompress) {
			BigInteger multiplier = conf.multipliers[num];
			BigInteger zeroOut = conf.multipliers[num + 1];
			recordGroup = recordGroup.subtract(
					recordGroup.mod(zeroOut).subtract(
							recordGroup.mod(multiplier))).add(
					BigInteger.valueOf(r.getState()).multiply(multiplier));
			return recordGroup;
		} else
			return BigInteger.valueOf(r.getState());
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param num
	 *            The index of the desired record
	 * @param r
	 *            The Record to store in.
	 */
	public static void getRecord(Configuration conf, long recordGroup, int num,
			Record r) {
		if (conf.superCompress)
			r.set(recordGroup / conf.longMultipliers[num] % conf.totalStates);
		else
			r.set(recordGroup);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param num
	 *            The index of the desired record
	 * @param r
	 *            The Record to store in.
	 */
	public static void getRecord(Configuration conf, BigInteger recordGroup,
			int num, Record r) {
		if (conf.superCompress)
			r.set(recordGroup.divide(conf.multipliers[num]).mod(
					conf.bigIntTotalStates).longValue());
		else
			r.set(recordGroup.longValue());
	}

	/**
	 * Outputs the bytes from this RecordGroup into byteArray
	 * 
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
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

	/**
	 * Outputs the bytes from this RecordGroup into byteArray
	 * 
	 * @param conf
	 *            The configuration object
	 * @param recordGroup
	 *            The Record Group
	 * @param byteArray
	 *            The byte array to output to
	 * @param offset
	 *            The offset into byteArray
	 */
	public static void toUnsignedByteArray(Configuration conf,
			BigInteger recordGroup, byte[] byteArray, int offset) {
		byte[] bigIntArray = recordGroup.toByteArray();
		int initialZeros = conf.recordGroupByteLength
				- (bigIntArray.length - 1);
		for (int i = 0; i < initialZeros; i++) {
			byteArray[offset++] = 0;
		}
		for (int i = 1; i < bigIntArray.length; i++) {
			byteArray[offset++] = bigIntArray[i];
		}
	}
}
