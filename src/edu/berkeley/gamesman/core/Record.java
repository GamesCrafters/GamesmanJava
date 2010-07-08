package edu.berkeley.gamesman.core;

/**
 * Stores information about a game state
 * 
 * @author dnspies
 */
public final class Record implements Cloneable {
	private final Configuration conf;

	/**
	 * The value of this record
	 */
	public Value value;

	/**
	 * The remoteness of this record
	 */
	public int remoteness;

	/**
	 * The score of this record
	 */
	public int score;

	/**
	 * Creates an empty record that can be written to.
	 * 
	 * @param conf
	 *            The configuration object
	 */
	public Record(Configuration conf) {
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
	 * Changes this record to the previous position by flipping the value and
	 * adding one to the remoteness. WARNING! Does not change the score. You
	 * must do that yourself.
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
	 * Changes this record to the next position by flipping the value and
	 * subtracting one from the remoteness. WARNING! Does not change the score.
	 * You must do that yourself.
	 */
	public void nextPosition() {
		value = value.flipValue();
		--remoteness;
	}
}
