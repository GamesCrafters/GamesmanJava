package edu.berkeley.gamesman.core;

import edu.berkeley.gamesman.util.Util;

/**
 * Describes the possible 'columns' that can appear in one of the Databases
 * The parameter is the (optional) default bit size of the field.  (-1 means no default)
 * @author Steven Schlansker
 */
public enum RecordFields {
	/**
	 * The value of the position
	 * @see PrimitiveValue
	 */
	VALUE(4),
	/**
	 * How many moves to the closest ending state
	 */
	REMOTENESS(1<<6),
	/**
	 * The best possible ending position you can get.
	 * @see Game#primitiveScore
	 */
	SCORE(1<<4);
	
	
	private int bs;
	private RecordFields(int bs){
		this.bs = bs;
	}
	
	/**
	 * @return the default size in bits of this field
	 */
	public int defaultNumberOfStates(){
		if(bs != -1)
			return bs;
		Util.fatalError("Tried to get default possibilities of field without a default");
		return 0;
	}
}
