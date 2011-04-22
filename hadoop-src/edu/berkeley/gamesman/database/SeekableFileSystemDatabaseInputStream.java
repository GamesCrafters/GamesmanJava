package edu.berkeley.gamesman.database;

import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import edu.berkeley.gamesman.database.util.SeekableInputStream;

public class SeekableFileSystemDatabaseInputStream extends
		SeekableInputStream {
	private final FSDataInputStream in;

	public SeekableFileSystemDatabaseInputStream(String uri) throws IOException {
		in = HDFSInfo.getHDFS().open(new Path(uri));
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		in.readFully(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		in.readFully(b, off, len);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		return in.skipBytes(n);
	}

	@Override
	public boolean readBoolean() throws IOException {
		return in.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return in.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return in.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException {
		return in.readShort();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return in.readUnsignedShort();
	}

	@Override
	public char readChar() throws IOException {
		return in.readChar();
	}

	@Override
	public int readInt() throws IOException {
		return in.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return in.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return in.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return in.readDouble();
	}

	@Deprecated
	@Override
	public String readLine() throws IOException {
		return in.readLine();
	}

	@Override
	public String readUTF() throws IOException {
		return in.readUTF();
	}

	@Override
	public void seek(long pos) throws IOException {
		in.seek(pos);
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public int read(byte b[]) throws IOException {
		return in.read(b);
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
		return in.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		in.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		in.reset();
	}

	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	public long getPos() throws IOException {
		return in.getPos();
	}

	public boolean seekToNewSource(long targetPos) throws IOException {
		return in.seekToNewSource(targetPos);
	}

	public void readFully(long position, byte[] buffer) throws IOException {
		in.readFully(position, buffer);
	}

	public void readFully(long position, byte[] buffer, int offset, int length)
			throws IOException {
		in.readFully(position, buffer, offset, length);
	}
}
