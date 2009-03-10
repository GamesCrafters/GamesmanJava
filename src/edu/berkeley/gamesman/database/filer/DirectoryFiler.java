package edu.berkeley.gamesman.database.filer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Filer;
import edu.berkeley.gamesman.database.BlockDatabase;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Pair;
import edu.berkeley.gamesman.util.Util;

/**
 * A DirectoryFiler is a collection of Databases.
 * The Filer keeps track of all databases stored and can
 * open them on request by name.
 * 
 * You must be sure to close the DirectoryFiler before exiting for it to save changes
 * @author Steven Schlansker
 */
public class DirectoryFiler extends Filer<Database>{
	File rootdir;
	HashMap<String,Pair<Configuration,File>> dbs = new HashMap<String,Pair<Configuration,File>>();

	/**
	 * Open a preexisting DirectoryFiler datastore
	 * @param rootdir The directory of the datastore
	 */
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
				dbs.put(bits[0],new Pair<Configuration, File>(Configuration.load(Util.decodeBase64(bits[2])),Util.getChild(rootdir, bits[1])));
			}
		} catch (IOException e) {
			Util.fatalError("IO error while reading from manifest: "+e);
		}
		
	}
	
	public String[] ls(){
		return dbs.keySet().toArray(new String[dbs.size()]);
	}

	@Override
	public Database openDatabase(String name, Configuration conf) {
		Database db = new BlockDatabase();
		File f = Util.getChild(rootdir, name);
		if(dbs.containsKey(name))
			db.initialize(f.toURI().toString(), dbs.get(name).car);
		else{
			db.initialize(f.toURI().toString(), conf);
			dbs.put(name, new Pair<Configuration,File>(conf,f));
		}
		return db;
	}

	@Override
	public Database openDatabase(Configuration conf) {
		Util.fatalError("Not implemented");
		return null;
	}

	@Override
	public void close() {
		File f = Util.getChild(rootdir, "MANIFEST");
		PrintWriter fw = null;
		assert Util.debug(DebugFacility.FILER, "Cleanly closing directory filer " + this);
		try {
			fw = new PrintWriter(new FileWriter(f,false));
		} catch (IOException e1) {
			Util.fatalError("Could not write manifest",e1);
		}
		for(Entry<String, Pair<Configuration, File>> e : dbs.entrySet()){
			fw.println(e.getKey() + "\t"+e.getValue().cdr.getName()+"\t"+Util.encodeBase64(e.getValue().car.store()));
		}
		
		fw.flush();
		fw.close();
	}
	
}
