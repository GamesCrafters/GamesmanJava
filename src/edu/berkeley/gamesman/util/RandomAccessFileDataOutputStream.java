package edu.berkeley.gamesman.util;

import java.io.DataOutput;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import edu.berkeley.gamesman.database.GZippedDatabaseOutputStream;

public class RandomAccessFileDataOutputStream extends
		GZippedDatabaseOutputStream implements DataOutput {
	RandomAccessFile myRaf;

	public RandomAccessFileDataOutputStream(String uri, String permissions)
			throws FileNotFoundException {
		myRaf = new RandomAccessFile(uri, permissions);
	}

	@Override
	public void write(int b) throws IOException {
		myRaf.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		myRaf.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		myRaf.write(b, off, len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		myRaf.writeBoolean(v);
	}

	@Override
	public void writeByte(int v) throws IOException {
		myRaf.writeByte(v);
	}

	@Override
	public void writeShort(int v) throws IOException {
		myRaf.writeShort(v);
	}

	@Override
	public void writeChar(int v) throws IOException {
		myRaf.writeChar(v);
	}

	@Override
	public void writeInt(int v) throws IOException {
		myRaf.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException {
		myRaf.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException {
		myRaf.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException {
		myRaf.writeDouble(v);
	}

	@Override
	public void writeBytes(String s) throws IOException {
		myRaf.writeBytes(s);
	}

	@Override
	public void writeChars(String s) throws IOException {
		myRaf.writeChars(s);
	}

	@Override
	public void writeUTF(String s) throws IOException {
		myRaf.writeUTF(s);
	}

	@Override
	public void close() throws IOException {
		myRaf.close();
	}

	public FileChannel getChannel() {
		return myRaf.getChannel();
	}

	public FileDescriptor getFD() throws IOException {
		return myRaf.getFD();
	}

	@Override
	public long getFilePointer() throws IOException {
		return myRaf.getFilePointer();
	}

	public long length() throws IOException {
		return myRaf.length();
	}

	@Override
	public void seek(long pos) throws IOException {
		myRaf.seek(pos);
	}

	public void setLength(long newLength) throws IOException {
		myRaf.setLength(newLength);
	}
}
