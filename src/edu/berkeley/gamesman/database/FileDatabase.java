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

public final class FileDatabase<Value> extends Database<Value> {

	protected File myFile;
	
	protected RandomAccessFile fd;
	
	public void close() {
		try{
			fd.close();
		}catch (IOException e) {
			Util.warn("Error while closing input stream for database: "+e);
		}
	}

	public void flush() {
		try{
			fd.getFD().sync();
		}catch(IOException e){
			Util.fatalError("Error while writing to database: "+e);
		}
	}

	public Value getValue(Number loc) {
		return null;
	}

	public void initialize(String url) {
		
		if(url == null) return;  //TODO: get rid of this case, it's just used to ignore null dbs for now
		
		try{
			myFile = new File(new URI(url));
		}catch (URISyntaxException e) {
			Util.fatalError("Could not open database; URI is malformed: "+e);
		}
		
		try {
			fd = new RandomAccessFile(myFile,"rw");
		}catch (FileNotFoundException e) {
			Util.fatalError("Could not create/open database: "+e);
		}
	}

	public void setValue(Number loc, Value value) {
		//Util.fatalError("BARF! " + value);
	}

}
