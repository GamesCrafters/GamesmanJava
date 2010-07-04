package edu.berkeley.gamesman.util;

import java.io.InputStream;
import java.util.Scanner;

/**
 * @author dnspies
 * 
 */
public class ErrorThread extends Thread {
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
		while (myErrors.hasNext())
			error(myErrors.nextLine());
		myErrors.close();
	}

	public void error(String error) {
		System.err.println((extra == null ? "" : (extra + ": ")) + error);
		hadErrors = true;
	}
}
