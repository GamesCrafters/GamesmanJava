package edu.berkeley.gamesman.database.filer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.RecordGroup;
import edu.berkeley.gamesman.util.DebugFacility;
import edu.berkeley.gamesman.util.Util;
import edu.berkeley.gamesman.util.biginteger.BigInteger;

/**
 * A DirectoryFilerClient connects to a DirectoryFilerServer and allows
 * the use of the remote DirectoryFiler resources as if they were local.
 * @see DirectoryFilerServer
 * @see DirectoryFiler
 * @author Steven Schlansker
 */
public final class DirectoryFilerClient {

	protected Socket sock;
	protected DataOutputStream dout;
	protected DataInputStream din;
	protected Random r = new Random();

	/**
	 * Connect to a remote DirectoryFilerServer with its
	 * gdfp:// URI
	 * @param u the URI to connect to
	 */
	public DirectoryFilerClient(URI u) {

		if (!u.getScheme().equals("gdfp"))
			Util.fatalError("URI has wrong scheme: \"" + u + "\"");

		try {
			sock = new Socket(u.getHost(), u.getPort());
		} catch (UnknownHostException e) {
			Util.fatalError("Could not find host: " + e);
		} catch (IOException e) {
			Util.fatalError("Could not connect to server: " + e);
		}

		if (u.getUserInfo() == null)
			Util.fatalError("You must provide a secret to authenticate with");

		try {
			dout = new DataOutputStream(sock.getOutputStream());
			din = new DataInputStream(sock.getInputStream());

			if (din.readInt() != 0x00FABFAB)
				Util.fatalError("Server gave wrong magic");
			dout.writeInt(0xBAFBAF00);

			byte[] entropy = new byte[16];

			din.readFully(entropy);

			byte[] secret = u.getUserInfo().getBytes();
			if (secret == null)
				Util.fatalError("You must specify a secret");

			try {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(secret, 0, secret.length);
				md.update(entropy);
				byte[] hash = md.digest();
				dout.writeInt(hash.length);
				dout.write(hash);
			} catch (NoSuchAlgorithmException nsa) {
				Util.fatalError("Can't SHA?");
			}

			assert Util.debug(DebugFacility.FILER, "Connected!");
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
	}

	/**
	 * Close the DirectoryFiler connection
	 */
	public void close() {
		try {
			dout.write(0);
			sock.close();
			assert Util.debug(DebugFacility.FILER, "Closed database");
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
	}

	/**
	 * Request that the remote server shut down.
	 */
	public void halt() {
		try {
			assert Util.debug(DebugFacility.FILER, "Asking server to halt...");
			dout.write(1);
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
	}

	/**
	 * List remote databases available to you
	 * @return a list of all databases available
	 */
	public String[] ls() {
		try {
			dout.write(2);
			int nFiles = din.readInt();
			assert Util.debug(DebugFacility.FILER, "Receiving " + nFiles + " files");
			for (int i = 0; i < nFiles; i++) {
				int len = din.readInt();
				assert Util.debug(DebugFacility.FILER, "Filename length is " + len);
				byte[] name = new byte[len];
				din.readFully(name);
				System.out.println(new String(name));
			}
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
		return null;
	}

	/**
	 * Request a handle to a remote database by name and configuration
	 * @param name The name of the database to request
	 * @param config The configuration to request
	 * @return a reference to that Database
	 */
	public Database openDatabase(String name, Configuration config) {
		try {
			dout.write(3);
			dout.writeInt(name.length());
			dout.write(name.getBytes());
			byte[] confb = config.store();
			dout.writeInt(confb.length);
			dout.write(confb);
			int fd = din.readInt();
			assert Util.debug(DebugFacility.FILER, "Client opened " + name + " for fd " + fd);
			return new RemoteDatabase(fd,config);
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
		return null; // Not reached
	}

	private class RemoteDatabase extends Database {
		final int fd;
		BigInteger pos = BigInteger.ZERO;
		final Configuration conf;
		byte[] rawRecord;

		protected RemoteDatabase(int fd, Configuration config) {
			this.fd = fd;
			conf = config;
			rawRecord=new byte[conf.recordGroupByteLength];
		}

		@Override
		public void close() {
			try {
				dout.write(4);
				dout.writeInt(fd);
			} catch (IOException e) {
				Util.fatalError("IO error while communicating with server: "
						+ e);
			}
		}

		@Override
		public void flush() {
			try {
				dout.write(7);
				dout.writeInt(fd);
			} catch (IOException e) {
				Util.fatalError("IO error while communicating with server: "
						+ e);
			}
		}

		@Override
		public RecordGroup getRecordGroup(long loc) {
			try {
				// Util.debug("Trying to read "+loc);
				BigInteger bigLoc = BigInteger.valueOf(loc);
				if (bigLoc.compareTo(pos) != 0)
					seek(bigLoc);

				dout.write(5);
				dout.writeInt(fd);
				pos = pos.add(BigInteger.ONE);
				din.read(rawRecord);
				return new RecordGroup(conf, rawRecord);
			} catch (IOException e) {
				Util.fatalError("IO error while communicating with server: "
						+ e);
			}
			return null; // Unreachable
		}

		protected void seek(BigInteger loc) {
			try {
				dout.write(8);
				dout.writeInt(fd);
				byte[] locb = loc.toByteArray();
				dout.writeInt(locb.length);
				dout.write(locb);
				pos = loc;
			} catch (IOException e) {
				Util.fatalError("IO error while communicating with server: "
						+ e);
			}
		}

		@Override
		public void initialize(String url) {
			Util.fatalError("Already initialized");
		}

		@Override
		public void putRecordGroup(long loc, RecordGroup value) {
			try {
				BigInteger bigLoc = BigInteger.valueOf(loc);
				if(bigLoc.compareTo(pos) != 0)
					seek(bigLoc);
				dout.write(6);
				dout.writeInt(fd);
				value.getState().outputPaddedUnsignedBytes((OutputStream)dout, conf.recordGroupByteLength);
				pos = pos.add(BigInteger.ONE);
			} catch (IOException e) {
				Util.fatalError("IO error while communicating with server: "
						+ e);
			}
		}
	}
}
