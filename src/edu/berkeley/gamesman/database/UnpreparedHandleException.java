package edu.berkeley.gamesman.database;

import java.io.IOException;

public class UnpreparedHandleException extends IOException {
	private static final long serialVersionUID = -3966528112539795929L;

	private final DatabaseHandle myHandle;

	public UnpreparedHandleException(DatabaseHandle handle, Throwable cause) {
		super(cause);
		myHandle = handle;
	}

	public UnpreparedHandleException(DatabaseHandle handle, String message,
			Throwable cause) {
		super(message, cause);
		myHandle = handle;
	}

	public UnpreparedHandleException(DatabaseHandle handle, String message) {
		super(message);
		myHandle = handle;
	}

	public UnpreparedHandleException(DatabaseHandle handle) {
		myHandle = handle;
	}

	public DatabaseHandle getHandle() {
		return myHandle;
	}
}
