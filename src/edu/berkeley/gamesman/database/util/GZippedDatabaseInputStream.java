package edu.berkeley.gamesman.database.util;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;

public abstract class GZippedDatabaseInputStream extends InputStream implements
		DataInput {

	public abstract void seek(long pos) throws IOException;
}
