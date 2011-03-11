package edu.berkeley.gamesman.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class ZipChunkOutputStream extends FilterOutputStream {
	private final ChunkOutputStream cos;
	private GZIPOutputStream gzos;

	public ZipChunkOutputStream(OutputStream out) throws IOException {
		super(new ChunkOutputStream(out));
		cos = (ChunkOutputStream) this.out;
		startChunk();
	}

	public void write(int b) throws IOException {
		gzos.write(b);
	}

	public void write(byte b[]) throws IOException {
		gzos.write(b);
	}

	public void write(byte b[], int off, int len) throws IOException {
		gzos.write(b, off, len);
	}

	private int finishChunk() throws IOException {
		gzos.finish();
		return cos.nextChunk();
	}

	public int nextChunk() throws IOException {
		int result = finishChunk();
		startChunk();
		return result;
	}

	private void startChunk() throws IOException {
		gzos = new GZIPOutputStream(cos);
	}

	public void close() throws IOException {
		finishChunk();
		super.close();
	}

	public void finish() throws IOException {
		finishChunk();
		cos.finish();
	}
}
