package edu.berkeley.gamesman.util;

import java.io.FilterInputStream;
import java.io.InputStream;

public class UncloseableInputStream extends FilterInputStream {

	protected UncloseableInputStream(InputStream in) {
		super(in);
	}

	public void close() {
	}
}
