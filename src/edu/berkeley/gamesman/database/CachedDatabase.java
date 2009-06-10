package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.TreeMap;

import edu.berkeley.gamesman.util.Util;

/**
 * @author Alex Trofimov
 * @version 1.4
 * 
 * ChangeLog:
 * 05/06/09 - 1.4 - getSize() fixed. Block pre-loading added.
 * 05/05/09 - 1.3 - synchronized keywords added. Tested, works in parallel, 2 Cores => 40% increase
 * 05/04/09 - 1.2 - BlockID converted from Integer to Long, for creation of files > 1GB.
 * 03/18/09 - 1.1 - Switched added to disable flush()ing when still open.
 * 03/15/09 - 1.0 - Initial Release
 * 
 * TODO: add support for breaking the DB into multiple files (say no file > 1GB)
 * TODO: allow blocks to be unsorted on disk (good for non-tiered solvers)
 * TODO: add compression to save space.
 * TODO: for random block allocation and compression, make an index file?
 */
public class CachedDatabase extends MemoryDatabase {

	/** Total size in bytes that cache will occupy in RAM */
	public static long cacheSize = 1024 * 1024 * 40;
	/** Size of a basic block that this DB will work with */
	public static int pageSize = 1024 * 1024 * 2; 	// Default Block Size = 4k.
	
	/** Set this to False so that flush() only works at close() */
	public static boolean flushWhenOpen = false;
	
	/** The number of pages that should be pre-loaded on each load */
	public static int preLoad = 1; // set to 0 to disable pre-loading.
	
	private int bufferPoolSize;			 		// Number of pages that should fit into the cache.
	private boolean[] dirtyBit;			 		// The array of dirty bits.
	private boolean[] notUseful;				// Whether this has been marked for deletion.
	private byte[] pinBytes;			 		// Count of who is using the bit
	private byte[][] bufferPool;		 		// Page Pool of size bufferPoolSize x blockSize.
	private TreeMap<Integer, Integer> blockIDs;	// Array of Block IDs in Page Pool.
	private long[] blockIDsArray;				// - || -
	private int clockPointer;			 		// Pointer for the next available buffer.
	
	private RandomAccessFile fileDescriptor;	// The file descriptor to this database
	
	private long lastBlockID;					// ID of the block last referenced.
	
	/* Some statistical variables */
	static enum Stats { HIT_READS, TOTAL_READS, HIT_WRITES,  TOTAL_WRITES,
		PAGE_LOADS, PAGE_WRITES, FLUSHES, PAGES };
	private int[] Statistics;
	
	
	@Override
	synchronized public void initialize(String filePath) {
		// Initializing Variables
		bufferPoolSize = (int) ((cacheSize + pageSize - 1)/ pageSize); // ceil
		open = true;	
		dirtyBit = new boolean[bufferPoolSize];
		pinBytes = new byte[bufferPoolSize];
		bufferPool = new byte[bufferPoolSize][pageSize];
		blockIDsArray = new long[bufferPoolSize];
		blockIDs = new TreeMap<Integer, Integer>();
		notUseful = new boolean[bufferPoolSize];
		reset(); // some initialization happens there
		
		// Initializing the storage file
		try {
			File filePointer = new File(filePath);
			fileDescriptor = new RandomAccessFile(filePointer, "rw");
			fileDescriptor.setLength(getByteSize());
		} catch (FileNotFoundException e) {
			e.printStackTrace(); System.exit(0);
		} catch (Exception e) {
			e.printStackTrace(); System.exit(0);
		}
		rawRecord = new byte[conf.recordGroupByteLength];
	}
	
	/** Reset all the data stored in cache. */
	synchronized public void reset() {
		synchronized (blockIDs) {
			blockIDs.clear();
			for (int i = 0; i < bufferPoolSize; i ++) blockIDsArray[i] = -1;
			clockPointer = 0;
			lastBlockID = -1;
			Statistics = new int[Stats.values().length];
		}
	}
	

	@Override
	synchronized public void flush() {
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
	synchronized public void close() {
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
	synchronized protected byte getByte(long index) {
		int blockID = (int) (index / pageSize);
		if (lastBlockID != blockID) Statistics[Stats.TOTAL_READS.ordinal()] ++;
		int byteOffset = (int) ((int) index % pageSize);
		int pageID;
		
		// First check cache
		if (blockIDs.containsKey(blockID)) {
			if (lastBlockID != blockID) Statistics[Stats.HIT_READS.ordinal()] ++;
			pageID = blockIDs.get(blockID);
		} else { // Then check disk
			pageID = cacheBlock(blockID);
		}
		notUseful[pageID] = false;
		lastBlockID = blockID;
		return this.bufferPool[pageID][byteOffset];
	}

	@Override
	synchronized protected void putByte(long index, byte data) {
		long blockID = index / pageSize;
		if (lastBlockID != blockID) Statistics[Stats.TOTAL_WRITES.ordinal()] ++;
		int byteOffset = (int) (index % pageSize);
		int pageID;
		
		// Check if the page is in cache		
		if (blockIDs.containsKey((int) blockID)) {
			if (lastBlockID != blockID) Statistics[Stats.HIT_WRITES.ordinal()] ++;
			pageID = blockIDs.get((int) blockID);
		} else { // Have to go to disk
			pageID = cacheBlock(blockID);
		}
		notUseful[pageID] = false;
		dirtyBit[pageID] = true;
		lastBlockID = blockID;
		bufferPool[pageID][byteOffset] = data;
	}
	
	/** Load a page identified by 'blockID', and 'preLoad' consecutive pages 
	 * @param blockID - the ID of the block being loaded first
	 * @return pageID - the ID of the page where blockID is located 
	 */
	protected int cacheBlock(long blockID) {		
		int iPageID = -1, pageID;
		for (int i = 0; i < preLoad; i ++) {
			pageID = getFreePage();
			if (i == 0) iPageID = pageID;
			loadPage(blockID + i, pageID);
		}
		assert iPageID > -1;
		return iPageID;
	}
	
	
	/**
	 * Read one page from disk and put it into cache.
	 * Size of the page is specified by pageSize
	 * @param blockID - ID of the block (page) on disk that is to be read.
	 */
	protected void loadPage(long blockID, int pageID) {
		assert !(dirtyBit[pageID] || blockIDs.containsKey(blockID));
		Statistics[Stats.PAGE_LOADS.ordinal()] ++;
		if (this.blockIDsArray[pageID] >= 0)  // There's a non-dirty page in there.
			blockIDs.remove((int) blockIDsArray[pageID]); // Let it know that it's a goner
		else Statistics[Stats.PAGES.ordinal()] ++; // This pageID wasn't used before.
		try {
			fileDescriptor.seek(blockID * pageSize);
			fileDescriptor.read(bufferPool[pageID]);
		} catch (IOException e) {
			e.printStackTrace(); System.exit(1);
		}
		blockIDs.put((int) blockID, pageID); 
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
		long blockID = blockIDsArray[pageID];
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
		System.out.printf("+ Read Hit Rate:  %6.2f%% out of %4s pages  +\n", rhr, Util.bytesToString(totalReadCount));
		float whr = (writeHitCount == 0) ? 0.0f : new Float(writeHitCount * 100) / (totalWriteCount);
		System.out.printf("+ Write Hit Rate: %6.2f%% out of %4s pages  +\n", whr, Util.bytesToString(totalWriteCount));		
		System.out.printf("+ Cache/Page = Pages:  %5s /%5s = %5d  +\n", Util.bytesToString(cacheSize), Util.bytesToString(pageSize), bufferPoolSize);
		System.out.printf("+ I/O Read:      %8d pages = %5s      +\n", pageLoads, Util.bytesToString(pageLoads * pageSize));
		System.out.printf("+ I/O Write:     %8d pages = %5s      +\n", pageWrites, Util.bytesToString(pageWrites * pageSize));
		System.out.printf("+ Pages Used/Total:    %8d/%-8d     +\n", Statistics[Stats.PAGES.ordinal()], bufferPoolSize);
		System.out.printf("+ Flush() Call Count:   %8d             +\n", Statistics[Stats.FLUSHES.ordinal()]);
		System.out.printf("+ Capacity:                %6s            +\n", Util.bytesToString(getByteSize()));
		System.out.printf("+--------------------------------------------+\n");
	}
}
