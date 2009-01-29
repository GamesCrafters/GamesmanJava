package edu.berkeley.gamesman.database;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.database.filer.DirectoryFilerClient;
import edu.berkeley.gamesman.util.Util;

public class RemoteDatabase extends Database {

	DirectoryFilerClient dfc;
	DBValue ex;
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
	public DBValue getValue(Number loc) {
		return real.getValue(loc);
	}

	@Override
	public void initialize(String uri, DBValue exampleValue) {
		try {
			dfc = new DirectoryFilerClient(new URI(uri));
		} catch (URISyntaxException e1) {
			Util.fatalError("Bad URI \""+uri+"\": "+e1);
		}
		ex = exampleValue;
		try {
			real = dfc.openDatabase(new URI(uri).getPath());
		} catch (URISyntaxException e) {
			Util.fatalError("Bad URI \""+uri+"\": "+e);
		}
	}

	@Override
	public void setValue(Number loc, DBValue value) {
		real.setValue(loc, value);
	}

}
