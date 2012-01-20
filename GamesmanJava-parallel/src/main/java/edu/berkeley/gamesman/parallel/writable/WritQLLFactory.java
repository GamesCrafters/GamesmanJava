package edu.berkeley.gamesman.parallel.writable;

import org.apache.hadoop.io.Writable;

import edu.berkeley.gamesman.util.qll.Factory;
import edu.berkeley.gamesman.util.qll.Pool;
import edu.berkeley.gamesman.util.qll.QLLFactory;

public class WritQLLFactory<T extends Writable> {
	private final QLLFactory<T> myFact;
	private final Pool<T> myPool;

	public WritQLLFactory(Factory<T> fact) {
		myFact = new QLLFactory<T>();
		myPool = new Pool<T>(fact);
	}

	public WritableQLL<T> getList() {
		return new WritableQLL<T>(myFact, myPool);
	}
}
