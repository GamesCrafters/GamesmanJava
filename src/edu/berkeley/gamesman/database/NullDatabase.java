package edu.berkeley.gamesman.database;

import java.util.Iterator;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * The NullDatabase is a database that simply throws away its results and
 * returns bogus records when you query it.
 * 
 * @author Steven Schlansker
 */
public final class NullDatabase extends Database {

	@Override
	public void close() {
	}

	@Override
	public void flush() {
	}

	@Override
	public RecordGroup getRecordGroup(long loc) {
		return new RecordGroup(conf, BigInteger.ZERO);
	}

	@Override
	public void initialize(String url) {
		// Util.warn("Using NullDatabase, answers will be incorrect and nothing will be saved.");
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup value) {
	}
	
	@Override
	public Iterator<RecordGroup> getRecordGroups(long startLoc, int numGroups) {
		return new NullRecordGroupIterator(numGroups);
	}
	
	public class NullRecordGroupIterator implements Iterator<RecordGroup>{
		private int remainingGroups;
		
		private NullRecordGroupIterator(int len){
			remainingGroups=len-1;
		}
		
		public RecordGroup next(){
			remainingGroups--;
			return new RecordGroup(conf, BigInteger.ZERO);
		}

		public boolean hasNext() {
			return remainingGroups>=0;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public void putRecordGroups(long startLoc, Iterator<RecordGroup> rg, int numGroups){
	}

}
