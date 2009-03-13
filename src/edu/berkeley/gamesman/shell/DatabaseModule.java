package edu.berkeley.gamesman.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Properties;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.util.Util;

public class DatabaseModule extends UIModule {
	private static RandomAccessFile dbFile;
	private static Record theRecord, curRecord;	
	
	public DatabaseModule(Configuration c) {
		super(c, "data");
		
		//theRecord = new Record(conf);
		requiredPropKeys = new ArrayList<String>();
		requiredPropKeys.add("gamesman.db.uri");
		//what property would it want? 
		//maybe that if conf has a database path?
		
		helpLines = new Properties();
		
		helpLines.setProperty("openDatabase", 
				"open a database file from the current configuration.");
		helpLines.setProperty("closeDatabase",
				"close a database file that this module was working on.");
		helpLines.setProperty("readRecord",
				"read a record from the database file.");
		helpLines.setProperty("viewRecord",
				"view the record that has been read into the system.");
		//helpLines.setProperty("editRecord", "");		
	}
	
	/**
	 * u_openDatabase calls openDatabase with curConf.
	 */
	protected void u_openDatabase(ArrayList<String> args) {
		openDatabase();
	}
	
	/**
	 * openDatabase takes a configuration and instantiates dbFile as a RandomAccessFile for the user to read.
	 * it's in RandomAccessFile because it uses Record class later on.
	 * @param conf configuration it's trying to read a database file from.
	 */
	private void openDatabase() {
		String filePath = conf.getProperty("gamesman.db.uri");
		try {
			dbFile = new RandomAccessFile(filePath, "rw");
		}		
		catch(FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * u_closeDatabase closes the dbFile that's opened.
	 */
	protected void u_closeDatabase(ArrayList<String> args) {
		if(dbFile != null) {
			try {
				dbFile.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * u_readRecord reads a record from theRecord into curRecord.
	 */	
	protected void u_readRecord(ArrayList<String> args) {
		try {
			curRecord = theRecord.readStream(conf, dbFile);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * u_viewRecord prints out curRecord
	 */
	protected void u_viewRecord(ArrayList<String> args) {
		System.out.println(curRecord);
	}
	
	/**
	 * u_editRecord edits the curRecord
	 */
	//I don't know what you would put in to change the record.	
	protected void u_editRecord(ArrayList<String> args) {
		if(curRecord != null) {
			
		}
	}		
}

/*
 * ability to open database, close, 
 * read records, edit records, view records
 */