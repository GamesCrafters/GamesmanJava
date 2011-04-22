package edu.berkeley.gamesman.parallel.tier;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Random;

import edu.berkeley.gamesman.DebugSetup;
import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.database.GZippedFileDatabase;
import edu.berkeley.gamesman.database.HDFSInfo;
import edu.berkeley.gamesman.database.HDFSSplitDatabase;
import edu.berkeley.gamesman.database.ReadWriteDatabase;
import edu.berkeley.gamesman.game.TierGame;
import edu.berkeley.gamesman.parallel.Range;
import edu.berkeley.gamesman.parallel.RangeFile;
import edu.berkeley.gamesman.solver.Solver;
import edu.berkeley.gamesman.solver.TierSolver;
import edu.berkeley.gamesman.util.Progressable;
import edu.berkeley.gamesman.util.Util;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import edu.berkeley.gamesman.core.Configuration;

/**
 * The mapper class for tier solves. Creates a database which merges the hadoop
 * SplitFileSystemDatabase for reading with a FileDatabase for writing. Then
 * solves the assigned portion. Then GZips to a GZippedFileDatabase. Uploads the
 * database to HDFS with a random long appended (to avoid conflicts in case the
 * same mapper is running in two places) and then renames to the correct file
 * name. Finally outputs zero as the key and the corresponding FileStatus as the
 * value.
 * 
 * @author dnspies
 */
public class HadoopTierMapper extends
		Mapper<Range, IntWritable, IntWritable, RangeFile> {
	private int tier;
	private TierSolver solver;
	private TierGame game;
	private String readUri;
	private final Random r = new Random();
	private Throwable failure = null;
	private HDFSSplitDatabase readDb;
	private String unzippedURI, zippedURI;

	private Configuration conf;
	private FileSystem fs;

	@Override
	public void setup(Context context) {
		try {
			org.apache.hadoop.conf.Configuration conf = context
					.getConfiguration();
			Properties props = new Properties();
			props.load(new StringReader(conf.get("gamesman.configuration")));
			DebugSetup.setup(props);
			this.conf = new Configuration(props);
			game = (TierGame) this.conf.getGame();
			fs = FileSystem.get(conf);
			tier = conf.getInt("tier", -1);
			if (tier == -1)
				throw new Error("No tier specified");
			if (tier < game.numberOfTiers() - 1) {
				readUri = conf.get("gamesman.hadoop.lastTierDb");
				HDFSInfo.initialize(conf);
				readDb = new HDFSSplitDatabase(readUri, this.conf,
						game.hashOffsetForTier(tier + 1),
						game.numHashesForTier(tier + 1), true, false);
			} else {
				readDb = null;
			}
		} catch (ClassNotFoundException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	public void map(Range key, IntWritable value, final Context context)
			throws IOException, InterruptedException {
		try {
			long firstHash = key.firstRecord;
			long numHashes = key.numRecords;
			if (numHashes == 0)
				return;
			else if (numHashes < 0)
				throw new Error("Negative number of hashes");
			String foldUri = conf.getProperty("gamesman.hadoop.dbfolder");
			final String uri = foldUri + File.separator + "s" + tier + "_"
					+ firstHash + ".db";
			final Path p = new Path(uri);
			if (fs.exists(p)) {
				writeRange(key, context, uri);
				return;
			}
			String solverName = this.conf.getProperty("gamesman.solver");
			Class<? extends TierSolver> solverc;
			try {
				solverc = Util.typedForName(
						"edu.berkeley.gamesman.solver." + solverName,
						Solver.class).asSubclass(TierSolver.class);
			} catch (ClassNotFoundException e1) {
				throw new Error(e1);
			}

			// setting write database file name, path etc and initializing it
			new File(foldUri).mkdirs();
			unzippedURI = uri + ".uz_local";
			Database writeDb = new FileDatabase(unzippedURI, conf, firstHash,
					numHashes, true, true);
			Database solverDb = new ReadWriteDatabase(readDb, writeDb, conf);
			Progressable progress = new Progressable() {
				@Override
				public void progress() {
					context.progress();
				}
			};
			try {
				solver = solverc.getConstructor(Configuration.class,
						Database.class, Integer.TYPE, Long.TYPE, Long.TYPE,
						Progressable.class).newInstance(this.conf, solverDb,
						tier, firstHash, numHashes, progress);
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
			File localFile = new File(unzippedURI);
			File localParent = localFile.getParentFile();
			localParent.setWritable(true, false);
			localParent.setExecutable(true, false);
			localParent.setReadable(true, false);
			localFile.setWritable(true, false);
			localFile.setReadable(true, false);
			solver.solve();
			Database readFrom = writeDb;
			zippedURI = uri + "_local";
			GZippedFileDatabase.zip(zippedURI, conf, readFrom, progress, true);
			writeDb.close();
			new File(unzippedURI).delete();
			unzippedURI = null;
			final Path pLocal;
			pLocal = new Path(zippedURI);
			final Path tempPath = new Path(uri + r.nextLong());
			if (!fs.exists(p)) {
				final Thread mt = Thread.currentThread();
				Thread t = new Thread() {
					@Override
					public void run() {
						try {
							fs.moveFromLocalFile(pLocal, tempPath);
							failure = null;
						} catch (Throwable e) {
							failure = e;
							mt.interrupt();
						}
					}
				};
				t.start();
				context.progress();
				while (t.isAlive()) {
					try {
						t.join(20000L);
					} catch (InterruptedException e) {
						if (failure != null)
							break;
						else
							e.printStackTrace();
					}
					context.progress();
				}
				if (failure != null) {
					if (failure instanceof Error)
						throw (Error) failure;
					else if (failure instanceof RuntimeException)
						throw (RuntimeException) failure;
					else if (failure instanceof IOException)
						throw (IOException) failure;
					else
						throw new Error(failure);
				}
				fs.rename(tempPath, p);
			}
			zippedURI = null;
			writeRange(key, context, uri);
		} catch (Error e) {
			cleanup(true);
			throw e;
		} catch (RuntimeException e) {
			cleanup(true);
			throw e;
		} catch (IOException e) {
			cleanup(true);
			throw e;
		} catch (Throwable t) {
			cleanup(true);
			throw new Error(t);
		}
	}

	private void writeRange(Range key, final Context context, String p)
			throws IOException, Error {
		try {
			context.write(new IntWritable(tier),
					new RangeFile(key, new Text(p)));
		} catch (InterruptedException e) {
			throw new Error(e);
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException,
			InterruptedException {
		cleanup(false);
	}

	private void cleanup(boolean errored) throws IOException,
			InterruptedException {
		if (readDb != null && !errored) {
			readDb.close();
			readDb = null;
		}
		if (unzippedURI != null) {
			new File(unzippedURI).delete();
			unzippedURI = null;
		}
		if (zippedURI != null) {
			new File(zippedURI).delete();
			zippedURI = null;
		}
	}
}
