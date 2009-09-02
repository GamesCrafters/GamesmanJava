package edu.berkeley.gamesman.core;

import java.util.Iterator;

import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * @author dnspies Stores a small group of records in the most compressed
 *         possible format.
 */
public class RecordGroup {
	protected final Configuration conf;

	private BigInteger values;

	/**
	 * @param conf
	 *            The configuration object.
	 * @param values
	 *            The byte representation of this RecordGroup
	 */
	public RecordGroup(Configuration conf, byte[] values) {
		this(conf, new BigInteger(1, values));
	}

	/**
	 * @param conf
	 *            The configuration object.
	 * @param values
	 *            The big integer representation of this RecordGroup
	 */
	public RecordGroup(Configuration conf, BigInteger values) {
		this.conf = conf;
		this.values = values;
	}

	public RecordGroup(Configuration conf, Record[] recs, int offset) {
		this.conf = conf;
		values = BigInteger.ZERO;
		for (int i = 0; i < conf.recordsPerGroup; i++)
			values = values.add(BigInteger.valueOf(recs[offset++].getState())
					.multiply(conf.multipliers[i]));
	}

	public RecordGroup(Configuration conf, Iterator<Record> recordIterator) {
		this.conf = conf;
		values = BigInteger.ZERO;
		for (int i = 0; i < conf.recordsPerGroup; i++) {
			values = values.add(BigInteger.valueOf(
					recordIterator.next().getState()).multiply(
					conf.multipliers[i]));
		}
	}

	public RecordGroup(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * @param num
	 *            The index of the desired record
	 * @return The record
	 */
	public Record getRecord(int num) {
		long val = values.divide(conf.multipliers[num]).mod(
				conf.bigIntTotalStates).longValue();
		return new Record(conf, val);
	}

	public void getRecords(Record[] recs, int offset) {
		BigInteger remainingValues = values;
		for (int i = 0; i < conf.recordsPerGroup; i++) {
			long mod = remainingValues.mod(conf.bigIntTotalStates).longValue();
			remainingValues = remainingValues.divide(conf.bigIntTotalStates);
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
	public void setRecord(int num, Record r) {
		BigInteger multiplier = conf.multipliers[num];
		BigInteger zeroOut = conf.multipliers[num + 1];
		values = values.subtract(
				values.mod(zeroOut).subtract(values.mod(multiplier))).add(
				BigInteger.valueOf(r.getState()).multiply(multiplier));
	}

	/**
	 * @return A BigInteger containing all the information about every record in
	 *         this group.
	 */
	public BigInteger getState() {
		return values;
	}

	/**
	 * @param values
	 *            The new value to give this RecordGroup
	 */
	public void setValue(byte[] values) {
		this.values = new BigInteger(1, values);
	}

	/**
	 * @param num
	 *            The index of the desired record
	 * @param r
	 *            The Record to store in.
	 */
	public void getRecord(int num, Record r) {
		long val = values.divide(conf.multipliers[num]).mod(
				conf.bigIntTotalStates).longValue();
		r.set(val);
	}

	public void set(RecordGroup group) {
		values = group.values;
	}

	public void set(Record[] recs) {
		values = BigInteger.ZERO;
		for (int i = 0; i < conf.recordsPerGroup; i++)
			values = values.add(BigInteger.valueOf(recs[i].getState())
					.multiply(conf.multipliers[i]));
	}
}
