package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.GZippedFileDatabase;

public class ReadZippedBytes {
	public static void main(String[] args) throws ClassNotFoundException,
			IOException {
		Properties props = Configuration.readProperties(args[0]);
		int tier = Integer.parseInt(args[1]);
		Configuration conf = new Configuration(props);
		long file = Long.parseLong(args[2]);
		long loc = Long.parseLong(args[3]);
		long length = Long.parseLong(args[4]);
		GZippedFileDatabase gzfd = new GZippedFileDatabase();
		gzfd.initialize(conf.getProperty("gamesman.slaveDbFolder")
				+ File.separator + "t" + tier + File.separator + "s" + file
				+ ".db.gz", conf, false);
		DatabaseHandle dh = gzfd.getHandle();
		byte[] tempArray = new byte[conf.getInteger("zip.bufferKB", 1 << 6) << 10];
		while (length > tempArray.length) {
			gzfd.getBytes(dh, loc - file, tempArray, 0, tempArray.length);
			System.out.write(tempArray);
			length -= tempArray.length;
			loc += tempArray.length;
		}
		gzfd.getBytes(dh, loc - file, tempArray, 0, (int) length);
		System.out.write(tempArray, 0, (int) length);
		System.out.println();
	}
}
