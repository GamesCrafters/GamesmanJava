package edu.berkeley.gamesman.database.filer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.HashMap;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Filer;
import edu.berkeley.gamesman.database.BlockDatabase;
import edu.berkeley.gamesman.util.Util;

public class DirectoryFiler extends Filer<BlockDatabase>{
	File rootdir;
	HashMap<String,File> dbs = new HashMap<String,File>();

	public DirectoryFiler(File rootdir){
		this.rootdir = rootdir;
		LineNumberReader mf = null;
		try {
			mf = new LineNumberReader(new FileReader(Util.getChild(rootdir,"MANIFEST")));
		} catch (Exception e) {
			Util.fatalError("Could not open MANIFEST file: "+e);
		}
		
		String line;
		try {
			while((line = mf.readLine()) != null){
				String[] bits = line.split("[ \t]+");
				dbs.put(bits[0],Util.getChild(rootdir, bits[1]));
			}
		} catch (IOException e) {
			Util.fatalError("IO error while reading from manifest: "+e);
		}
		
	}
	
	public String[] ls(){
		return dbs.keySet().toArray(new String[dbs.size()]);
	}

	@Override
	public BlockDatabase openDatabase(String name) {
	}

	@Override
	public BlockDatabase openDatabase(Configuration conf) {
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	
}
