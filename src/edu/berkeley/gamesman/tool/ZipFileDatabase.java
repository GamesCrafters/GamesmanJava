package edu.berkeley.gamesman.tool;

import java.io.*;

import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.database.GZippedFileDatabase;

/**
 * Converts a FileDatabase into a GZippedFileDatabase
 * 
 * @author dnspies
 */
public class ZipFileDatabase {
	/**
	 * @param args
	 *            The file name of the database to zip followed by the length of
	 *            each entry followed by the buffer length for the OutputStream
	 * @throws IOException
	 *             If an IOException is thrown
	 * @throws ClassNotFoundException
	 *             If loading the configuration object throws a
	 *             ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		FileDatabase readFrom = new FileDatabase();
		readFrom.initialize(args[0], false);
		File writeTo = new File(args[0] + ".gz");
		int bufferSize;
		long entrySize;
		if (args.length > 1) {
			entrySize = Long.parseLong(args[1]) << 10;
			if (args.length > 2)
				bufferSize = Integer.parseInt(args[2]) << 10;
			else
				bufferSize = 1 << 16;
		} else {
			entrySize = 1 << 16;
			bufferSize = 1 << 16;
		}
		GZippedFileDatabase.createFromFile(readFrom, writeTo, true, entrySize,
				bufferSize);
	}
}
