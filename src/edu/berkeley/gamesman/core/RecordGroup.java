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
	 * @param conf The configuration object.
	 * @param values The byte representation of this RecordGroup
	 */
	public RecordGroup(Configuration conf, byte[] values) {
		this(conf, new BigInteger(1, values));
	}

	/**
	 * @param conf The configuration object.
	 * @param values The big integer representation of this RecordGroup
	 */
	public RecordGroup(Configuration conf, BigInteger values) {
		this.conf = conf;
		this.values = values;
	}
	
	public RecordGroup(Configuration conf, Record[] recs, int offset){
		BigInteger multiplier=BigInteger.ONE;
		this.conf=conf;
		values=BigInteger.ZERO;
		for(int i=0;i<conf.recordsPerGroup;i++){
			values=values.add(recs[offset++].getState().multiply(multiplier));
			multiplier=multiplier.multiply(conf.totalStates);
		}
	}

	public RecordGroup(Configuration conf, Iterator<Record> recordIterator) {
		BigInteger multiplier=BigInteger.ONE;
		this.conf=conf;
		values=BigInteger.ZERO;
		for(int i=0;i<conf.recordsPerGroup;i++){
			values=values.add(recordIterator.next().getState().multiply(multiplier));
			multiplier=multiplier.multiply(conf.totalStates);
		}
	}

	/**
	 * @param num The index of the desired record
	 * @return The record
	 */
	public Record getRecord(int num) {
		BigInteger divideOut = conf.totalStates.pow(num);
		BigInteger val = values.divide(divideOut).mod(conf.totalStates);
		return new Record(conf, val);
	}
	
	public void getRecords(Record[] recs, int offset){
		BigInteger[] remainingValues={values};
		for(int i=0;i<conf.recordsPerGroup;i++){
			remainingValues=remainingValues[0].divideAndRemainder(conf.totalStates);
			recs[offset++]=new Record(conf, remainingValues[1]);
		}
	}

	/**
	 * Changes a single record in the group.
	 * 
	 * @param num The record to change
	 * @param r The values to change it to
	 */
	public void setRecord(int num, Record r) {
		BigInteger multiplier = conf.totalStates.pow(num);
		BigInteger zeroOut = multiplier.multiply(conf.totalStates);
		values = values.subtract(
				values.mod(zeroOut).subtract(values.mod(multiplier))).add(
				r.getState().multiply(multiplier));
	}

	/**
	 * @return A BigInteger containing all the information about every record in
	 *         this group.
	 */
	public BigInteger getState() {
		return values;
	}

	/**
	 * @param values The new value to give this RecordGroup
	 */
	public void setValue(byte[] values) {
		this.values = new BigInteger(1, values);
	}
}
