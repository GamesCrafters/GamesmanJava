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
public class RecordGroup {
	protected final Configuration conf;

	private BigInteger values;

	private long longValues;

	/**
	 * @param conf
	 *            The configuration object.
	 * @param values
	 *            The byte representation of this RecordGroup
	 */
	public RecordGroup(Configuration conf, byte[] values) {
		this.conf = conf;
		if (conf.recordGroupUsesLong) {
			this.longValues = 0;
			for (int i = 0; i < conf.recordGroupByteLength; i++) {
				longValues <<= 8;
				longValues |= (values[i] & 255L);
			}
		} else
			this.values = new BigInteger(1, values);
	}

	/**
	 * @param conf
	 *            The configuration object.
	 * @param values
	 *            The big integer representation of this RecordGroup
	 */
	public RecordGroup(Configuration conf, BigInteger values) {
		this.conf = conf;
		if (conf.recordGroupUsesLong)
			this.longValues = values.longValue();
		else
			this.values = values;
	}

	/**
	 * @param conf
	 *            Creates a RecordGroup from the given configuration and records
	 * @param recs
	 *            The records array
	 * @param offset
	 *            The offset into the array. len = conf.recordsPerGroup
	 */
	public RecordGroup(Configuration conf, Record[] recs, int offset) {
		this.conf = conf;
		if (conf.recordGroupUsesLong) {
			this.longValues = 0;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				longValues += recs[offset++].getState()
						* conf.longMultipliers[i];
		} else {
			values = BigInteger.ZERO;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				values = values.add(BigInteger.valueOf(
						recs[offset++].getState())
						.multiply(conf.multipliers[i]));
		}
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param recordIterator
	 *            An iterator over the records to use to construct this group
	 */
	public RecordGroup(Configuration conf, Iterator<Record> recordIterator) {
		this.conf = conf;
		if (conf.recordGroupUsesLong) {
			this.longValues = 0L;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				longValues += recordIterator.next().getState()
						* conf.longMultipliers[i];
		} else {
			values = BigInteger.ZERO;
			for (int i = 0; i < conf.recordsPerGroup; i++) {
				values = values.add(BigInteger.valueOf(
						recordIterator.next().getState()).multiply(
						conf.multipliers[i]));
			}
		}
	}

	/**
	 * Creates an empty RecordGroup to be set later
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public RecordGroup(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * @param num
	 *            The index of the desired record
	 * @return The record
	 */
	public Record getRecord(int num) {
		long val;
		if (conf.recordGroupUsesLong) {
			val = longValues / conf.longMultipliers[num] % conf.totalStates;
		} else {
			val = values.divide(conf.multipliers[num]).mod(
					conf.bigIntTotalStates).longValue();
		}
		return conf.getGame().newRecord(val);
	}

	/**
	 * Sets the records in this group to Records from recs
	 * 
	 * @param recs
	 *            An array of records to use
	 * @param offset
	 *            The offset into the array
	 */
	public void getRecords(Record[] recs, int offset) {
		if (conf.recordGroupUsesLong) {
			long remainingValues = longValues;
			for (int i = 0; i < conf.recordsPerGroup; i++) {
				long mod = remainingValues % conf.totalStates;
				remainingValues /= conf.totalStates;
				recs[offset++].set(mod);
			}
		} else {
			BigInteger remainingValues = values;
			for (int i = 0; i < conf.recordsPerGroup; i++) {
				long mod = remainingValues.mod(conf.bigIntTotalStates)
						.longValue();
				remainingValues = remainingValues
						.divide(conf.bigIntTotalStates);
				recs[offset++].set(mod);
			}
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
	public void setRecord(int num, Record r) {
		if (conf.recordGroupUsesLong) {
			long multiplier = conf.longMultipliers[num];
			long zeroOut = conf.longMultipliers[num + 1];
			longValues = longValues
					- ((longValues % zeroOut) - (longValues % multiplier))
					+ (r.getState() * multiplier);
		} else {
			BigInteger multiplier = conf.multipliers[num];
			BigInteger zeroOut = conf.multipliers[num + 1];
			values = values.subtract(
					values.mod(zeroOut).subtract(values.mod(multiplier))).add(
					BigInteger.valueOf(r.getState()).multiply(multiplier));
		}
	}

	/**
	 * @param values
	 *            The new value to give this RecordGroup
	 */
	public void setValue(byte[] values) {
		if (conf.recordGroupUsesLong) {
			this.longValues = 0L;
			for (int i = 0; i < conf.recordGroupByteLength; i++) {
				longValues <<= 8;
				longValues |= (values[i] & 255L);
			}
		} else
			this.values = new BigInteger(1, values);
	}

	/**
	 * @param num
	 *            The index of the desired record
	 * @param r
	 *            The Record to store in.
	 */
	public void getRecord(int num, Record r) {
		long val;
		if (conf.recordGroupUsesLong) {
			val = longValues / conf.longMultipliers[num] % conf.totalStates;
		} else {
			val = values.divide(conf.multipliers[num]).mod(
					conf.bigIntTotalStates).longValue();
		}
		r.set(val);
	}

	/**
	 * Copies group to this RecordGroup
	 * 
	 * @param group
	 *            The group to copy
	 */
	public void set(RecordGroup group) {
		if (conf.recordGroupUsesLong)
			longValues = group.longValues;
		else
			values = group.values;
	}

	/**
	 * Sets this RecordGroup to recs
	 * 
	 * @param recs
	 *            The records to use
	 * @param offset
	 *            The offset into recs
	 */
	public void set(Record[] recs, int offset) {
		if (conf.recordGroupUsesLong) {
			this.longValues = 0L;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				longValues += recs[offset++].getState()
						* conf.longMultipliers[i];
		} else {
			values = BigInteger.ZERO;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				values = values.add(BigInteger.valueOf(
						recs[offset++].getState())
						.multiply(conf.multipliers[i]));
		}
	}

	/**
	 * Outputs the bytes from this RecordGroup into output
	 * 
	 * @param output
	 *            A ByteBuffer to output to
	 */
	public void outputUnsignedBytes(ByteBuffer output) {
		if (conf.recordGroupUsesLong) {
			for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
				output.put((byte) (longValues >>> i));
		} else
			values.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}

	/**
	 * Outputs the bytes from this RecordGroup into byteArray
	 * 
	 * @param byteArray
	 *            The byte array to output to
	 * @param offset
	 *            The offset into byteArray
	 */
	public void toUnsignedByteArray(byte[] byteArray, int offset) {
		if (conf.recordGroupUsesLong) {
			long inValues = longValues;
			for (int i = offset + conf.recordGroupByteLength - 1; i >= offset; i--) {
				byteArray[i] = (byte) inValues;
				inValues >>>= 8;
			}
		} else
			values.toUnsignedByteArray(byteArray, offset,
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
	public void outputUnsignedBytes(DataOutput output) throws IOException {
		if (conf.recordGroupUsesLong) {
			for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
				output.write((int) (longValues >>> i));
		} else
			values.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}

	/**
	 * Outputs the bytes from this RecordGroup into output
	 * 
	 * @param output
	 *            A MemoryDatabase to output to
	 * @param offset
	 *            The offset into the MemoryDatabase
	 */
	public void writeToUnsignedMemoryDatabase(MemoryDatabase output, long offset) {
		if (conf.recordGroupUsesLong) {
			long inValues = longValues;
			for (long i = offset + conf.recordGroupByteLength - 1; i >= offset; i--) {
				output.putByte(i, (byte) inValues);
				inValues >>>= 8;
			}
		} else
			values.writeToUnsignedMemoryDatabase(output, offset,
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
	public void outputUnsignedBytes(OutputStream output) throws IOException {
		if (conf.recordGroupUsesLong) {
			for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
				output.write((int) (longValues >>> i));
		} else
			values.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @return The number of bytes used by a RecordGroup object with this
	 *         configuration
	 */
	public static int byteSize(Configuration conf) {
		return 24 + (conf.recordGroupUsesLong ? 0
				: (56 + (conf.recordGroupByteLength + 7) / 8 * 8));
	}
}
