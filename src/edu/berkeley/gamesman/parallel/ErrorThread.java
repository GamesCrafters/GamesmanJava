package edu.berkeley.gamesman.parallel;

import java.io.InputStream;
import java.util.Scanner;

/**
 * @author dnspies
 * 
 */
public final class ErrorThread extends Thread {
	private final Scanner myErrors;
	public boolean hadErrors = false;
	private final String extra;

	public ErrorThread(InputStream errorStream) {
		this(errorStream, null);
	}

	public ErrorThread(InputStream errorStream, String extra) {
		myErrors = new Scanner(errorStream);
		this.extra = extra;
		setDaemon(true);
	}

	public void run() {
		while (myErrors.hasNext()) {
			System.err.println((extra == null ? "" : (extra + ": "))
					+ myErrors.nextLine());
			hadErrors = true;
		}
		myErrors.close();
	}
}
