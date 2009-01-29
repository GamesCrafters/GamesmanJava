package edu.berkeley.gamesman.database.filer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import edu.berkeley.gamesman.core.Record;
import edu.berkeley.gamesman.core.Database;
import edu.berkeley.gamesman.core.Values;
import edu.berkeley.gamesman.core.Configuration;
import edu.berkeley.gamesman.util.Util;

public final class DirectoryFilerClient {

	protected Socket sock;
	protected DataOutputStream dout;
	protected DataInputStream din;
	protected Random r = new Random();

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

		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
	}

	public void close() {
		try {
			dout.write(0);
			sock.close();
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
	}

	public void halt() {
		try {
			dout.write(1);
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
	}

	public String[] ls() {
		try {
			dout.write(2);
			int nFiles = din.readInt();
			Util.debug("Receiving " + nFiles + " files");
			for (int i = 0; i < nFiles; i++) {
				int len = din.readInt();
				Util.debug("Filename length is " + len);
				byte[] name = new byte[len];
				din.readFully(name);
				System.out.println(new String(name));
			}
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
		return null;
	}

	public Database openDatabase(String name, Configuration config) {
		try {
			dout.write(3);
			dout.writeInt(name.length());
			dout.write(name.getBytes());
			dout.writeInt(config.getConfigString().length());
			dout.write(config.getConfigString().getBytes());
			int fd = din.readInt();
			Util.debug("Client opened " + name + " for fd " + fd);
			return new RemoteDatabase(fd);
		} catch (IOException e) {
			Util.fatalError("IO error while communicating with server: " + e);
		}
		return null; // Not reached
	}

	private class RemoteDatabase extends Database {
		int fd;
		BigInteger pos = BigInteger.ZERO;

		protected RemoteDatabase(int fd) {
			this.fd = fd;
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
		public Record getValue(BigInteger loc) {
			try {
				// Util.debug("Trying to read "+loc);

				if (loc.compareTo(pos) != 0)
					seek(loc);

				dout.write(5);
				dout.writeInt(fd);
				pos = pos.add(BigInteger.ONE);
				return Values.Invalid.wrapValue(din.readByte()); // TODO: hacky
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
		public void initialize(String url, Configuration config) {
			Util.fatalError("Already initialized");
		}

		@Override
		public void setValue(BigInteger loc, Record value) {
			try {
				if(loc.compareTo(pos) != 0) seek(loc);
				dout.write(6);
				dout.writeInt(fd);
				value.write(dout);
				pos = pos.add(BigInteger.ONE);
			} catch (IOException e) {
				Util.fatalError("IO error while communicating with server: "
						+ e);
			}
		}

	}
}