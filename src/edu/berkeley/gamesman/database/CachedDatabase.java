package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Util;

/**
 * TODO: Description and Javadoc
 *  
 * @author Alex Trofimov
 * @version 1.1
 * ChangeLog:
 * 1.1 - Switched added to disable flush()ing when still open.
 * 1.0 - Initial Release
 * TODO: add concurrency control for multiple threads
 * TODO: add support for breaking the DB into multiple files (say no file > 1GB)
 * TODO: fix getSize()
 * TODO: if the file does not exist, on reads, load zeros instead of reading empty space.
 * TODO: allow blocks to be unsorted on disk (good for non-tiered solvers)
 * TODO: make it so that it prefetches some blocks.
 * TODO: add compression to save space.
 */
public class CachedDatabase extends MemoryDatabase {

	/** Total size in bytes that cache will occupy in RAM */
	public static int cacheSize = 1024 * 1024 * 40; 
	/** Size of a basic block that this DB will work with */
	public static int pageSize = 1024 * 1024 * 2; 	// Default Block Size = 4k.
	
	/** Set this to False so that flush() only works at close() */
	public static boolean flushWhenOpen = false;
	
	private int bufferPoolSize;			 		// Number of pages that should fit into the cache.
	private boolean[] dirtyBit;			 		// The array of dirty bits.
	private boolean[] notUseful;				// Whether this has been marked for deletion.
	private byte[] pinBytes;			 		// Count of who is using the bit
	private byte[][] bufferPool;		 		// Page Pool of size bufferPoolSize x blockSize.
	private TreeMap<Integer, Integer> blockIDs;	// Array of Block IDs in Page Pool.
	private int[] blockIDsArray;				// - || -
	private int clockPointer;			 		// Pointer for the next available buffer.
	
	private RandomAccessFile fileDescriptor;	// The file descriptor to this database
	
	private int lastBlockID;					// ID of the block last referenced.
	
	/* Some statistical variables */
	static enum Stats { HIT_READS, TOTAL_READS, HIT_WRITES,  TOTAL_WRITES,
		PAGE_LOADS, PAGE_WRITES, FLUSHES, PAGES };
	private int[] Statistics;
	
	
	/** 
	 * Null Constructor... for testing purposes only.
	 */
	public CachedDatabase() {};
	

	@Override
	public void initialize(String filePath) {
		// Initializing Variables
		bufferPoolSize = (cacheSize + pageSize - 1)/ pageSize; // ceil
		open = true;	
		dirtyBit = new boolean[bufferPoolSize];
		pinBytes = new byte[bufferPoolSize];
		bufferPool = new byte[bufferPoolSize][pageSize];
		blockIDsArray = new int[bufferPoolSize];
		blockIDs = new TreeMap<Integer, Integer>();
		notUseful = new boolean[bufferPoolSize];
		reset(); // some initialization happens there
		
		// Initializing the storage file
		try {
			URI fileLocation = new URI(filePath);
			if (!fileLocation.isAbsolute()) {
				String currentDirectory = System.getProperty("user.dir").replace('\\', '/');
				currentDirectory = currentDirectory.replace(" ", "%20");
				fileLocation = new URI("file:///" + currentDirectory + "/" + filePath);
			}
			File filePointer = new File(fileLocation);
			fileDescriptor = new RandomAccessFile(filePointer, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace(); System.exit(0);
		} catch (Exception e) {
			e.printStackTrace(); System.exit(0);
		}

	}
	
	/** Reset all the data stored in cache. */
	public void reset() {
		Statistics = new int[Stats.values().length];
		blockIDs.clear();
		for (int i = 0; i < bufferPoolSize; i ++) blockIDsArray[i] = -1;
		clockPointer = 0;
		lastBlockID = -1;
		Statistics = new int[Stats.values().length];
	}
	

	@Override
	public void flush() {
		Statistics[Stats.FLUSHES.ordinal()] ++;
		if (!flushWhenOpen && open) return;
		// Flush any dirty buffers
		for (int i = 0; i < this.bufferPoolSize; i ++) {
			if (dirtyBit[i]) unloadPage(i); 	}
		// Sync with Disk
		try {
			fileDescriptor.getFD().sync();
		} catch (IOException e) {
			e.printStackTrace(); System.exit(0);
		}
	}
	
	@Override
	protected void ensureCapacity(long numBytes) {
		if (numBytes > capacity)
			capacity = numBytes;
		try {
			if (fileDescriptor.length() < numBytes)
				fileDescriptor.setLength(numBytes);
		} catch (IOException e) {
			e.printStackTrace(); System.exit(0);
		} 
	}
	
	@Override
	public void close() {
		try {
			open = false;
			flush();
			fileDescriptor.getChannel().close();
			fileDescriptor.close();			
			 printStats(); //not really necessary here
		} catch (IOException e) {
			e.printStackTrace(); System.exit(0);
		} catch (Exception e) {
			e.printStackTrace(); System.exit(0);
		}
	}
	
	@Override
	protected byte getByte(long index) {
		int blockID = (int) (index / pageSize);
		if (lastBlockID != blockID) Statistics[Stats.TOTAL_READS.ordinal()] ++;
		int byteOffset = (int) index % pageSize;
		int pageID;
		
		// First check cache
		if (blockIDs.containsKey(blockID)) {
			if (lastBlockID != blockID) Statistics[Stats.HIT_READS.ordinal()] ++;
			pageID = blockIDs.get(blockID);
		} else { // Then check disk
			pageID = getFreePage();
			loadPage(blockID, pageID);
		}

		notUseful[pageID] = false;
		lastBlockID = blockID;
		return this.bufferPool[pageID][byteOffset];
	}

	@Override
	protected void putByte(long index, byte data, boolean preserve) {
		int blockID = (int) (index / pageSize);
		if (lastBlockID != blockID) Statistics[Stats.TOTAL_WRITES.ordinal()] ++;
		int byteOffset = (int) index % pageSize;
		int pageID;
		
		// Check if the page is in cache
		if (blockIDs.containsKey(blockID)) {
			if (lastBlockID != blockID) Statistics[Stats.HIT_WRITES.ordinal()] ++;
			pageID = blockIDs.get(blockID);
		} else { // Have to go to disk
			pageID = getFreePage();
			loadPage(blockID, pageID);
		}
		
		notUseful[pageID] = false;
		dirtyBit[pageID] = true;
		lastBlockID = blockID;
		if (preserve) bufferPool[pageID][byteOffset] |= data;
		else bufferPool[pageID][byteOffset] = data;
	}
	
	/**
	 * Read one page from disk and put it into cache.
	 * Size of the page is specified by pageSize
	 * @param pageID - ID of the buffer where to store read data
	 * @param blockID - ID of the block (page) on disk that is to be read.
	 */
	protected void loadPage(int blockID, int pageID) {
		assert !(dirtyBit[pageID] || blockIDs.containsKey(blockID));
		Statistics[Stats.PAGE_LOADS.ordinal()] ++;
		if (this.blockIDsArray[pageID] >= 0)  // There's a non-dirty page in there.
			blockIDs.remove(blockIDsArray[pageID]); // Let it know that it's a goner
		else Statistics[Stats.PAGES.ordinal()] ++; // This pageID wasn't used before.
		if ((blockID + 1) * pageSize >= capacity) // if the data might not be there
			bufferPool[pageID] = new byte[pageSize]; // make sure to erase junk
		try {
			fileDescriptor.seek(blockID * pageSize);
			fileDescriptor.read(bufferPool[pageID]);
		} catch (IOException e) {
			e.printStackTrace(); System.exit(1);
		}
		blockIDs.put(blockID, pageID); 
		notUseful[pageID] = false;
		blockIDsArray[pageID] = blockID;
	}
	
	/**
	 * Take a page in cache, and write it to disk.
	 * @param pageID - ID of the page needs to be written to disk.
	 */
	protected void unloadPage(int pageID) {
		assert dirtyBit[pageID];
		Statistics[Stats.PAGE_WRITES.ordinal()] ++;
		int blockID = blockIDsArray[pageID];
		dirtyBit[pageID] = false;
		try {
			fileDescriptor.seek(blockID * pageSize);
			fileDescriptor.write(bufferPool[pageID]);
		} catch (IOException e) {
			e.printStackTrace(); System.exit(1);
		} 
	}
	
	/**
	 * Find an unpinned buffer and return it's id.
	 * If there are no clean buffers, write one of them to disk.
	 * The algorithm implemented here is Clock.
	 * @return the ID of a page that is not occupied
	 */
	protected int getFreePage() {
		int pass = 0;
		do {
			if (clockPointer == 0) pass ++;
			if (pass > 2) { // No unpinned buffers found
				System.err.println("No unpinned buffers found!"); System.exit(1);
			}
			notUseful[clockPointer] = true;
			clockPointer = (clockPointer + 1) % bufferPoolSize;
		} while (!(notUseful[clockPointer] && pinBytes[clockPointer] == 0));
		if (dirtyBit[clockPointer])
			unloadPage(clockPointer);
		return clockPointer;
	}
	
	/** print the statistics */
	public void printStats() {
		System.out.printf("+----------------+Statistics+----------------+\n");
		int readHitCount = Statistics[Stats.HIT_READS.ordinal()];
		int writeHitCount = Statistics[Stats.HIT_WRITES.ordinal()];
		int totalReadCount = Statistics[Stats.TOTAL_READS.ordinal()];
		int totalWriteCount = Statistics[Stats.TOTAL_WRITES.ordinal()];
		int pageLoads = Statistics[Stats.PAGE_LOADS.ordinal()];
		int pageWrites = Statistics[Stats.PAGE_WRITES.ordinal()];
		float rhr = (readHitCount == 0) ? 0.0f : new Float(readHitCount * 100) / (totalReadCount);
		System.out.printf("+ Read Hit Rate:  %6.2f%% out of %s pages  +\n", rhr, Util.bytesToString(totalReadCount));
		float whr = (writeHitCount == 0) ? 0.0f : new Float(writeHitCount * 100) / (totalWriteCount);
		System.out.printf("+ Write Hit Rate: %6.2f%% out of %s pages  +\n", whr, Util.bytesToString(totalWriteCount));		
		System.out.printf("+ Cache/Page=Pages:  %sb /%sb = %5d    +\n", Util.bytesToString(cacheSize), Util.bytesToString(pageSize), bufferPoolSize);
		System.out.printf("+ I/O Read:      %8d pages = %sb      +\n", pageLoads, Util.bytesToString(pageLoads * pageSize));
		System.out.printf("+ I/O Write:     %8d pages = %sb      +\n", pageWrites, Util.bytesToString(pageWrites * pageSize));
		System.out.printf("+ Pages Used/Total:    %8d/%-8d     +\n", Statistics[Stats.PAGES.ordinal()], bufferPoolSize);
		System.out.printf("+ Flush() Call Count:   %8d             +\n", Statistics[Stats.FLUSHES.ordinal()]);
		System.out.printf("+--------------------------------------------+\n");
	}
	
	@Override
	public long getSize() {
		return capacity;
	}
	
	/**
	 * Run a test to see if this DB is working.
	 * @param args - nothing
	 */
	public static void main(String[] args) {
		
		/* Printing some variables */
		Runtime thisMachine = Runtime.getRuntime();
		int procNum = thisMachine.availableProcessors();
		System.out.println("Available Processors: " + procNum);
		System.out.println("Free Memory:  " + Util.bytesToString(thisMachine.freeMemory()));
		System.out.println("Total Memory: " + Util.bytesToString(thisMachine.totalMemory()));
		
		
		/* Defining Variables */
		long startTime = (new Date()).getTime();
		Random random = new Random();
		int testSize = 250;//000;
		int [] bitSizes = new int[] { 57, 63, 32, 30, 29, 28, 60, 43, 32, 31, 9, 8, 7, 5, 3, 2, 1 };
		int bitSize = 0;
		for (int i : bitSizes) bitSize += i; 
		BigInteger[] BigInts = new BigInteger[testSize];
		
		
		/* Initializing the file */
		String filename = "CacheDB.TestFile.db";
		File testFile = new File(filename);
		if (testFile.exists()) testFile.delete();
		
		/* Initializing the Database */
		CachedDatabase DB = new CachedDatabase();
		DB.initialize(filename, null);
		
		/* Generating Random Numbers */
		int bc;
		for (int i = 0; i < testSize; i ++) {
			BigInts[i] = new BigInteger(bitSize, random);
			bc = BigInts[i].bitLength();
			if (bc < bitSize)
				BigInts[i] = BigInts[i].shiftLeft(bitSize - bc); }
		
		/* Putting them into records */
		edu.berkeley.gamesman.core.Record[] Records = new edu.berkeley.gamesman.core.Record[testSize];
		for (int i = 0; i < testSize; i ++) {
			Records[i] = new edu.berkeley.gamesman.core.Record(bitSizes);
			Records[i].loadBigInteger(BigInts[i]);
		}
		
		/* Writing numbers to database in random order */
		java.util.TreeSet<Integer> visited = new java.util.TreeSet<Integer>();
		java.util.Random randomness = new java.util.Random();
		int index;
		for (int i = 0; i < testSize; i ++) {
			index = randomness.nextInt(testSize);
			while (visited.contains(index))
				index = (index + 1) % testSize;
			DB.putRecord(i, Records[i]);
			visited.add(index);
		}
		
		/* Closing the database */
		//DB.close();
		//DB.printStats();
		//DB.initialize(filename);
		
		/* Testing that it was written correctly */
		BigInteger temp;
		int pass = 0;
		int fail = 0;
		for (int i = 0; i < testSize; i ++) {
			temp = DB.getRecord(i, Records[i]).toBigInteger();
			if (temp.toString(2).equals(BigInts[i].toString(2))) pass ++;
			else fail ++;
		}
		
		/* Closing the database and outputting results */
		DB.close();
		System.out.printf("%d tests passed. %d tests failed.\n", pass, fail);
		long endTime = (new Date()).getTime() - startTime;
		System.out.printf("Testing Complete in %s.\n", Util.millisToETA(endTime));
		
		
		
	}

}
