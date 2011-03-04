package edu.berkeley.gamesman.parallel.tier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import edu.berkeley.gamesman.core.WorkUnit;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHeader;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.database.SplitFileSystemDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.master.LocalMasterTextTask;
import edu.berkeley.gamesman.parallel.RangeFile;
import edu.berkeley.gamesman.solver.Solver;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.Task;
import edu.berkeley.gamesman.util.TaskFactory;
import edu.berkeley.gamesman.util.Util;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.core.Configuration;

public class HadoopTierMapper extends
		Mapper<Range, IntWritable, IntWritable, RangeFile> implements
		TaskFactory {
	private int tier;
	private TierSolver solver;
	private TierGame game;
	private String readUri;
	private Context currentContext;
	private final Random r = new Random();
	private IOException failure;

	Configuration conf;
	FileSystem fs;

	@Override
	public void setup(Context context) {
		try {
			Task.setTaskFactory(this);
			org.apache.hadoop.conf.Configuration conf = context
					.getConfiguration();
			this.conf = Configuration.deserialize(conf
					.get("gamesman.configuration"));
			game = (TierGame) this.conf.getGame();
			fs = FileSystem.get(conf);
			tier = conf.getInt("tier", -1);
			if (tier == -1)
				throw new Error("No tier specified");
			if (tier < game.numberOfTiers() - 1) {
				readUri = conf.get("gamesman.hadoop.lastTierDb");
			}
			String solverName = this.conf.getProperty("gamesman.solver");
			Class<? extends TierSolver> solverc = Util.typedForName(
					"edu.berkeley.gamesman.solver." + solverName, Solver.class)
					.asSubclass(TierSolver.class);
			try {
				solver = solverc.getConstructor(Configuration.class)
						.newInstance(this.conf);
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
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private class HadoopMapperRunnable implements Runnable {
		WorkUnit w;

		HadoopMapperRunnable(WorkUnit u) {
			w = u;
		}

		public void run() {
			w.conquer();
		}
	}

	@Override
	public void map(Range key, IntWritable value, Context context)
			throws IOException {
		synchronized (this) {
			currentContext = context;
		}
		long firstHash = key.firstRecord;
		long numHashes = key.numRecords;
		if (numHashes == 0)
			return;
		else if (numHashes < 0)
			throw new Error("Negative number of hashes");
		DatabaseHeader head = new DatabaseHeader(conf, firstHash, numHashes);
		int prevTier = tier + 1;
		SplitFileSystemDatabase readDb;
		if (prevTier < game.numberOfTiers()) {
			readDb = new SplitFileSystemDatabase(fs, readUri, firstHash,
					numHashes);
			solver.setReadDb(readDb);
		} else {
			readDb = null;
		}

		// setting write database file name, path etc and initializing it*******
		String foldUri = conf.getProperty("gamesman.hadoop.dbfolder");
		String uri = foldUri + File.separator + "s" + tier + "_" + firstHash
				+ ".db";
		String zippedURI = uri + "_local";
		String unzippedURI = zippedURI + ".uz_local";
		Database writeDb = Database.openDatabase(unzippedURI, conf, true, head);
		File localFile = new File(unzippedURI);
		File localParent = localFile.getParentFile();
		localParent.setWritable(true, false);
		localParent.setExecutable(true, false);
		localParent.setReadable(true, false);
		localFile.setWritable(true, false);
		localFile.setReadable(true, false);
		solver.setWriteDb(writeDb);
		// ***********************************************************************

		int threads = conf.getInteger("gamesman.threads", 1);
		Collection<WorkUnit> list;
		WorkUnit wu = solver.prepareSolve(conf, tier, firstHash, numHashes);
		if (threads > 1)
			list = wu.divide(threads);
		else {
			list = Collections.singleton(wu);
		}
		ArrayList<Thread> myThreads = new ArrayList<Thread>(list.size());
		ThreadGroup solverGroup = new ThreadGroup("Solver Group: "
				+ game.describe());
		for (WorkUnit w : list) {
			Thread t = new Thread(solverGroup, new HadoopMapperRunnable(w));
			t.start();
			myThreads.add(t);
		}
		for (Thread t : myThreads)
			while (t.isAlive())
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		if (readDb != null) {
			readDb.close();
		}
		long maxMem = conf.getLong("gamesman.memory", Integer.MAX_VALUE);
		Database readFrom = writeDb;
		GZippedFileDatabase writeTo;
		try {
			writeTo = new GZippedFileDatabase(zippedURI, conf, readFrom, maxMem);
			File localZipFile = new File(zippedURI);
			localZipFile.setWritable(true, false);
			localZipFile.setReadable(true, false);
		} catch (IOException e) {
			throw new Error(e);
		}
		Thread[] threadList = new Thread[threads];
		for (int i = 0; i < threads; i++) {
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
		writeTo.close();
		writeDb.close();
		new File(unzippedURI).delete();
		Path p;
		final Path pLocal;

		pLocal = new Path(zippedURI);
		final Path tempPath = new Path(uri + r.nextLong());
		p = new Path(uri);
		if (!fs.exists(p)) {
			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						fs.copyFromLocalFile(pLocal, tempPath);
						failure = null;
					} catch (IOException e) {
						failure = e;
					}
				}
			};
			t.start();
			context.progress();
			while (t.isAlive()) {
				try {
					t.join(60000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				context.progress();
			}
			if (failure != null)
				throw new Error(failure);
			fs.rename(tempPath, p);
		}
		new File(zippedURI).delete();
		FileStatus finalFile = fs.getFileStatus(p);
		try {
			context.write(new IntWritable(tier), new RangeFile(key, finalFile));
		} catch (InterruptedException e) {
			throw new Error(e);
		}
		synchronized (this) {
			currentContext = null;
		}
	}

	private class TierMapperTextTask extends LocalMasterTextTask {

		protected TierMapperTextTask(String name) {
			super(name);
		}

		@Override
		public void begin() {
			synchronized (HadoopTierMapper.this) {
				if (currentContext != null)
					currentContext.progress();
			}
			super.begin();
		}

		@Override
		public void complete() {
			synchronized (HadoopTierMapper.this) {
				if (currentContext != null)
					currentContext.progress();
			}
			super.complete();
		}

		@Override
		public void update() {
			synchronized (HadoopTierMapper.this) {
				if (currentContext != null)
					currentContext.progress();
			}
			super.update();
		}
	}

	@Override
	public Task createTask(String name) {
		return new TierMapperTextTask(name);
	}
}
