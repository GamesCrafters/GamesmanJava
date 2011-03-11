package edu.berkeley.gamesman.util;

import java.io.InputStream;
import java.util.Scanner;

/**
 * @author dnspies
 * 
 */
public class ErrorThread extends Thread {
	private Scanner myErrors;
	public boolean hadErrors = false;
	private final String extra;

	public ErrorThread(InputStream errorStream) {
		this(errorStream, null);
	}

	public ErrorThread(InputStream errorStream, String extra) {
		if (errorStream != null)
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

	public void setStream(InputStream es) {
		myErrors = new Scanner(es);
	}

	public void error(String thisExtra, Throwable t) {
		System.err.print((extra == null ? "" : (extra + ": "))
				+ (thisExtra == null ? "" : (thisExtra + ": ")));
		t.printStackTrace();
		hadErrors = true;
	}
}
