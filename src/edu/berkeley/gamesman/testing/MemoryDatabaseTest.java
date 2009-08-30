//package edu.berkeley.gamesman.testing;
//
//import java.io.File;
//import java.math.BigInteger;
//import java.util.Date;
//import java.util.Random;
//
//import edu.berkeley.gamesman.core.Record;
//import edu.berkeley.gamesman.database.CachedDatabase;
//import edu.berkeley.gamesman.database.MemoryDatabase;
//import edu.berkeley.gamesman.util.Util;
//
///** Class to test Memory Database */
//public class MemoryDatabaseTest {
//	
//	/** Some Internal Variables */
//	static int numThreadsRunning = 0;
//	MemoryDatabase DB;
//	
//	/** Testing Memory Database Entry Point
//	 * @param args - set args[0] to test
//	 */
//	public static void main(String[] args) {
//		long startTime = (new Date()).getTime();
//		
//		if (args.length < 3) {
//			System.out.printf("Usage: MemoryDatabaseTest [TestNum] [fileName] [MemDBClassName]\n" +
//							  " - eg: MemoryDatabaseTest 2 TestDB.db MemoryDatabase\n");
//			System.exit(1);
//		}
//		int testNum = Integer.parseInt(args[0]);
//		
//		String filename = args[1];
//		File testFile = new File(filename);
//		if (testFile.exists()) testFile.delete();
//		MemoryDatabaseTest Test = new MemoryDatabaseTest();
//		
//		String className = "edu.berkeley.gamesman.database." + args[2]; 
//		try {
//			Class<?> DBClass = Class.forName(className);
//			Test.DB = (MemoryDatabase) DBClass.newInstance();
//			Test.DB.initialize(filename);
//			System.out.printf("Initializing %s\n", className);
//		} catch (ClassNotFoundException e) {
//			System.err.printf("Class '%s' not found.\n", className);
//			System.exit(3);
//		} catch (Exception e) {
//			System.err.printf("Some class cast exception with '%s': %s", className, e.getMessage());
//			System.exit(3);
//		}
//		
//		switch(testNum) {
//		case 1:	Test.TestParallel();	break;
//		case 2: Test.TestSpace(); 		break;
//		default: System.err.printf("Test #'%s' is not defined.\n", args[0]);
//		}
//		
//		long DBSize = Test.DB.getSize();
//		Test.DB.close();
//		long totalTime = (new Date()).getTime() - startTime;
//		System.out.printf("Testing Complete in %d milli-seconds. Size of DB: %s\n", totalTime, Util.bytesToString(DBSize));
//		System.exit(0);
//	}
//	
//	
//	/** Testing parallel access to MemDB */
//	synchronized public void TestParallel() {
//		int testSize = 10000;
//		int [] bitSizes = new int[] { 57, 63, 32, 30, 29, 28, 60, 43, 32, 31, 9, 8, 7, 5, 3, 2, 1 };
//		int bitSize = 0;
//		for (Integer i : bitSizes) bitSize += i;
//		int numThreads = Runtime.getRuntime().availableProcessors();
//		MemoryDatabaseTest.numThreadsRunning = numThreads;
//		System.out.printf("Running Parallel Test. %d Records (%d bits), %d Threads.\n", testSize, bitSize, numThreads);
//		
//
//		DB.ensureCapacity((testSize * bitSize + 7) >> 3);
//		for (int i = 0; i < numThreads; i ++) {
//			new MDBTester(i, testSize / numThreads, bitSizes, bitSize, i, numThreads, this);
//		}
//		
//		while (MemoryDatabaseTest.numThreadsRunning > 0)
//			try {
//				wait(10);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//				System.exit(2);
//			}
//		
//	}
//	
//	/** Test usage of large space requirements */
//	public void TestSpace() {
//		/* Defining Variables */
//		Random random = new Random();
//		int bloatedness = 4000; //40000000; // use this and test size 25, to make 10GB file.
//		int testSize = 25;
//		int [] bitSizes = new int[] { 57, 63, 32, 30, 29, 28, 60, 43, 32, 31, 9, 8, 7, 5, 3, 2, 1 };
//		int bitSize = 0;
//		for (int i : bitSizes) bitSize += i; 
//		BigInteger[] BigInts = new BigInteger[testSize];
//		
//		
//		/* Generating Random Numbers */
//		int bc;
//		for (int i = 0; i < testSize; i ++) {
//			BigInts[i] = new BigInteger(bitSize, random);
//			bc = BigInts[i].bitLength();
//			if (bc < bitSize)
//				BigInts[i] = BigInts[i].shiftLeft(bitSize - bc); }
//		
//		/* Putting them into records */
//		edu.berkeley.gamesman.core.Record[] Records = new edu.berkeley.gamesman.core.Record[testSize];
//		for (int i = 0; i < testSize; i ++) {
//			Records[i] = new edu.berkeley.gamesman.core.Record(bitSizes);
//			Records[i].loadBigInteger(BigInts[i]);
//		}
//		
//		/* Writing numbers to database in random order */
//		java.util.TreeSet<Integer> visited = new java.util.TreeSet<Integer>();
//		java.util.Random randomness = new java.util.Random();
//		int index;
//		for (int i = 0; i < testSize; i ++) {
//			index = randomness.nextInt(testSize);
//			while (visited.contains(index))
//				index = (index + 1) % testSize;
//			DB.putRecord(i * bloatedness, Records[i]);
//			visited.add(index);
//		}
//		
//		/* Testing that it was written correctly */
//		BigInteger temp;
//		int pass = 0;
//		int fail = 0;
//		for (int i = 0; i < testSize; i ++) {
//			temp = DB.getRecord(i * bloatedness, Records[i]).toBigInteger();
//			if (temp.toString(2).equals(BigInts[i].toString(2))) pass ++;
//			else fail ++;
//		}
//		
//		System.out.printf("%d tests passed. %d tests failed.\n", pass, fail);
//	}
//	
//	class MDBTester extends Thread {
//		
//		int id;
//		int testSize;
//		int bitSize;
//		int offset;
//		int[] bitSizes;
//		int skip;
//		MemoryDatabaseTest Test; 
//		
//		/** Public Constructor
//		 * @param id - this thread's number
//		 * @param testSize - how many samples
//		 * @param bitSizes - values of the field sizes in bits 
//		 * @param bitSize - how big is each sample
//		 * @param offset - how many samples to skip in DB
//		 * @param skip - how far the records are spaced
//		 * @param Test - the database to use
//		 */
//		public MDBTester(int id, int testSize, int[] bitSizes, int bitSize, int offset, int skip, MemoryDatabaseTest Test) {
//			super("Thread-" + id);
//			this.id = id;
//			this.testSize = testSize;
//			this.bitSizes = bitSizes;
//			this.bitSize = bitSize;
//			this.offset = offset;
//			this.skip = skip;
//			this.Test = Test;
//			this.start();
//		}
//		
//		/** The Running **/
//		public void run() {
//			Record[] testSet = generateRandomRecords(testSize, bitSize, bitSizes);
//			Test.putBigIntArrayRandSeq(testSet, offset, skip);
//			int failed = Test.testDBContent(testSet, offset, skip, bitSizes);
//			System.out.printf("%d: Result: %d out of %d tests failed.\n", id, failed, testSize);
//			MemoryDatabaseTest.numThreadsRunning --;
//		}
//	}
//	
//	/** Make a Random BigInts array
//	 * @param testSize - size of the array
//	 * @param bitSize - bit length of each element
//	 * @return it
//	 */
//	public static BigInteger[] generateRandomBigInts(int testSize, int bitSize) {
//		BigInteger[] BigInts = new BigInteger[testSize];
//		Random random = new Random();
//		int bc;
//		for (int i = 0; i < testSize; i ++) {
//			BigInts[i] = new BigInteger(bitSize, random);
//			bc = BigInts[i].bitLength();
//			if (bc < bitSize)
//				BigInts[i] = BigInts[i].shiftLeft(bitSize - bc); }
//		return BigInts;
//	}
//	
//	/** Make Random Record array
//	 * @param testSize - size of the array
//	 * @param bitSize = bit length of each record
//	 * @param bitSizes - sizes of the fields in bits
//	 * @return it
//	 */
//	public static Record[] generateRandomRecords(int testSize, int bitSize, int[] bitSizes) {
//		BigInteger[] BigInts = generateRandomBigInts(testSize, bitSize);
//		Record[] Records = new Record[testSize];
//		for (int i = 0; i < testSize; i ++) {
//			Records[i] = new Record(bitSizes);
//			Records[i].loadBigInteger(BigInts[i]);
//		}
//		return Records;
//	}
//	
//	/**
//	 * Populate this DB with BigInts
//	 * @param Records - array of values to be stored into the DB
//	 * @param offset - from which RecordID should the count off start
//	 * @param skip - what intervals the records are spread in
//	 */
//	public void putBigIntArrayRandSeq(Record[] Records, int offset, int skip) {
//		int testSize = Records.length;
//		boolean[] visited = new boolean[testSize];
//		Random random = new Random();
//		int index;
//		for (int i = 0; i < testSize; i ++) {
//			index = random.nextInt(testSize);
//			while (visited[index])
//				index = (index + 1) % testSize;
//			DB.putRecord(index * skip + offset, Records[index]);
//			visited[index] = true;
//		}
//	}
//	
//	/** Compare the contents of DB to BigInts[]
//	 * @param Records - the array that should be in this
//	 * @return - # of records that aren't in DB, or stored incorrectly
//	 * @param offset - where to start reading
//	 * @param skip - how far the records are spread
//	 */
//	public int testDBContent(Record[] Records, int offset, int skip, int[] bitSizes) {
//		int testSize = Records.length;		
//		int bitSize = Records[0].bitlength();
//		Record tempRecord = new Record(bitSizes);
//		int pass = 0;
//		int fail = 0;
//		for (int i = 0; i < testSize; i ++) {
//			DB.getRecord(i * skip + offset, tempRecord);
//			if (tempRecord.toBigInteger().toString(2).equals(Records[i].toBigInteger().toString(2))) 
//				pass ++;
//			else 
//				fail ++;
//		}
//		return fail;
//	}
//	
//}
