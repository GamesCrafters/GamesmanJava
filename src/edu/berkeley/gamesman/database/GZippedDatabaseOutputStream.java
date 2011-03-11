package edu.berkeley.gamesman.database;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

public abstract class GZippedDatabaseOutputStream extends OutputStream
		implements DataOutput {

	public abstract long getFilePointer() throws IOException;

	public abstract void seek(long pos) throws IOException;
}
