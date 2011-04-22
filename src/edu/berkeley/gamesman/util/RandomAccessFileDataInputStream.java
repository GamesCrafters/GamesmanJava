package edu.berkeley.gamesman.util;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import edu.berkeley.gamesman.database.util.SeekableInputStream;

public class RandomAccessFileDataInputStream extends SeekableInputStream {
	private final RandomAccessFile raf;
	private long marked = 0L;

	public RandomAccessFileDataInputStream(String uri)
			throws FileNotFoundException {
		this(new RandomAccessFile(uri, "r"));
	}

	public RandomAccessFileDataInputStream(RandomAccessFile raf) {
		this.raf = raf;
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		raf.readFully(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		raf.readFully(b, off, len);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		return raf.skipBytes(n);
	}

	@Override
	public boolean readBoolean() throws IOException {
		return raf.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return raf.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return raf.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException {
		return raf.readShort();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return raf.readUnsignedShort();
	}

	@Override
	public char readChar() throws IOException {
		return raf.readChar();
	}

	@Override
	public int readInt() throws IOException {
		return raf.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return raf.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return raf.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return raf.readDouble();
	}

	@Override
	public String readLine() throws IOException {
		return raf.readLine();
	}

	@Override
	public String readUTF() throws IOException {
		return raf.readUTF();
	}

	@Override
	public void seek(long pos) throws IOException {
		raf.seek(pos);
	}

	@Override
	public int read() throws IOException {
		return raf.read();
	}

	@Override
	public int read(byte b[]) throws IOException {
		return raf.read(b);
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		return raf.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return raf.skipBytes((int) Math.min(n, Integer.MAX_VALUE));
	}

	@Override
	public int available() throws IOException {
		return (int) Math.min(raf.length() - raf.getFilePointer(),
				Integer.MAX_VALUE);
	}

	@Override
	public void close() throws IOException {
		raf.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		try {
			marked = raf.getFilePointer();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		raf.seek(marked);
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	public final FileDescriptor getFD() throws IOException {
		return raf.getFD();
	}

	public final FileChannel getChannel() {
		return raf.getChannel();
	}

	public long length() throws IOException {
		return raf.length();
	}
}
