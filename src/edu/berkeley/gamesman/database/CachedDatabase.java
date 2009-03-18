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
 * TODO: allow blocks to be unsorted on disk (good for non-tiered solvers)
 * TODO: make it so that it prefetches some blocks.
 * TODO: add compression to save space.
 */
public class CachedDatabase extends MemoryDatabase {

	private static int cacheSize = 4096 * 456;	// Size of cache in bytes.
	private static int pageSize = 4096; 		// Default Block Size = 4k.
	private int bufferPoolSize;			 		// Number of pages that should fit into the cache.
	private static boolean flushWhenOpen = false;// Set this to False so that flush() only works at close();
	
	private boolean[] dirtyBit;			 		// The array of dirty bits.
	private boolean[] notUseful;				// Whether this has been marked for deletion.
	private byte[] pinBytes;			 		// Count of who is using the bit
	private byte[][] bufferPool;		 		// Page Pool of size bufferPoolSize x blockSize.
	private TreeMap<Integer, Integer> blockIDs;	// Array of Block IDs in Page Pool.
	private int[] blockIDsArray;				// - || -
	private int clockPointer;			 		// Pointer for the next available buffer.
	
	private RandomAccessFile fileDescriptor;	// The file descriptor to this database
	
	/* Some statistical variables */
	static enum Stats { HIT_READS, TOTAL_READS, HIT_WRITES,  TOTAL_WRITES,
		PAGE_LOADS, PAGE_WRITES, FLUSHES, PAGES };
	public int[] Statistics;
	
	
	/** 
	 * Null Constructor... why the hell do we need one???...
	 * TODO: Talk to someone about why this should exist....
	 */
	public CachedDatabase() {};
	
	/** 
	 * Default constructor. For testing purposes only!!!
	 */
	public CachedDatabase(String filePath, Configuration conf) {
		this.conf = conf;
		this.initialize(filePath);
	}
	
	@Override
	public void initialize(String filePath) {
		// Initializing Variables
		bufferPoolSize = (cacheSize + pageSize - 1)/ pageSize; // ceil
		clockPointer = 0;
		open = true;
		Statistics = new int[Stats.values().length];
		
		
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
				
		// Initializing (big) internal variables
		dirtyBit = new boolean[bufferPoolSize];
		pinBytes = new byte[bufferPoolSize];
		bufferPool = new byte[bufferPoolSize][pageSize];
		blockIDsArray = new int[bufferPoolSize];
		for (int i = 0; i < bufferPoolSize; i ++) blockIDsArray[i] = -1;
		blockIDs = new TreeMap<Integer, Integer>();
		notUseful = new boolean[bufferPoolSize];
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
			printStats();
		} catch (IOException e) {
			e.printStackTrace(); System.exit(0);
		} catch (Exception e) {
			e.printStackTrace(); System.exit(0);
		}
	}
	
	@Override
	protected byte get(long index) {
		Statistics[Stats.TOTAL_READS.ordinal()] ++;
		int blockID = (int) (index / pageSize);
		int byteOffset = (int) index % pageSize;
		int pageID;
		
		// First check cache
		if (blockIDs.containsKey(blockID)) {
			Statistics[Stats.HIT_READS.ordinal()] ++;
			pageID = blockIDs.get(blockID);
		} else { // Then check disk
			pageID = getFreePage();
			loadPage(blockID, pageID);
		}

		notUseful[pageID] = false;
		return this.bufferPool[pageID][byteOffset];
	}

	@Override
	protected void put(long index, byte data, boolean preserve) {
		Statistics[Stats.TOTAL_WRITES.ordinal()] ++;
		int blockID = (int) (index / pageSize);
		int byteOffset = (int) index % pageSize;
		int pageID;
		
		// Check if the page is in cache
		if (blockIDs.containsKey(blockID)) {
			Statistics[Stats.HIT_WRITES.ordinal()] ++;
			pageID = blockIDs.get(blockID);
		} else { // Have to go to disk
			pageID = getFreePage();
			loadPage(blockID, pageID);
		}
		
		notUseful[pageID] = false;
		dirtyBit[pageID] = true;
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
		/* while (true) {
			clockPointer = (clockPointer + 1) % bufferPoolSize;
			if (pinBytes[clockPointer] == 0 && !notUseful[clockPointer])
				notUseful[clockPointer] = true;
			else if (pinBytes[clockPointer] == 0 && notUseful[clockPointer]) 
				break;				
		} */
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
		float rhr = (readHitCount == 0) ? 0.0f : new Float(readHitCount * 100) / (totalReadCount);
		System.out.printf("+ Read Hit Rate:     %6.2f%% out of %-8d +\n", rhr, totalReadCount);
		float whr = (writeHitCount == 0) ? 0.0f : new Float(writeHitCount * 100) / (totalWriteCount);
		System.out.printf("+ Write Hit Rate:    %6.2f%% out of %-8d +\n", whr, totalWriteCount);		
		System.out.printf("+ I/O Page Load Count:  %8d             +\n", Statistics[Stats.PAGE_LOADS.ordinal()]);
		System.out.printf("+ I/O Page Write Count: %8d             +\n", Statistics[Stats.PAGE_WRITES.ordinal()]);
		System.out.printf("+ Pages in Memory:     %8d/%-8d     +\n", Statistics[Stats.PAGES.ordinal()], bufferPoolSize);
		System.out.printf("+ Flush() Call Count:   %8d             +\n", Statistics[Stats.FLUSHES.ordinal()]);
		System.out.printf("+--------------------------------------------+\n");
	}
	
	/** reset the statistics */
	public void resetStats() {
		Statistics = new int[Stats.values().length];
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
		System.out.println("Probing JVM...");
		Runtime thisMachine = Runtime.getRuntime();
		int procNum = thisMachine.availableProcessors();
		System.out.println("Available Processors: " + procNum);
		System.out.println("Free Memory:  " + thisMachine.freeMemory() / 1024 / 1024 + "MB.");
		System.out.println("Total Memory: " + thisMachine.totalMemory() / 1024 / 1024 + "MB.");
		//System.out.println("Cache Size: " + cacheSizeInMB + "MB.");
		System.out.println("Cache Size: " + cacheSize);
		

		/* Defining Variables */
		long startTime = (new Date()).getTime();
		Random random = new Random();
		int testSize = 250;//000;
		int [] bitSizes = new int[] { 63, 32, 30, 29, 28, 60, 43, 32, 31, 9, 8, 7, 5, 3, 2, 1 };
		int bitSize = 0;
		for (int i : bitSizes) bitSize += i; 
		BigInteger[] BigInts = new BigInteger[testSize];
		String filename = "CacheDB.TestFile.db";
		File testFile = new File(filename);
		if (testFile.exists()) {
			testFile.delete();
		}
		CachedDatabase DB = new CachedDatabase(filename, null);
		System.out.println("Number of Pages: " + DB.bufferPoolSize);
		
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
			//System.out.printf("Want File[%d] = %s\n", index, BigInts[index].toString(16));
			//DB.putBits(index, bitSize, BigInts[index]);
			DB.putRecord(i, Records[i]);
			visited.add(index);
		}
		
		DB.close();
		DB.printStats();
		DB.initialize(filename);
		
		/* Testing that it was written correctly */
		BigInteger temp;
		int pass = 0;
		int fail = 0;
		for (int i = 0; i < testSize; i ++) {
			temp = DB.getRecord(i, Records[i]).toBigInteger();
			//temp = DB.getBits(i, bitSize);
			//System.out.printf(" Reading %s\n", temp.toString(16));
			if (temp.toString(2).equals(BigInts[i].toString(2)))
				pass ++;
			else {
				fail ++;
			}
		}
		DB.close();
		DB.printStats();
		System.out.printf("Datbase size: %d bytes.\n", DB.getSize());
		System.out.printf("%d tests passed. %d tests failed.\n", pass, fail);
		long endTime = (new Date()).getTime() - startTime;
		System.out.printf("Testing Complete in %d milli-seconds.\n", endTime);
		
		
		
	}

}
