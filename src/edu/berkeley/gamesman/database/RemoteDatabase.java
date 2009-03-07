package edu.berkeley.gamesman.database;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.database.filer.DirectoryFilerClient;
import edu.berkeley.gamesman.util.Util;

/**
 * A RemoteDatabase is a 'stub' database that uses a DirectoryFilerClient to connect
 * to a server and open remote databases.  It then passes on all requests to that
 * remote database and returns the result locally.  (all synchronously)
 * @author Steven Schlansker
 */
public class RemoteDatabase extends Database {

	private static ThreadLocal<DirectoryFilerClient> dfc;
	private static ThreadLocal<Database> real;
	
	private static final List<DirectoryFilerClient> alldfc = Collections.synchronizedList(new ArrayList<DirectoryFilerClient>());
	private static final List<Database> alldbs = Collections.synchronizedList(new ArrayList<Database>());
	

	@Override
	public void close() {
		for(Database db : alldbs){
			db.close();
		}
		for(DirectoryFilerClient d : alldfc){
			d.close();
		}
		alldfc.clear();
		alldbs.clear();
	}
	
	public void finalize(){
		close();
	}

	@Override
	public void flush() {
		real.get().flush();
	}

	@Override
	public Record getRecord(BigInteger loc) {
		return real.get().getRecord(loc);
	}

	@Override
	@SuppressWarnings("synthetic-access")
	public void initialize(final String uri) {
		dfc = new ThreadLocal<DirectoryFilerClient>() {
			@Override
			protected DirectoryFilerClient initialValue() {
				try {
					DirectoryFilerClient d = new DirectoryFilerClient(new URI(uri));
					alldfc.add(d);
					return d;
				} catch (URISyntaxException e1) {
					Util.fatalError("Bad URI \"" + uri + "\": " + e1);
				}
				return null;
			}
		};
		real = new ThreadLocal<Database>() {
			protected Database initialValue() {
				try {
					Database db = dfc.get().openDatabase(new URI(uri).getPath(), conf);
					alldbs.add(db);
					return db;
				} catch (URISyntaxException e) {
					Util.fatalError("Bad URI \"" + uri + "\": " + e);
				}
				return null;
			}
		};
	}

	@Override
	public void putRecord(BigInteger loc, Record value) {
		real.get().putRecord(loc, value);
	}

}
