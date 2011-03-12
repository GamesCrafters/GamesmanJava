package edu.berkeley.gamesman.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

public class ZipChunkOutputStream extends FilterOutputStream {
	private final ChunkOutputStream cos;
	private GZIPOutputStream gzos;
	private boolean bytesWritten;

	public ZipChunkOutputStream(OutputStream out) throws IOException {
		super(new ChunkOutputStream(out));
		cos = (ChunkOutputStream) this.out;
		startChunk();
	}

	public void write(int b) throws IOException {
		gzos.write(b);
		bytesWritten = true;
	}

	public void write(byte b[]) throws IOException {
		gzos.write(b);
		bytesWritten = true;
	}

	public void write(byte b[], int off, int len) throws IOException {
		gzos.write(b, off, len);
		bytesWritten = true;
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
		bytesWritten = false;
	}

	public void close() throws IOException {
		if (bytesWritten) {
			finishChunk();
			super.close();
		} else {
			clearChunkAndClose();
		}
	}

	public void finish() throws IOException {
		if (bytesWritten) {
			finishChunk();
			cos.finish();
		} else {
			clearChunkAndFinish();
		}
	}

	public void clearChunkAndFinish() {
		cos.clearChunk();
	}

	public void clearChunkAndClose() throws IOException {
		cos.clearChunkAndClose();
	}

	public void clearChunk() throws IOException {
		if (bytesWritten) {
			cos.clearChunk();
			startChunk();
		}
	}
}
