package edu.berkeley.gamesman.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.util.Util;

public final class FileDatabase extends Database {

	protected File myFile;

	protected RandomAccessFile fd;

	DBRecord generator;

	long offset;

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
	public synchronized DBRecord getValue(BigInteger loc) {
		try {
			fd.seek(loc.longValue()+offset);
			byte b = fd.readByte();
			DBRecord v = generator.wrapValue(b);
			// Util.debug("Location "+loc+" = "+v+" ("+b+")");
			return v;
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
		Util.fatalError("WTF");
		return null; // Not reached
	}

	@Override
	public synchronized void initialize(String loc, Configuration config,
			DBRecord example) {

		boolean previouslyExisted;

		try {
			myFile = new File(new URI(loc));
		} catch (URISyntaxException e1) {
			Util.fatalError("Could not open URI " + loc + ": " + e1);
		}

		previouslyExisted = myFile.exists();

		try {
			fd = new RandomAccessFile(myFile, "rw");
		} catch (FileNotFoundException e) {
			Util.fatalError("Could not create/open database: " + e);
		}

		generator = example;

		try {
			fd.seek(0);
			if (previouslyExisted) {
				int headerLen = fd.readInt();
				byte[] header = new byte[headerLen];
				fd.readFully(header);
				Util.assertTrue(new String(header).equals(config.getConfigString()), "File database has wrong header; expecting \""+config.getConfigString()+"\" got \""+new String(header)+"\"");
			}else{
				fd.writeInt(config.getConfigString().length());
				fd.write(config.getConfigString().getBytes());
			}
			offset = fd.getFilePointer();
		} catch (IOException e) {
			Util.fatalError("IO error while checking header: " + e);
		}
	}

	@Override
	public synchronized void setValue(BigInteger loc, DBRecord value) {
		try {
			fd.seek(loc.longValue()+offset);
			fd.writeByte(value.byteValue());
			// Util.debug("Wrote "+value.byteValue()+" to "+loc);
		} catch (IOException e) {
			Util.fatalError("IO Error: " + e);
		}
	}

}
