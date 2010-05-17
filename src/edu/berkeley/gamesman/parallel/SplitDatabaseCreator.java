package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.util.ArrayList;

import edu.berkeley.gamesman.database.Database;
import edu.berkeley.gamesman.database.DatabaseHandle;
import edu.berkeley.gamesman.database.FileDatabase;
import edu.berkeley.gamesman.game.TieredGame;

/**
 * @author dnspies
 */
public class SplitDatabaseCreator extends Database {
	private File parent;
	private File tierPath;
	private ArrayList<FileDatabase> openDbs = new ArrayList<FileDatabase>();
	private ArrayList<Long> starts = new ArrayList<Long>();

	@Override
	public void close() {
		for (FileDatabase fd : openDbs) {
			fd.close();
		}
	}

	@Override
	public void getBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		((SplitHandle) dh).openDb.getBytes(((SplitHandle) dh).innerHandle, loc,
				arr, off, len);
	}

	@Override
	public void initialize(String uri, boolean solve) {
		parent = new File(uri);
		if (!parent.exists())
			parent.mkdir();
	}

	@Override
	public void putBytes(DatabaseHandle dh, long loc, byte[] arr, int off,
			int len) {
		((SplitHandle) dh).openDb.putBytes(((SplitHandle) dh).innerHandle, loc,
				arr, off, len);
	}

	private class SplitHandle extends DatabaseHandle {
		FileDatabase openDb;
		DatabaseHandle innerHandle;

		public SplitHandle(int tier, long recordStart, long numRecords) {
			openDb = new FileDatabase(false);
			long byteStart = recordStart / conf.recordsPerGroup
					* conf.recordGroupByteLength;
			long numBytes = (recordStart + numRecords + conf.recordsPerGroup - 1)
					/ conf.recordsPerGroup
					* conf.recordGroupByteLength
					- byteStart;
			openDb.setRange(byteStart, numBytes);
			String fileName = "s" + Long.toString(byteStart) + ".db";
			starts.add(byteStart);
			openDb.initialize(tierPath.getPath() + File.separator + fileName,
					conf, true);
			innerHandle = openDb.getHandle();
			openDbs.add(openDb);
		}
	}

	@Override
	public DatabaseHandle getHandle(long recordStart, long numRecords) {
		return new SplitHandle(((TieredGame) conf.getGame())
				.hashToTier(recordStart), recordStart, numRecords);
	}

	@Override
	public void closeHandle(DatabaseHandle dh) {
		SplitHandle handle = (SplitHandle) dh;
		openDbs.remove(handle.openDb);
		handle.openDb.close();
	}

	/**
	 * @return A list of the starting hashes of every file contained (separated
	 *         by spaces)
	 */
	public String getStartList() {
		StringBuilder sb = new StringBuilder();
		sb.append(starts.get(0));
		for (int i = 1; i < starts.size(); i++) {
			sb.append(" ");
			sb.append(starts.get(i));
		}
		return sb.toString();
	}

	/**
	 * @param tier
	 *            The tier for solving in
	 */
	public void setTier(int tier) {
		tierPath = new File(parent, "t" + tier);
		if (!tierPath.exists())
			tierPath.mkdir();
	}
}
