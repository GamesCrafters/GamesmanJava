package edu.berkeley.gamesman.core;

/**
 * Stores information about a game state
 * 
 * @author dnspies
 */
public final class Record implements Cloneable, Comparable<Record> {
	private final Configuration conf;

	/**
	 * The value of this record
	 */
	public Value value = Value.UNDECIDED;

	/**
	 * The remoteness of this record
	 */
	public int remoteness = 0;

	/**
	 * The score of this record
	 */
	public int score = 0;

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
	 * adding one to the remoteness.
	 */
	public void previousPosition() {
		if (conf.hasValue)
			value = value.flipValue();
		if (conf.hasRemoteness)
			++remoteness;
	}

	@Override
	public boolean equals(Object r) {
		if (r instanceof Record) {
			Record rec = (Record) r;
			return (!conf.hasValue || value == rec.value)
					&& (!conf.hasRemoteness || remoteness == rec.remoteness || (conf.hasValue && !value.hasRemoteness))
					&& (!conf.hasScore || score == rec.score);
		} else
			return false;
	}

	@Override
	public String toString() {
		String s;
		if (conf.hasValue) {
			s = value.name();
		} else
			s = "Finish";
		if (conf.hasRemoteness && (!conf.hasValue || value.hasRemoteness)) {
			s += " in " + remoteness;
		}
		if (conf.hasScore)
			s += " with score " + score;
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
	 * subtracting one from the remoteness.
	 */
	public void nextPosition() {
		value = value.flipValue();
		--remoteness;
	}

	@Override
	public int compareTo(Record other) {
		if (conf.hasValue) {
			int compare = value.compareTo(other.value);
			if (compare != 0)
				return compare;
		}
		if (conf.hasScore)
			if (score > other.score)
				return 1;
			else if (score < other.score)
				return -1;
		if (conf.hasRemoteness)
			if (!conf.hasValue || value.compareTo(Value.DRAW) > 0) {
				if (remoteness < other.remoteness)
					return 1;
				else if (remoteness > other.remoteness)
					return -1;
			} else if (Value.DRAW.compareTo(value) > 0)
				if (remoteness > other.remoteness)
					return 1;
				else if (remoteness < other.remoteness)
					return -1;
		return 0;
	}

	/*
	 * public boolean compareTo(Record other){ if (other.value ==
	 * Value.IMPOSSIBLE){ return true; } else{ return isPreferableTo(other); }}
	 */

}
