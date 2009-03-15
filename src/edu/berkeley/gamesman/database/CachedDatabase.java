package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Date;
import java.util.Random;
import java.util.TreeMap;

import edu.berkeley.gamesman.core.Configuration;

/**
 * TODO: Description and Javadoc
 *  
 * @author Alex Trofimov
 * @version 1.0
 * TODO: add concurrency control for multiple threads
 * TODO: add support for breaking the DB into multiple files (say no file > 1GB)
 * TODO: allow blocks to be unsorted on disk (good for non-tiered solvers)
 * TODO: make it so that it prefetches some blocks.
 */
public class CachedDatabase extends MemoryDatabase {

	private static int cacheSizeInMB = 5;  	 	// self-explanatory.
	//private static int cacheSize = 8192;  	 	// self-explanatory.
	private static int pageSize = 4096; 		// Default Block Size = 4k.
	private int bufferPoolSize;			 		// Number of pages that should fit into the cache.
	
	private boolean[] dirtyBit;			 		// The array of dirty bits.
	private boolean[] notUseful;				// Whether this has been marked for deletion.
	private byte[] pinBytes;			 		// Count of who is using the bit
	private byte[][] bufferPool;		 		// Page Pool of size bufferPoolSize x blockSize.
	private TreeMap<Integer, Integer> blockIDs;	// Array of Block IDs in Page Pool.
	private int[] blockIDsArray;				// - || -
	private int clockPointer;			 		// Pointer for the next available buffer.
	
	private RandomAccessFile fileDescriptor;	// The file descriptor to this database
	
	/* Some statistical variables */
	public int readHitCount;
	public int totalReadCount;
	public int writeHitCount;
	public int totalWriteCount;
	
	/** 
	 * Default constructor. For testing purposes only!!!
	 */
	public CachedDatabase(String filename, Configuration conf) {
		this.conf = conf;
			this.initialize(filename);
		//else super(filename, conf);
	}
	
	@Override
	public void initialize(String locations) {
		// Initializing Variables
		bufferPoolSize = cacheSizeInMB * 1024 * 1024 / pageSize;
		//bufferPoolSize = cacheSize / pageSize;
		clockPointer = 0;
		open = true;
		totalReadCount = 0;
		totalWriteCount = 0;
		readHitCount = 0;
		writeHitCount = 0;
		
		// Initializing the storage file
		try {
			File filePointer = new File(locations);
			fileDescriptor = new RandomAccessFile(filePointer, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace(); System.exit(0);
		} catch (IOException e) {
			e.printStackTrace(); System.exit(0);
		} catch (Exception e) {
			e.printStackTrace(); System.exit(0);
		}
				
		// Initializing (big) internal variables
		this.dirtyBit = new boolean[bufferPoolSize];
		//for (int i = 0; i < bufferPoolSize; i ++ ) dirtyBit[i] = false;
		this.pinBytes = new byte[bufferPoolSize];
		//for (int i = 0; i < bufferPoolSize; i ++ ) pinBytes[i] = 0;
		this.bufferPool = new byte[bufferPoolSize][pageSize];
		blockIDsArray = new int[bufferPoolSize];
		this.blockIDs = new TreeMap<Integer, Integer>();
		this.notUseful = new boolean[bufferPoolSize];
		System.out.println("Buffer Pool Size: " + bufferPoolSize);
	}

	@Override
	public void flush() {
		for (int i = 0; i < this.bufferPoolSize; i ++) {
			if (dirtyBit[i])
				this.unloadPage(i);
		}
		try {
			//fileDescriptor.getChannel().force(true);
			fileDescriptor.getFD().sync();
		} catch (IOException e) {
			e.printStackTrace(); System.exit(0);
		}
	}
	
	@Override
	protected void ensureCapacity(long numBytes) {
		try {
			if (fileDescriptor.length() < numBytes)
				fileDescriptor.setLength(numBytes);
			//fileDescriptor.seek(numBytes);
			//fileDescriptor.write(0);
		} catch (IOException e) {
			e.printStackTrace(); System.exit(0);
		}
	}
	
	@Override
	public void close() {
		try {
			this.flush();
			//fileDescriptor.getChannel().force(true);
			fileDescriptor.getChannel().close();
			fileDescriptor.close();
			this.open = false;
		} catch (IOException e) {
			e.printStackTrace(); System.exit(0);
		} catch (Exception e) {
			e.printStackTrace(); System.exit(0);
		}
	}
	
	@Override
	protected byte get(long index) {
		totalReadCount ++;
		int blockID = (int) (index / pageSize);
		int byteOffset = (int) index % pageSize;
		int pageID;
		
		// First check cache
		if (blockIDs.containsKey(blockID)) {
			readHitCount ++;
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
		totalWriteCount ++;
		int blockID = (int) (index / pageSize);
		int byteOffset = (int) index % pageSize;
		int pageID;
		
		// Check if the page is in cache
		if (blockIDs.containsKey(blockID)) {
			writeHitCount ++;
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
		return clockPointer;
	}
	
	/** print the staticstis */
	public void printStats() {
		System.out.printf("+---------------+Statistics+----------------+\n");
		float rhr = (readHitCount == 0) ? 0.0f : new Float(this.readHitCount * 100) / (totalReadCount);
		System.out.printf("+ Read Hit Rate:     %5.2f%% out of %-8d +\n", rhr, totalReadCount);
		float whr = (writeHitCount == 0) ? 0.0f : new Float(this.writeHitCount * 100) / (totalWriteCount);
		System.out.printf("+ Write Hit Rate:    %5.2f%% out of %-8d +\n", whr, totalWriteCount);		
		System.out.printf("+-------------------------------------------+\n");
	}
	
	/** reset the statistics */
	public void resetStats() {
		totalReadCount = 0;
		totalWriteCount = 0;
		writeHitCount = 0;
		readHitCount = 0;
	}
	
	@Override
	public int getSize() {
		return -1; //TODO: FIXME
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
		System.out.println("Cache Size: " + cacheSizeInMB + "MB.");
		//System.out.println("Cache Size: " + cacheSize / 1024 / 1024 + "MB.");

		/* Defining Variables */
		long startTime = (new Date()).getTime();
		Random random = new Random();
		int testSize = 11000;
		int bitSize = 8; // Should be an odd number > 64 to test longs.
		BigInteger[] BigInts = new BigInteger[testSize];
		String filename = "CacheDB.TestFile.db";
		CachedDatabase DB = new CachedDatabase(filename, null);
		
		/* Generating Random Numbers */
		int bc;
		for (int i = 0; i < testSize; i ++) {
			BigInts[i] = new BigInteger(bitSize, random);
			bc = BigInts[i].bitLength();
			if (bc < bitSize)
				BigInts[i] = BigInts[i].shiftLeft(bitSize - bc); }
		
		/* Writing numbers to database */
		for (int i = 0; i < testSize; i ++) {
			DB.putBits(i, bitSize, BigInts[i]);	}
		
		DB.close();
		DB.printStats();
		DB.initialize(filename);
		
		/* Testing that it was written correctly */
		BigInteger temp;
		int pass = 0;
		int fail = 0;
		for (int i = 0; i < testSize; i ++) {
			temp = DB.getBits(i, bitSize);
			if (temp.toString(2).equals(BigInts[i].toString(2)))
				pass ++;
			else
				fail ++;
		}
		long endTime = (new Date()).getTime() - startTime;
		System.out.printf("Testing Complete in %d milli-seconds.\n", endTime);
		System.out.printf("%d tests passed. %d tests failed.\n", pass, fail);
		System.out.printf("Datbase size: %d bytes.\n", DB.getSize());
		DB.printStats();
		
		
	}

}
