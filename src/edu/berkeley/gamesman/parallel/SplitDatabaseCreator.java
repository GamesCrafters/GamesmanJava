package edu.berkeley.gamesman.parallel;

import java.io.File;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.database.FileDatabase;

/**
 * @author dnspies
 */
public class SplitDatabaseCreator extends Database {
	private File parent;
	private FileDatabase openDb;
	private ArrayList<FileDatabase> openDbs = new ArrayList<FileDatabase>();
	private ArrayList<Long> starts = new ArrayList<Long>();

	@Override
	public void close() {
		for (FileDatabase fd : openDbs) {
			fd.close();
		}
	}

	@Override
	public void flush() {
		openDb.flush();
	}

	@Override
	public void getBytes(byte[] arr, int off, int len) {
		openDb.getBytes(arr, off, len);
	}

	@Override
	public void getBytes(long loc, byte[] arr, int off, int len) {
		openDb.getBytes(loc, arr, off, len);
	}

	@Override
	public void initialize(String uri, boolean solve) {
		parent = new File(uri);
		if (!parent.exists())
			parent.mkdir();
	}

	@Override
	public void putBytes(byte[] arr, int off, int len) {
		openDb.putBytes(arr, off, len);
	}

	@Override
	public void putBytes(long loc, byte[] arr, int off, int len) {
		openDb.putBytes(loc, arr, off, len);
	}

	@Override
	public void seek(long loc) {
		openDb.seek(loc);
	}

	@Override
	public synchronized Database beginWrite(int tier, long recordStart,
			long recordEnd) {
		openDb = new FileDatabase();
		openDb.setRange(conf, recordStart, recordEnd - recordStart);
		String fileName = "s" + Long.toString(recordStart) + ".db";
		starts.add(recordStart);
		openDb.initialize(parent.getPath() + File.separator + fileName, conf,
				true);
		openDbs.add(openDb);
		return openDb;
	}

	@Override
	public synchronized void endWrite(int tier, Database db, long recordStart,
			long recordEnd) {
		openDbs.remove(db);
		db.close();
		if (openDb == db)
			if (openDbs.size() > 0)
				openDb = openDbs.get(openDbs.size() - 1);
			else
				openDb = null;
	}

	public String getStartList() {
		StringBuilder sb = new StringBuilder();
		sb.append(starts.get(0));
		for (int i = 1; i < starts.size(); i++) {
			sb.append(" ");
			sb.append(starts.get(i));
		}
		return sb.toString();
	}
}
