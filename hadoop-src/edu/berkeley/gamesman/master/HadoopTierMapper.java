package edu.berkeley.gamesman.master;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.DatabaseHeader;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.database.SplitFileSystemDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.solver.TierSolver;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.GamesmanConf;

public class HadoopTierMapper extends
		Mapper<Range, NullWritable, IntWritable, RangeFile> {
	private final RangeFile mapValue = new RangeFile();
	private final IntWritable tier = new IntWritable();
	private TierSolver solver;
	private TierGame game;
	private Database writeDb;
	private SplitFileSystemDatabase readDb;
	private boolean doZip;
	private String writeURI;

	Configuration conf;
	FileSystem fs;
	FSDataInputStream is;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration conf = context
					.getConfiguration();
			this.conf = new GamesmanConf(conf);
			game = (TierGame) this.conf.getGame();

			fs = FileSystem.get(conf);
			tier.set(conf.getInt("tier", -1));
			Class<? extends TierSolver> solverc = Class.forName(conf.get(""))
					.asSubclass(TierSolver.class);
			try {
				solver = solverc.getConstructor(Configuration.class)
						.newInstance(conf);
			} catch (IllegalArgumentException e) {
				throw new Error(e);
			} catch (SecurityException e) {
				throw new Error(e);
			} catch (InstantiationException e) {
				throw new Error(e);
			} catch (IllegalAccessException e) {
				throw new Error(e);
			} catch (InvocationTargetException e) {
				throw new Error(e.getCause());
			} catch (NoSuchMethodException e) {
				throw new Error(e);
			}
			doZip = conf.getBoolean("gamesman.remote.zipped", false);

		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	public void map(Range key, NullWritable value, Context context)
			throws IOException {
		long firstHash = key.firstRecord;
		long numHashes = key.numRecords;
		byte[] headBytes = new byte[18];
		for (int i = 0; i < 18; i++) {
			headBytes[i] = 0;
		}
		byte recordsPerGroup = (byte) conf.getInteger("recordsPerGroup", -1);
		byte recordGroupByteLength = (byte) conf.getInteger(
				"recordGroupByteLength", -1);
		String readUri = conf.getProperty("gamesman.hadoop.dbfolder");
		headBytes[16] = recordsPerGroup;
		headBytes[17] = recordGroupByteLength;
		DatabaseHeader head = new DatabaseHeader(conf, firstHash, numHashes);
		int prevTier = tier.get() + 1;
		readUri = readUri + "_" + prevTier + ".db";
		if (prevTier < game.numberOfTiers() - 1) {
			readDb = new SplitFileSystemDatabase(new Path(readUri), is, fs);
		} else {
			readDb = null;
		}

		// setting write database file name, path etc and initializing it*******
		String foldUri = conf.getProperty("gamesman.hadoop.dbfolder");
		doZip = conf.getBoolean("gamesman.hadoop.zipped", false);
		writeURI = foldUri + "_" + tier.get() + "_" + firstHash + ".db";
		String zipURI = null;
		if (doZip) {
			zipURI = writeURI;
			writeURI = writeURI + ".uz";
		}
		writeDb = Database.openDatabase(writeURI, conf, true, head);
		solver.setWriteDb(writeDb);
		// ***********************************************************************

		int t = tier.get();
		WorkUnit wu = solver.prepareSolve(conf, t, firstHash, numHashes);
		wu.conquer(); // solve it

		if (readDb != null) {
			readDb.close();
		}

		if (doZip) {

			int threads = conf.getInteger("gamesman.threads", 1);
			long time = System.currentTimeMillis();
			long maxMem = conf.getLong("gamesman.memory", Integer.MAX_VALUE);
			Database readFrom = writeDb;
			GZippedFileDatabase writeTo;
			try {
				writeTo = new GZippedFileDatabase(zipURI, conf, readFrom,
						maxMem);
			} catch (IOException e) {
				throw new Error(e);
			}
			Thread[] threadList = new Thread[threads];
			DatabaseHandle[] readHandle = new DatabaseHandle[threads];
			for (int i = 0; i < threads; i++) {
				readHandle[i] = readFrom.getHandle();
				threadList[i] = new Thread(writeTo);
				threadList[i].start();
			}
			for (int i = 0; i < threads; i++) {
				while (threadList[i].isAlive())
					try {
						threadList[i].join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

			}
		}

		long length = numHashes;
		boolean isdir = false;
		int block_replication = conf.getInteger("block_replication", 3);
		long blocksize = conf.getLong("blocksize", 64);
		// DONNO
		long modification_time = 0;

		Path path = new Path(writeURI);
		FileStatus finalFile = new FileStatus(length, isdir, block_replication,
				blocksize, modification_time, path);
		boolean successful = false;
		while (!successful) {
			try {
				mapValue.set(key, finalFile);
				context.write(tier, mapValue);
				successful = true;
			} catch (InterruptedException e) {
				successful = false;
				e.printStackTrace();
			}
		}
	}
}
