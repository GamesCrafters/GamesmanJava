package edu.berkeley.gamesman.database;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.database.filer.DirectoryFilerClient;
import edu.berkeley.gamesman.util.Util;

public class RemoteDatabase extends Database {

	static ThreadLocal<DirectoryFilerClient> dfc;
	static ThreadLocal<Database> real;

	@Override
	public void close() {
		real.get().close();
		dfc.get().close();
	}

	@Override
	public void flush() {
		real.get().flush();
	}

	@Override
	public Record getValue(BigInteger loc) {
		return real.get().getValue(loc);
	}

	@Override
	public void initialize(final String uri) {
		dfc = new ThreadLocal<DirectoryFilerClient>() {
			@Override
			protected DirectoryFilerClient initialValue() {
				try {
					return new DirectoryFilerClient(new URI(uri));
				} catch (URISyntaxException e1) {
					Util.fatalError("Bad URI \"" + uri + "\": " + e1);
				}
				return null;
			}
		};
		real = new ThreadLocal<Database>() {
			protected Database initialValue() {
				try {
					return dfc.get().openDatabase(new URI(uri).getPath(), conf);
				} catch (URISyntaxException e) {
					Util.fatalError("Bad URI \"" + uri + "\": " + e);
				}
				return null;
			}
		};
	}

	@Override
	public void setValue(BigInteger loc, Record value) {
		real.get().setValue(loc, value);
	}

}
