package edu.berkeley.gamesman.database;

import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.parallel.TierSlave;

public final class TransferMaster {
	// TODO Don't use the whole
	// "am I done yet?" thing, just do multiple calls to makeStream"
	public static void main(String[] args) {
		SplitDatabase readFrom = (SplitDatabase) Database.openDatabase(args[0]);
		Configuration conf = readFrom.getConfiguration();
		SplitDatabase writeTo = new SplitDatabase(args[1], conf);
		int numSplits = Integer.parseInt(args[2]);
		int entrySize = conf.getInteger("gamesman.db.zip.entryKB", 64) << 10;
		Scanner dbScan = new Scanner(readFrom.makeStream(0, conf.getGame()
				.numHashes()));
		int i = 0;
		int maxLen = 0;
		String nextType = dbScan.next();
		long firstForDb = 0;
		final long hashes = conf.getGame().numHashes();
		long lastForDb = hashes / numSplits;
		while (!nextType.equals("end")) {
			String uri = dbScan.next();
			long firstRecord = dbScan.nextLong();
			long numRecords = dbScan.nextLong();
			StringBuilder moveProc = new StringBuilder(maxLen);
			String[] hostFile = uri.split(":");
			String host = hostFile[0];
			String path = hostFile[1];
			String file = hostFile[2];
			if (!file.startsWith("/") && !file.startsWith(path))
				file = path + "/" + file;
			String user = null;
			if (host.contains("@")) {
				String[] userHost = host.split("@");
				user = userHost[0];
				host = userHost[1];
			}
			String command = "ssh "
					+ (user == null ? host : (user + "@" + host));
			command += " java -cp " + path + "/bin ";
			command += TransferSlave.class.getName() + " " + firstForDb + " "
					+ (lastForDb - firstForDb);
			// TODO Finish method
			nextType = dbScan.next();
		}
		readFrom.close();
	}
}
