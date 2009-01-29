package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.Util;

public final class FileDatabase extends Database {

	protected File myFile;

	protected RandomAccessFile fd;

	DBValue generator;

	@Override
	public synchronized void close() {
		try {
			fd.close();
		} catch (IOException e) {
			Util.warn("Error while closing input stream for database: " + e);
		}
	}

	@Override
	public synchronized void flush() {
		try {
			fd.getFD().sync();
			fd.getChannel().force(true);
		} catch (IOException e) {
			Util.fatalError("Error while writing to database: " + e);
		}
	}

	@Override
	public synchronized DBValue getValue(Number loc) {
		try {
			fd.seek(loc.longValue());
			byte b = fd.readByte();
			DBValue v = generator.wrapValue(b);
			Util.debug("Location "+loc+" = "+v+" ("+b+")");
			return v;
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
		Util.fatalError("WTF");
		return null; // Not reached
	}

	@Override
	public synchronized void initialize(String loc, Configuration config, DBValue example) {

		try {
			myFile = new File(new URI(loc));
		} catch (URISyntaxException e1) {
			Util.fatalError("Could not open URI "+loc+": "+e1);
		}

		try {
			fd = new RandomAccessFile(myFile, "rw");
		} catch (FileNotFoundException e) {
			Util.fatalError("Could not create/open database: " + e);
		}
		
		generator = example;
	}

	@Override
	public synchronized void setValue(Number loc, DBValue value) {
		try {
			fd.seek(loc.longValue());
			fd.writeByte(value.byteValue());
			Util.debug("Wrote "+value.byteValue()+" to "+loc);
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
	}

}
