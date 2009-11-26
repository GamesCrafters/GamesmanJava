package edu.berkeley.gamesman.core;

/**
 * Stores information about a game state
 * 
 * @author dnspies
 */
public class Record {
	private final Configuration conf;

	public PrimitiveValue value;

	public int remoteness;

	public int score;

	protected Record(Configuration conf, long state) {
		this.conf = conf;
		int fieldStates = 1;
		if (conf.valueStates > 0) {
			fieldStates = conf.valueStates;
			value = PrimitiveValue.values[(int) (state % fieldStates)];
		}
		if (conf.remotenessStates > 0) {
			state /= fieldStates;
			fieldStates = conf.remotenessStates;
			remoteness = (int) (state % fieldStates);
		}
		if (conf.scoreStates > 0) {
			state /= fieldStates;
			score = (int) state;
		}
	}

	/**
	 * @param conf
	 *            The configuration object
	 * @param pVal
	 *            Just the primitive value. All other fields are initialized to
	 *            zero.
	 */
	protected Record(Configuration conf, PrimitiveValue pVal) {
		this.conf = conf;
		value = pVal;
	}

	/**
	 * Creates an empty record that can be written to.
	 * 
	 * @param conf
	 *            The configuration object
	 */
	protected Record(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * Sets the fields of this record to be the same as the passed record.
	 * 
	 * @param record
	 *            Another record
	 */
	public void set(Record record) {
		value = record.value;
		remoteness = record.remoteness;
		score = record.score;
	}

	/**
	 * @return The integer value of this record
	 */
	public long getState() {
		int fieldStates = 1;
		long totalState = 0;
		if (conf.scoreStates > 0) {
			totalState += score;
			fieldStates = conf.scoreStates;
		}
		if (conf.remotenessStates > 0) {
			totalState *= fieldStates;
			totalState += remoteness;
			fieldStates = conf.remotenessStates;
		}
		if (conf.valueStates > 0) {
			totalState *= fieldStates;
			totalState += value.value;
		}
		return totalState;
	}

	/**
	 * Changes this record to the previous position. WARNING! Does not change
	 * the score. You must do that yourself.
	 */
	public void previousPosition() {
		if (conf.valueStates > 0)
			value = value.flipValue();
		if (conf.remotenessStates > 0)
			++remoteness;
	}

	@Override
	public boolean equals(Object r) {
		if (r instanceof Record) {
			Record rec = (Record) r;
			return (conf.valueStates == 0 || value == rec.value)
					&& (conf.remotenessStates == 0 || remoteness == rec.remoteness)
					&& (conf.scoreStates == 0 || score == rec.score);
		} else
			return false;
	}

	@Override
	public String toString() {
		String s;
		if (conf.valueStates > 0)
			s = value.name();
		else
			s = "Finish";
		if (conf.remotenessStates > 0)
			return s + " in " + remoteness;
		else
			return s;
	}

	private Record(Record record) {
		this.conf = record.conf;
		value = record.value;
		remoteness = record.remoteness;
		score = record.score;
	}

	@Override
	public Record clone() {
		return new Record(this);
	}

	/**
	 * Derives all the fields from the passed long value
	 * 
	 * @param state
	 *            The state to derive the fields from
	 */
	public void set(long state) {
		int fieldStates = 1;
		if (conf.valueStates > 0) {
			fieldStates = conf.valueStates;
			value = PrimitiveValue.values[(int) (state % fieldStates)];
		}
		if (conf.remotenessStates > 0) {
			state /= fieldStates;
			fieldStates = conf.remotenessStates;
			remoteness = (int) (state % fieldStates);
		}
		if (conf.scoreStates > 0) {
			state /= fieldStates;
			score = (int) state;
		}
	}

	/**
	 * @param conf
	 *            a configuration object
	 * @return Determines the size (in bytes) of a record with a given
	 *         configuration object
	 */
	public static int byteSize(Configuration conf) {
		return 24;
	}

	public void nextPosition() {
		value = value.flipValue();
		--remoteness;
	}
}
