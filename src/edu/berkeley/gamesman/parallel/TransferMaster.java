package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.database.SplitDatabase;
import edu.berkeley.gamesman.util.ChunkInputStream;
import edu.berkeley.gamesman.util.ErrorThread;
import edu.berkeley.gamesman.util.UndeterminedChunkInputStream;

public final class TransferMaster {
	public static void main(String[] args) throws IOException {
		SplitDatabase readFrom = SplitDatabase.openSplitDatabase(args[0]);
		Configuration conf = readFrom.getConfiguration();
		SplitDatabase writeTo = SplitDatabase.openSplitDatabase(args[1], conf,
				true, true);
		int numSplits = Integer.parseInt(args[2]);
		int entrySize = conf.getInteger("gamesman.db.zip.entryKB", 64) << 10;
		int maxLen = 0;
		final long hashes = conf.getGame().numHashes();
		long lastForDb = 0;
		String parentFile = args[3];
		for (int i = 0; i < numSplits; i++) {
			long firstForDb = lastForDb;
			lastForDb = (i + 1) * hashes / numSplits;
			String dbUri = parentFile + File.separator + "s" + firstForDb
					+ ".db";
			GZippedFileDatabase db = new GZippedFileDatabase(dbUri, conf,
					readFrom.getHeader(firstForDb, lastForDb - firstForDb));
			Scanner dbScan = new Scanner(readFrom.makeStream(firstForDb,
					lastForDb - firstForDb));
			String nextType = dbScan.next();
			while (!nextType.equals("end")) {
				String uri = dbScan.next();
				dbScan.nextLong();
				dbScan.nextLong();
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
				command += TransferSlave.class.getName() + " " + file + " "
						+ firstForDb + " " + (lastForDb - firstForDb);
				System.out.println(command);
				Process p = Runtime.getRuntime().exec(command);
				new ErrorThread(p.getErrorStream(), host).start();
				PrintStream ps = new PrintStream(p.getOutputStream(), true);
				InputStream in = p.getInputStream();
				UndeterminedChunkInputStream ucis = new UndeterminedChunkInputStream(
						in);
				Scanner readScan = new Scanner(ucis);
				String neededChunk = readScan.next();
				while (!(neededChunk.equals("ready") || neededChunk
						.equals("skip"))) {
					if (neededChunk.equals("fetch:")) {
						long firstRequestedRecord = readScan.nextLong();
						long numRequestedRecords = readScan.nextLong();
						ps.println(readFrom.makeStream(firstRequestedRecord,
								numRequestedRecords));
					} else
						System.out.println(host + ": " + neededChunk + " "
								+ readScan.nextLine());
					neededChunk = readScan.next();
				}
				if (neededChunk.equals("ready")) {
					ucis.finish();
					ps.println("go");
					ChunkInputStream cis = new ChunkInputStream(in);
					db.putZippedBytes(cis);
				}
				nextType = dbScan.next();
			}
			writeTo.addDb(GZippedFileDatabase.class.getName(), dbUri,
					firstForDb, lastForDb - firstForDb);
			db.close();
		}
		readFrom.close();
		writeTo.close();
	}
}
