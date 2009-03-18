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
	VALUE(2),
	/**
	 * How many moves to the closest ending state
	 */
	REMOTENESS(6),
	SCORE(6);
	
	
	private int bs;
	private RecordFields(int bs){
		this.bs = bs;
	}
	
	/**
	 * @return the default size in bits of this field
	 */
	public int defaultBitSize(){
		if(bs != -1)
			return bs;
		Util.fatalError("Tried to get default bit size of field without a default");
		return 0;
	}
}
