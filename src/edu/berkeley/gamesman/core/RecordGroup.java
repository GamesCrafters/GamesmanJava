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

	private final boolean usesLong;

	/**
	 * @param conf
	 *            The configuration object.
	 * @param values
	 *            The byte representation of this RecordGroup
	 */
	public RecordGroup(Configuration conf, byte[] values) {
		this.conf = conf;
		usesLong = conf.recordGroupByteLength < 8;
		if (usesLong) {
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
		usesLong = conf.recordGroupByteLength < 8;
		if (usesLong)
			this.longValues = values.longValue();
		else
			this.values = values;
	}

	public RecordGroup(Configuration conf, Record[] recs, int offset) {
		this.conf = conf;
		usesLong = conf.recordGroupByteLength < 8;
		if (usesLong) {
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

	public RecordGroup(Configuration conf, Iterator<Record> recordIterator) {
		this.conf = conf;
		usesLong = conf.recordGroupByteLength < 8;
		if (usesLong) {
			this.longValues = 0;
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

	public RecordGroup(Configuration conf) {
		this.conf = conf;
		usesLong = conf.recordGroupByteLength < 8;
	}

	/**
	 * @param num
	 *            The index of the desired record
	 * @return The record
	 */
	public Record getRecord(int num) {
		long val;
		if (usesLong) {
			val = longValues / conf.longMultipliers[num] % conf.totalStates;
		} else {
			val = values.divide(conf.multipliers[num]).mod(
					conf.bigIntTotalStates).longValue();
		}
		return new Record(conf, val);
	}

	public void getRecords(Record[] recs, int offset) {
		if (usesLong) {
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
		if (usesLong) {
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
		if (usesLong) {
			this.longValues = 0;
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
		if (usesLong) {
			val = longValues / conf.longMultipliers[num] % conf.totalStates;
		} else {
			val = values.divide(conf.multipliers[num]).mod(
					conf.bigIntTotalStates).longValue();
		}
		r.set(val);
	}

	public void set(RecordGroup group) {
		if (usesLong)
			longValues = group.longValues;
		else
			values = group.values;
	}

	public void set(Record[] recs) {
		if (usesLong) {
			this.longValues = 0;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				longValues += recs[i].getState() * conf.longMultipliers[i];
		} else {
			values = BigInteger.ZERO;
			for (int i = 0; i < conf.recordsPerGroup; i++)
				values = values.add(BigInteger.valueOf(recs[i].getState())
						.multiply(conf.multipliers[i]));
		}
	}

	public void outputUnsignedBytes(ByteBuffer output) {
		if (usesLong) {
			for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
				output.put((byte) (longValues >>> i));
		} else
			values.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}

	public void toUnsignedByteArray(byte[] byteArray, int offset) {
		if (usesLong) {
			long inValues = longValues;
			for (int i = offset + conf.recordGroupByteLength - 1; i >= offset; i--) {
				byteArray[i] = (byte) inValues;
				inValues >>>= 8;
			}
		} else
			values.toUnsignedByteArray(byteArray, offset,
					conf.recordGroupByteLength);
	}

	public void outputUnsignedBytes(DataOutput output) throws IOException {
		if (usesLong) {
			for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
				output.write((int) (longValues >>> i));
		} else
			values.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}

	public void writeToUnsignedMemoryDatabase(MemoryDatabase output, long offset) {
		if (usesLong) {
			long inValues = longValues;
			for (long i = offset + conf.recordGroupByteLength - 1; i >= offset; i--) {
				output.putByte(i, (byte) inValues);
				inValues >>>= 8;
			}
		} else
			values.writeToUnsignedMemoryDatabase(output, offset,
					conf.recordGroupByteLength);
	}

	public void outputUnsignedBytes(OutputStream output) throws IOException {
		if (usesLong) {
			for (int i = (conf.recordGroupByteLength - 1) * 8; i >= 0; i -= 8)
				output.write((int) (longValues >>> i));
		} else
			values.outputUnsignedBytes(output, conf.recordGroupByteLength);
	}
}
