package edu.berkeley.gamesman.database;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Simple solid file database that supports compression.
 * @author Patrick Reiter Horn
 */
public class SolidFileDatabase extends SolidDatabase {

	@Override
	protected InputStream openInputStream(String uri) throws IOException {
		return new FileInputStream(uri);
	}

	@Override
	protected OutputStream openOutputStream(String uri) throws IOException {
		return new FileOutputStream(uri);
	}

}
