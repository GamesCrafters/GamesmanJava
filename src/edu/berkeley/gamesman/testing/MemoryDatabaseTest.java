package edu.berkeley.gamesman.testing;

import java.math.BigInteger;
import java.util.Date;
import java.util.Random;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.MemoryDatabase;
import edu.berkeley.gamesman.util.Util;

/** Class to test Memory Database */
public class MemoryDatabaseTest {
	
	/** Some Internal Variables */
	static int numThreadsRunning = 0;
	MemoryDatabase DB;
	
	/** Testing Memory Database Entry Point
	 * @param args - set args[0] to test
	 */
	public static void main(String[] args) {
		long startTime = (new Date()).getTime();
		
		if (args.length == 0) {
			System.out.printf("Usage: -[TestNum]\n");
			System.exit(1);
		}
		int testNum = Integer.parseInt(args[0]);
		
		MemoryDatabaseTest Test = new MemoryDatabaseTest();
		Test.DB = new MemoryDatabase();
		Test.DB.initialize("lmao");
		
		switch(testNum) {
		case 1:	Test.TestParallel();	break;
		default: System.err.printf("Test '%s' is not defined.\n", args[0]);
		}
		
		Test.DB.close();
		long totalTime = (new Date()).getTime() - startTime;
		System.out.printf("Testing Complete in %d milli-seconds.\n", totalTime);
		System.exit(0);
	}
	
	
	/** Testing parallel access to MemDB */
	synchronized public void TestParallel() {
		int testSize = 500000;
		int bitSize = 1;
		int numThreads = Runtime.getRuntime().availableProcessors() * 2;
		MemoryDatabaseTest.numThreadsRunning = numThreads;
		

		DB.ensureCapacity(testSize * bitSize + 1);
		for (int i = 0; i < numThreads; i ++) {
			new MDBTester(i, testSize / numThreads, bitSize, i, numThreads, this);
		}
		
		while (MemoryDatabaseTest.numThreadsRunning > 0)
			try {
				wait(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(2);
			}
		
	}
	
	class MDBTester extends Thread {
		
		int id;
		int testSize;
		int bitSize;
		int offset;
		int skip;
		MemoryDatabaseTest Test; 
		
		/** Public Constructor
		 * @param id - this thread's number
		 * @param testSize - how many samples
		 * @param bitSize - how big is each sample
		 * @param offset - how many samples to skip in DB
		 * @param skip - how far the records are spaced
		 * @param Test - the database to use
		 */
		public MDBTester(int id, int testSize, int bitSize, int offset, int skip, MemoryDatabaseTest Test) {
			super("Thread-" + id);
			this.id = id;
			this.testSize = testSize;
			this.bitSize = bitSize;
			this.offset = offset;
			this.skip = skip;
			this.Test = Test;
			this.start();
		}
		
		/** The Running **/
		public void run() {
			Record[] testSet = generateRandomRecords(testSize, bitSize);
			Test.putBigIntArrayRandSeq(testSet, offset, skip);
			int failed = Test.testDBContent(testSet, offset, skip);
			System.out.printf("%d: Result: %d out of %d tests failed.\n", id, failed, testSize);
			MemoryDatabaseTest.numThreadsRunning --;
		}
	}
	
	/** Make a Random BigInts array
	 * @param testSize - size of the array
	 * @param bitSize - bit length of each element
	 * @return it
	 */
	public static BigInteger[] generateRandomBigInts(int testSize, int bitSize) {
		BigInteger[] BigInts = new BigInteger[testSize];
		Random random = new Random();
		int bc;
		for (int i = 0; i < testSize; i ++) {
			BigInts[i] = new BigInteger(bitSize, random);
			bc = BigInts[i].bitLength();
			if (bc < bitSize)
				BigInts[i] = BigInts[i].shiftLeft(bitSize - bc); }
		return BigInts;
	}
	
	/** Make Random Record array
	 * @param testSize - size of the array
	 * @param bitSize = bit length of each record
	 * @return it
	 */
	public static Record[] generateRandomRecords(int testSize, int bitSize) {
		BigInteger[] BigInts = generateRandomBigInts(testSize, bitSize);
		Record[] Records = new Record[testSize];
		for (int i = 0; i < testSize; i ++) {
			Records[i] = new Record(new int[] { bitSize });
			Records[i].loadBigInteger(BigInts[i]);
		}
		return Records;
	}
	
	/**
	 * Populate this DB with BigInts
	 * @param Records - array of values to be stored into the DB
	 * @param offset - from which RecordID should the count off start
	 * @param skip - what intervals the records are spread in
	 */
	public void putBigIntArrayRandSeq(Record[] Records, int offset, int skip) {
		int testSize = Records.length;
		boolean[] visited = new boolean[testSize];
		Random random = new Random();
		int index;
		for (int i = 0; i < testSize; i ++) {
			index = random.nextInt(testSize);
			while (visited[index])
				index = (index + 1) % testSize;
			DB.putRecord(index * skip + offset, Records[index]);
			visited[index] = true;
		}
	}
	
	/** Compare the contents of DB to BigInts[]
	 * @param Records - the array that should be in this
	 * @return - # of records that aren't in DB, or stored incorrectly
	 * @param offset - where to start reading
	 * @param skip - how far the records are spread
	 */
	public int testDBContent(Record[] Records, int offset, int skip) {
		int testSize = Records.length;		
		int bitSize = Records[0].bitlength();
		Record tempRecord = new Record(new int[] { bitSize });
		int pass = 0;
		int fail = 0;
		for (int i = 0; i < testSize; i ++) {
			DB.getRecord(i * skip + offset, tempRecord);
			if (tempRecord.toBigInteger().toString(2).equals(Records[i].toBigInteger().toString(2))) 
				pass ++;
			else 
				fail ++;
		}
		return fail;
	}

}
