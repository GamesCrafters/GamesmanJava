package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.util.LinkedList;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.database.GZippedFileDatabase;

/**
 * For zipping files
 * 
 * @author dnspies
 */
public class ZipFiles {
	private final Configuration conf;
	private final long entrySize;
	private final int bufferSize;
	private final LinkedList<String> paths = new LinkedList<String>();
	private final int numThreads;

	/**
	 * @param conf
	 *            The configuration object
	 * @param tier
	 *            The tier to zip in
	 * @param args
	 *            The job file followed by the files to be zipped
	 */
	public ZipFiles(Configuration conf, int tier, String[] args) {
		this.conf = conf;
		entrySize = conf.getLong("zip.entryKB", 1 << 6) << 10;
		bufferSize = conf.getInteger("zip.bufferKB", 1 << 6) << 10;
		String parentPath = conf.getProperty("gamesman.slaveDbFolder")
				+ File.separator + "t" + tier;
		numThreads = conf.getInteger("gamesman.threads", 1);
		for (int i = 2; i < args.length; i++)
			paths.add(parentPath + File.separator + "s" + args[i] + ".db");
	}

	private synchronized String getTask() {
		if (paths.isEmpty())
			return null;
		else
			return paths.removeFirst();
	}

	/**
	 * @param args
	 *            The configuration file followed by each of the files to be
	 *            zipped.
	 * @throws ClassNotFoundException
	 *             If instantiating the configuration throws a
	 *             ClassNotFoundException
	 */
	public static void main(String[] args) throws ClassNotFoundException {
		Properties props = Configuration.readProperties(args[0]);
		int tier = Integer.parseInt(args[1]);
		Configuration conf = new Configuration(props);
		ZipFiles zf = new ZipFiles(conf, tier, args);
		zf.startZip();
	}

	/**
	 * Begin zipping
	 */
	public void startZip() {
		for (int i = 0; i < numThreads; i++) {
			new Thread() {
				public void run() {
					String filePath = getTask();
					while (filePath != null) {
						FileDatabase readFrom = new FileDatabase();
						readFrom.setRange(0, new File(filePath).length() - 4);
						readFrom.initialize(filePath, conf, false);
						File writeTo = new File(filePath + ".gz");
						GZippedFileDatabase.createFromFile(readFrom, writeTo,
								false, entrySize, bufferSize);
						readFrom.myFile.delete();
						filePath = getTask();
					}
				}
			}.start();
		}
	}
}