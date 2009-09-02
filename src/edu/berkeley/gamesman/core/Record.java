package edu.berkeley.gamesman.core;

import java.util.EnumMap;
import java.util.Map.Entry;

/**
 * Stores information about a game state
 * 
 * @author dnspies
 */
public final class Record {
	private final EnumMap<RecordFields, Integer> values;

	private final EnumMap<RecordFields, Integer> numStates;

	Record(Configuration conf, long state) {
		numStates = conf.storedFields;
		values = new EnumMap<RecordFields, Integer>(RecordFields.class);
		long remainingState = state;
		for (Entry<RecordFields, Integer> e : numStates.entrySet()) {
			int val = e.getValue();
			this.values.put(e.getKey(), (int) (remainingState % val));
			remainingState /= val;
		}
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param values
	 *            The values of each of the respective fields in this record
	 *            ordered VALUE, REMOTENESS, SCORE
	 */
	public Record(Configuration conf, int... values) {
		numStates = conf.storedFields;
		this.values = new EnumMap<RecordFields, Integer>(RecordFields.class);
		int i = 0;
		for (Entry<RecordFields, Integer> e : numStates.entrySet())
			this.values.put(e.getKey(), values[i++]);
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param pVal
	 *            Just the primitive value. All other fields are initialized to
	 *            zero.
	 */
	public Record(Configuration conf, PrimitiveValue pVal) {
		this.values = new EnumMap<RecordFields, Integer>(RecordFields.class);
		numStates = conf.storedFields;
		for (Entry<RecordFields, Integer> e : numStates.entrySet())
			this.values.put(e.getKey(), 0);
		this.values.put(RecordFields.VALUE, pVal.value());
	}

	/**
	 * Creates an empty record that can be written to.
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public Record(Configuration conf) {
		numStates = conf.storedFields;
		values = new EnumMap<RecordFields, Integer>(RecordFields.class);
	}

	/**
	 * @param field
	 *            The field to change
	 * @param value
	 *            The new value of the field
	 */
	public void set(RecordFields field, int value) {
		values.put(field, value);
	}

	public void set(Record record) {
		values.putAll(record.values);
	}

	/**
	 * @param rf
	 *            The type of one block of information
	 * @return The information encoded as a long
	 */
	public int get(RecordFields rf) {
		return values.get(rf);
	}

	/**
	 * @return The primitive value of this position
	 */
	public PrimitiveValue get() {
		return PrimitiveValue.values()[(int) get(RecordFields.VALUE)];
	}

	/**
	 * @return The integer value of this record
	 */
	public long getState() {
		long currentState = 0;
		long multiplier = 1;
		for (Entry<RecordFields, Integer> e : values.entrySet()) {
			currentState += e.getValue() * multiplier;
			multiplier *= numStates.get(e.getKey());
		}
		return currentState;
	}

	/**
	 * Changes this record to the previous position. WARNING! Does not change
	 * the score. You must do that yourself.
	 */
	public void previousPosition() {
		set(RecordFields.VALUE, get().previousMovesValue().value());
		if (values.containsKey(RecordFields.REMOTENESS))
			set(RecordFields.REMOTENESS, get(RecordFields.REMOTENESS) + 1);
	}

	@Override
	public boolean equals(Object r) {
		if (r instanceof Record) {
			Record rec = (Record) r;
			return values.equals(rec.values);
		} else
			return false;
	}

	@Override
	public String toString() {
		return values.toString();
	}

	private Record(Record record) {
		this.values = new EnumMap<RecordFields, Integer>(RecordFields.class);
		this.values.putAll(record.values);
		numStates = record.numStates;
	}

	@Override
	public Record clone() {
		return new Record(this);
	}

	public void set(long state) {
		long remainingState = state;
		for (Entry<RecordFields, Integer> e : numStates.entrySet()) {
			int val = e.getValue();
			this.values.put(e.getKey(), (int) (remainingState % val));
			remainingState /= val;
		}
	}

	public void reset() {
		values.clear();
	}

	public boolean contains(RecordFields rf) {
		return values.containsKey(rf);
	}
}
