package edu.berkeley.gamesman.tool;

import java.io.*;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
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
		File readFromFile = new File(args[0]);
		InputStream is = new FileInputStream(readFromFile);
		int confLength = 0;
		for (int i = 0; i < 4; i++) {
			confLength <<= 8;
			confLength |= is.read();
		}
		byte[] confBytes = new byte[confLength];
		is.read(confBytes);
		Configuration conf = Configuration.load(confBytes);
		is.close();
		Database readFrom = conf.openDatabase(false);
		File writeTo = new File(args[1]);
		int bufferSize;
		long entrySize;
		if (args.length > 2) {
			entrySize = Long.parseLong(args[2]) << 10;
			if (args.length > 3)
				bufferSize = Integer.parseInt(args[3]) << 10;
			else
				bufferSize = 1 << 16;
		} else {
			entrySize = 1 << 16;
			bufferSize = 1 << 16;
		}
		GZippedFileDatabase.createFromFile(readFrom, writeTo, true, entrySize,
				bufferSize, System.out);
	}
}
