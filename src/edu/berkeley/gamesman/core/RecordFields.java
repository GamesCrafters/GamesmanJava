package edu.berkeley.gamesman.core;

import edu.berkeley.gamesman.util.Util;

public enum RecordFields {
	Value(2),
	Remoteness(-1);
	
	
	private int bs;
	private RecordFields(int bs){
		this.bs = bs;
	}
	
	public int defaultBitSize(){
		if(bs != -1)
			return bs;
		Util.fatalError("Tried to get default bit size of field without a default");
		return 0;
	}
}
