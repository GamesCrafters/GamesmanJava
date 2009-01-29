package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;

import edu.berkeley.gamesman.util.Util;

public final class FileDatabase extends Database {

	protected File myFile;

	protected RandomAccessFile fd;

	DBValue generator;

	public void close() {
		try {
			fd.close();
		} catch (IOException e) {
			Util.warn("Error while closing input stream for database: " + e);
		}
	}

	public void flush() {
		try {
			fd.getFD().sync();
		} catch (IOException e) {
			Util.fatalError("Error while writing to database: " + e);
		}
	}

	public DBValue getValue(Number loc) {
		try {
			fd.seek(loc.longValue());
			return generator.wrapValue(fd.readByte());
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
		return null; // Not reached
	}

	public void initialize(String loc, DBValue example) {

		myFile = new File(loc);

		try {
			fd = new RandomAccessFile(myFile, "rw");
		} catch (FileNotFoundException e) {
			Util.fatalError("Could not create/open database: " + e);
		}
		
		generator = example;
	}

	public void setValue(Number loc, DBValue value) {
		try {
			fd.seek(loc.longValue());
			fd.writeByte(value.byteValue());
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
	}

}
