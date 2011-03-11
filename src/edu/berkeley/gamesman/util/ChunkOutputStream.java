package edu.berkeley.gamesman.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ChunkOutputStream extends FilterOutputStream {
	private final DataOutput dos;
	private final ByteArrayOutputStream baos;

	public ChunkOutputStream(OutputStream out) {
		this(out, -1);
	}

	public ChunkOutputStream(OutputStream out, int chunkBufferByteSize) {
		super(out instanceof DataOutput ? out : new DataOutputStream(out));
		dos = (DataOutput) this.out;
		if (chunkBufferByteSize == -1)
			baos = new ByteArrayOutputStream();
		else
			baos = new ByteArrayOutputStream(chunkBufferByteSize);
	}

	@Override
	public void write(int b) {
		baos.write(b);
	}

	@Override
	public void write(byte b[]) throws IOException {
		baos.write(b);
	}

	@Override
	public void write(byte b[], int off, int len) {
		baos.write(b, off, len);
	}

	@Override
	public void close() throws IOException {
		nextChunk();
		super.close();
	}

	public int nextChunk() throws IOException {
		byte[] bytes = baos.toByteArray();
		dos.writeInt(bytes.length);
		dos.write(bytes);
		baos.reset();
		return 4 + bytes.length;
	}

	public void finish() throws IOException {
		nextChunk();
	}
}
