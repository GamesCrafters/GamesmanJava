package edu.berkeley.gamesman.database;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.database.SplitDatabase.DatabaseDescriptor;

public class SplitDBMaker {
	private final DataOutputStream out;

	public SplitDBMaker(String uri, Configuration conf, long firstRecordIndex,
			long numRecords) throws IOException {
		File f = new File(uri);
		f.getParentFile().mkdirs();
		out = new DataOutputStream(new FileOutputStream(f));
		out.writeLong(firstRecordIndex);
		out.writeLong(numRecords);
		conf.store(out);
	}

	public SplitDBMaker(String uri, Configuration conf) throws IOException {
		this(uri, conf, 0L, conf.getGame().numHashes());
	}

	public void addDb(String dbClass, String uri, long firstRecordIndex,
			long numRecords) throws IOException {
		out.writeUTF(dbClass);
		out.writeUTF(uri);
		out.writeLong(firstRecordIndex);
		out.writeLong(numRecords);
	}

	public void close() throws IOException {
		out.close();
	}

	public void addDb(DatabaseDescriptor dd) throws IOException {
		addDb(dd.dbClass, dd.uri, dd.firstRecordIndex, dd.numRecords);
	}
}
