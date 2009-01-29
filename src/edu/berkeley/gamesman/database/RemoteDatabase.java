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

	DirectoryFilerClient dfc;
	Database real;
	
	@Override
	public void close() {
		real.close();
		dfc.close();
	}

	@Override
	public void flush() {
		real.flush();
	}

	@Override
	public Record getValue(BigInteger loc) {
		return real.getValue(loc);
	}

	@Override
	public void initialize(String uri, Configuration config) {
		try {
			dfc = new DirectoryFilerClient(new URI(uri));
		} catch (URISyntaxException e1) {
			Util.fatalError("Bad URI \""+uri+"\": "+e1);
		}
		try {
			real = dfc.openDatabase(new URI(uri).getPath(), config);
		} catch (URISyntaxException e) {
			Util.fatalError("Bad URI \""+uri+"\": "+e);
		}
	}

	@Override
	public void setValue(BigInteger loc, Record value) {
		real.setValue(loc, value);
	}

}
