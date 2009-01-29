package edu.berkeley.gamesman.database;

import java.net.MalformedURLException;
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
	public void initialize(String url, DBValue exampleValue) {
		dfc = new DirectoryFilerClient(url);
		ex = exampleValue;
		try {
			real = dfc.openDatabase(new URL(url).getFile());
		} catch (MalformedURLException e) {
			Util.fatalError("Bad URL \""+url+"\": "+e);
		}
	}

	@Override
	public void setValue(Number loc, DBValue value) {
		real.setValue(loc, value);
	}

}
