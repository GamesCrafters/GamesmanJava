package edu.berkeley.gamesman.database;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.core.TieredGame;

public class TierSplitDatabase extends Database {
	File dbDirectory;

	Constructor<? extends Database> underBaseConstructor;

	ArrayList<ArrayList<Database>> myBases = new ArrayList<ArrayList<Database>>();
	
	int split;

	public TierSplitDatabase() {
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	@Override
	public RecordGroup getRecordGroup(long loc) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(String uri) {
		dbDirectory = new File(uri);
		if (!dbDirectory.exists())
			dbDirectory.mkdir();
		int tiers = ((TieredGame<?>) conf.getGame()).numberOfTiers();
		for (int i = 0; i < tiers; i++) {
			File f = new File("tier" + i);
			f.mkdir();
		}
	}

	@Override
	public void putRecordGroup(long loc, RecordGroup rg) {
		// TODO Auto-generated method stub

	}
}
