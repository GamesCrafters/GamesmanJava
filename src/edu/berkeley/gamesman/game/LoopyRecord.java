package edu.berkeley.gamesman.game;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;

public class LoopyRecord extends Record {
	public LoopyRecord(Configuration conf) {
		super(conf);
	}

	int remainingChildren;
}
