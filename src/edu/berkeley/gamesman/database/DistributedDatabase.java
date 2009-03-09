package edu.berkeley.gamesman.database;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

/**
 * This database is supposed to be able to work over network 
 * More details will follow some other time... 
 * 
 * @author Alex Trofimov
 * @version .1 Alpha... not even working yet. Not even close.
 */
public class DistributedDatabase {
	
	// TODO: allow for IPv6
	// TODO: let it handle multiple jobs
	// TODO: link this to Memory Database or a child of it.
	
	static final int headerSize = 20; // Do not modify without modifying everything
	static final int blockSize = 64;
	
	static enum opCode { REQUEST, GRANT, REDIRECT, UPDATE, ACK, REGISTER, DONE };
	static enum dataKind {                  IP, PORT, BLOCK, JOB, BID, OPCODE, FLAG };
	static int[] dataOffset = new int[] {   12,   16,    20,   4,   8,      0,   12 }; 
	
	/* Header Layout:
	 * For: REQUEST, GRANT, ACK, DONE // Padding cannot be all 1s. 
	 * <- OPCODE (4 Bytes) -><- JOB ID (4 Bytes) -><- BLOCK ID (4 Bytes) -><- PADDING (8 Bytes) ->
	 * 
	 * For: UPDATE
	 * <- OPCODE (4 Bytes) -><- JOB ID (4 Bytes) -><- BLOCK ID (4 Bytes) -><- (-1) (4 Bytes) -><- PADDING (4 Bytes) -><- BLOCK DATA (blockSize Bytes) ->
	 * 
	 * For: REDIRECT, REGISTER
	 * <- OPCODE (4 Bytes) -><- JOB ID (4 Bytes) -><- BLOCK ID (4 Bytes) -><- IP (4 Bytes) -><- PORT (4 Bytes) -> 
	 */
	
	static public int retryAllowed = 15;				// # of retries before timing out.
	static public int retryTimeout = 1000 * 60 * 60; 	// one hour in milliseconds
	static public int sleepTimeout = 500; 				// half  a second
	
	static public boolean sendACKs = true;				// Whether after each packet an ACK should be sent.
	
	static public boolean debug = true;
	static public boolean error = true;
	
	public static void Debug(String msg) {
		if (debug)	System.out.println(Thread.currentThread().getName() + ": " + msg); }
	
	public static void Debug(Exception e, String msg) {
		if (error) {
			System.err.println(Thread.currentThread().getName() + ": " + msg);
			e.printStackTrace(); } }
	
	public static void Error(String msg) {
		if (error)	System.err.println(Thread.currentThread().getName() + ": " + msg); }
	
	public static void Error(Exception e, String msg) {
		if (error) {
			System.err.println(Thread.currentThread().getName() + ": " + msg);
			e.printStackTrace(); } }
	
	public static final byte[] intToBA(int i) {
		byte[] temp = new byte[4];
		temp[3] = (byte) (i & 0xFF);
		temp[2] = (byte) ((i >> 8) & 0xFF);
		temp[1] = (byte) ((i >> 16) & 0xFF);
		temp[0] = (byte) ((i >> 24) & 0xFF);
		return temp;
	}
	
	public static final void pushIntToBuf(dataKind kind, int data, byte[] buffer) {
		int offset = DistributedDatabase.dataOffset[kind.ordinal()];
		if (buffer.length < offset + 4) {
			DistributedDatabase.Debug("Buffer Length is too small to fit offset + 4 with offset = " + offset);
			return;
		}
		buffer[offset + 0] = (byte) ((data >> 24) & 0xFF);
		buffer[offset + 1] = (byte) ((data >> 16) & 0xFF);
		buffer[offset + 2] = (byte) ((data >> 8) & 0xFF);
		buffer[offset + 3] = (byte) ((data >> 0) & 0xFF);
	}
	
	public static final int pullIntFromBuf(dataKind kind, byte[] buffer) {
		int offset = DistributedDatabase.dataOffset[kind.ordinal()];		
		if (buffer.length < offset + 4) {
			DistributedDatabase.Debug("Read from a byte array failed. Length is too short with offset " + offset);
			return 0;
		}
		return buffer[offset + 3] + buffer[offset + 2] << 8 +
			   buffer[offset + 1] << 16 + buffer[offset] << 24;
	}
	
	public static final int byteArrayToInt(byte[] barray) {
		return barray[3] + barray[2] << 8 + barray[1] << 16 + barray[0] << 24;
	}
	
	public static final byte[] longToBA(long i) {
		byte[] temp = new byte[8];
		temp[7] = (byte) (i & 0xFF);
		temp[6] = (byte) ((i >> 8) & 0xFF);
		temp[5] = (byte) ((i >> 16) & 0xFF);
		temp[4] = (byte) ((i >> 24) & 0xFF);
		temp[3] = (byte) ((i >> 32) & 0xFF);
		temp[2] = (byte) ((i >> 40) & 0xFF);
		temp[1] = (byte) ((i >> 48) & 0xFF);
		temp[0] = (byte) ((i >> 56) & 0xFF);
		return temp;
	}

	/**
	 * For testing purposes only.
	 * @param args - not used.
	 */
	public static void main(String[] args) {
		//TODO: put a meaningful test here
	}
}
/* ************************************************************************ */
/* GamesMan Java Client
/* ************************************************************************ */
/** GamesMan Java Client Thread
 *  This thread runs a serverSocket and accepts incoming connections from other
 *  client threads.
 *  @author Alex Trofimov
 */
class GJDBClient extends Thread {
	
	private int Port;						// Port to bind to
	private SocketAddress Server;			// Address of the server to connect to
	private GJDBClientServerHelper Sender;  // Thread that is responsible for sending packets to the server.
	
	
	/** The run() method for this thread. This should not be called externally 
	 *  It doesn't do anything special, just wait for a connection, and when one
	 *  happens, assign a clientHelperThread to handle it. 
	 */
    public final void run() {
    	try {
    		ServerSocket serverSocket = new ServerSocket(Port);
    		Socket friendSocket;
    		GJDBClientClientHelper friendThread;
    		while(true) {
    			friendSocket = serverSocket.accept();
    			friendThread = new GJDBClientClientHelper(friendSocket);
    			friendThread.start();
    		}
    	} catch (IOException e) {
    		DistributedDatabase.Error(e, "Client Thread error");
    	}
    }

    /** Construct a Client Thread with a name
     * @param name - name of the thread.
     * @param bindingPort - Port that this server should listen to
     * @param serverAddress - InetSocketAddress that points to the server IP + Port
     */
    public GJDBClient (String name, int bindingPort, SocketAddress serverAddress) {
    	super(name);
    	Port = bindingPort;
    	Server = serverAddress; 
    	try {
    		Socket serverSocket = new Socket();
    		serverSocket.connect(serverAddress);
    		Sender = new GJDBClientServerHelper(serverSocket);
    		Sender.start();
    	} catch (IOException e) {
    		// This thread is now offline. There's no server connection
    	}

    }
    

/* ************************************************************************ */
/* GamesMan Java Client-Client Helper 
/* ************************************************************************ */
	/** GamesMan Java Client Helper Thread
	 *  This thread is only responsible for receiving an update to a block
	 *  that is owned by a client. It should die after that. 
	 */
	class GJDBClientClientHelper extends GJDBConnection {
		
		public GJDBClientClientHelper(Socket connection) {
			super  ("Client Helper Unknown",
					connection, 				// Connection with the client
					false); 					// This thread will listen (wait for updates)
		}
		
		@Override
		public void handleUpdate (int JobID, int BlockID, boolean blockAttached) {
			/* Ignore JobID
			 * TODO: need to bitwise & this with the block we have.
			 * if the block is in RAM (cache), update that.
			 * Otherwise, update it with the disk.
			 * If it's not found, it's a critical error.
			 * You will have to modify GJDBClient class for that, and
			 * also somehow synchronize it.
			 * Note: if blockAttached == true, then the contents of the block
			 * will be in this.blockBuffer
			 */
		}
		
	}

/* ************************************************************************ */
/* GamesMan Java Client-Server Helper 
/* ************************************************************************ */
	/** GamesMan Java Client-Server Helper Thread
	 *  
	 */
	class GJDBClientServerHelper extends GJDBConnection {
		
		public GJDBClientServerHelper(Socket connection) {
			super  ("Client Helper Unknown",
					connection, 				// Connection with the server
					true); 						// Tell it that this is a client thread (not server)
			sendRegister();						// Register this client with the server.
		}
		
		/** Send a register packet to the server, to let it know the connect IP + Port of this client */
		public void sendRegister() {
			byte[] localIP = (new Socket()).getLocalAddress().getAddress();
			int IPasInt = DistributedDatabase.byteArrayToInt(localIP);
			boolean success = readyToSay(DistributedDatabase.opCode.REGISTER, 0, 0, IPasInt, Port, null) && tellFriend(true);
			if (!success)
				DistributedDatabase.Error("Couldnt register with the server");
				
		}
		
		/** Ask the server for permission to own Block specified by BlockID 
		 * @param BlockID - id of the block that is being requested. */
		public void sendRequest(int BlockID) {
			boolean success = readyToSay(DistributedDatabase.opCode.REQUEST, 0, BlockID, 0, 0, null) && tellFriend(true);
			if (!success) DistributedDatabase.Error("Couldn't ask for block #" + BlockID);
		}
		
		@Override
		public void handleGrant  (int JobID, int BlockID) {
			/* TODO: Make this client believe that it is the owner of the block, meaning that it should
			 * dump it to disk, locally, and handle updates for it when other clients want to write
			 * to it. 
			 * This block should already be in memory.
			 */
		}
		
		@Override
		public void handleRedirect(int JobID, int BlockID, int IP, int Port) {
			// TODO: populate Block with something meaningful from this client's Cache.
			byte[] Block = new byte[DistributedDatabase.blockSize];
			
			boolean success;
			// Connect to another friend
			SocketAddress tempFriendAddress = new InetSocketAddress((new Integer(IP).toString()), Port);
			success = changeFriend(tempFriendAddress);
			if (!success) {	DistributedDatabase.Error("Couldn't connect to friend @ " + tempFriendAddress.toString());	return;	}			
			// Send it the updated block
			success = readyToSay(DistributedDatabase.opCode.UPDATE, 0, BlockID, 0, 0, Block) && tellFriend(true);
			if (!success) { DistributedDatabase.Error("Couldn't send update for block #" + BlockID); return; }
			// Connect back to the server
			success = changeFriend(Server);
			if (!success) DistributedDatabase.Error("Couldn't connect back to server.");
			
		}
		
	}
}
/* ************************************************************************ */
/* GamesMan Java DataBase Server 
/* ************************************************************************ */
/**
 * GamesMan Java DataBase Server Thread Class
 * This thread starts a ServerSocket and listens for incoming connections. 
 * When a client connects, this class starts a Helper Thread to handle
 * communication with each client.
 * TODO: Add support for multiple jobs, not just one.
 * TODO: This thread doesn't really exit gracefully, or at all.
 * @author Alex Trofimov
 */
class GJDBServer extends Thread {

	protected ArrayList<Long> socketList;					// List of Clients connected to
	protected TreeMap<Integer, Integer> BuffIDs;			// DataStructure that holds Buffer IDs
	protected int numClients;								// Number of clients connected;
	protected int port;										// Port to bind to
	
	/** The run() method for this thread. This should not be called externally */
    public final void run() {
    	try {
    		ServerSocket serverSocket = new ServerSocket(this.port);
    		Socket clientSocket;
    		int clientID;
    		GJDBServerHelper clientThread;
    		while(true) {
    			clientSocket = serverSocket.accept();
    			clientID = this.numClients++;
    			clientThread = new GJDBServerHelper(this, clientID, clientSocket);
    			clientThread.start();
    		}
    	} catch (IOException e) {
    		DistributedDatabase.Error(e, "Server error");
    	}
    }

    /** Construct a Server Thread with a name
     *  @param name - name of the thread.
     *  @param port - Port that this server should listen to
     */
    public GJDBServer (String name, int port) {
    	super(name);
    	socketList = (ArrayList<Long>) Collections.synchronizedList(new ArrayList<Long>());
    	BuffIDs = (TreeMap<Integer, Integer>) Collections.synchronizedMap(new TreeMap<Integer, Integer>());
    	this.port = port;
    	numClients = 0;
    }

/* ************************************************************************ */
/* GamesMan Java Server Helper Thread 
/* ************************************************************************ */
    /** GamesMan Java Server Helper
     *  This class instantiates Connection and Thread. It's main purpose is while
     *  a connection with a client is active, to listen for incoming packets
     *  and act accordingly. 
     *  Right now, all it does is given a request to own a block, it either 
     *  redirects the request to the block owner or grants the request, storing
     *  the information (BlockID, OwnerID tuple).
     *  In a case of a network interrupt, this thread dies. It is the client's job
     *  to reconnect. 
     */
    class GJDBServerHelper extends GJDBConnection {
    	
    	private GJDBServer server;			// Link to server instance.
    	private int ClientID;				// ID of the client connected to.

    	/** Create a new ServerHelper thread.
    	 * 
    	 * @param server - Pointer to the server, data of which this will modify
    	 * @param ClientID - ID of the client that this connection is with
    	 * @param clientSocket - An open Socket connection to the client.
    	 */
    	public GJDBServerHelper(GJDBServer server, int ClientID, Socket clientSocket) {
    		super  ("Server Helper " + ClientID,
    				clientSocket, 				// Connection with the client
    				false); 					// Tell it that this is a server thread (not client)
    		this.server = server;
    		this.ClientID = ClientID;
    	}
    	
    	@Override
    	public final void handleRequest(int JobID, int BlockID) {
    		// TODO: Do something with JobID
    		if (server.BuffIDs.containsKey(BlockID)) {  		// If someone else owns the Block 
    			int OwnerID = server.BuffIDs.get(BlockID);      // Get the ID of the Owner for the Block
    			sendRedirect(JobID, BlockID, OwnerID);			// Redirect client to ask Owner for Block
    		} else {											// If the block is free for the taking
    			server.BuffIDs.put(BlockID, ClientID);			// Store info internally
    			sendGrant(JobID, BlockID);						// Let the Client know it can keep the block.
    		}
    	}
    	
    	
    	/** Send the client a packet informing it that it now owns Block specified by BlockID */
    	final void sendGrant(int JobID, int BlockID) {
    		boolean success = readyToSay(DistributedDatabase.opCode.GRANT, JobID, BlockID, 0, 0, null) && tellFriend(true);
    		if (!success)
    			DistributedDatabase.Error("Couldnt grant Client #" + ClientID + " the Block #" + BlockID);
    	}
    	
    	/** Send the client a packet redirecting it's request to another client specified by OwnerID */
    	final void sendRedirect(int JobID, int BlockID, int OwnerID) {
    		Long ipAndPort = server.socketList.get(OwnerID);
    		int IP = (int) (ipAndPort >>> 32);
    		int Port = new Long(ipAndPort).intValue();
    		boolean success = readyToSay(DistributedDatabase.opCode.REDIRECT, JobID, BlockID, IP, Port, null) && tellFriend(true);
    		if (!success)
    			DistributedDatabase.Error("Couldnt Redirect Client #" + ClientID + " to Client #" + OwnerID + " for the Block #" + BlockID);
    	}
    }


        
}
/* ************************************************************************ */
/* GamesMan Java Connection Class Stub
/* ************************************************************************ */	
/** GJDB Connection Class Stub
 * 
 * @author Alex Trofimov
 *
 */
class GJDBConnection extends Thread {
	
	private SocketAddress ofFriend;				// Address of friend
	private Socket withFriend; 					// Connection with friend
	private InputStream fromFriend; 			// Incoming from friend
	private OutputStream toFriend;				// Outgoing to friend

	private byte[] headerBuffer;				// Buffer to store header info
    private byte[] blockBuffer;					// Buffer to store blocks
    private boolean blockAttached;				// Whether a block is attached.
	private DistributedDatabase.opCode[] opcodeList = DistributedDatabase.opCode.values();
	
	
	private boolean isListener;					// Whether this is a isListener or a talker.
												// More specifically, isListener halts on any error.
												// Talkers, try to reconnect, and keep talking.
	private boolean defaultAction;				// Whether to try to recover from errors.
	private boolean waitingForNod;				// Whether we're awaiting an ACK.
	private boolean moreToSay;					// Whether there's more to say to the friend;
	
	
	/** Make a Connection with another computer
	 * 
	 * @param threadName - name of the thread (useful for debugging)
	 * @param connection - Connection with a friend (an open socket)
	 * @param clientSide - whether this instance is running on client side
	 */
	public GJDBConnection(String threadName, Socket connection, boolean clientSide) {
		super(threadName);
		ofFriend = connection.getRemoteSocketAddress();
		isListener = !clientSide;
		defaultAction = clientSide;
		moreToSay = false;
		headerBuffer = new byte[DistributedDatabase.headerSize];
		blockBuffer = new byte[DistributedDatabase.blockSize];
	}
	
	/** Try to establish a connection with a friend.
	 *  Close the connection if it already exists. 
	 *  This method will keep trying to connect for a number of milliseconds 
	 *  specified in DistributedDatabase.retryTimeout. The total number of retries will
	 *  be DistributedDatabase.retryAllowed.
	 *  @return True if connection was successful, false otherwise
	 */
	public boolean connect() {
		if (isListener) return false; // Have no way of reconnecting.
		int waitingTime = DistributedDatabase.retryTimeout / (1 << DistributedDatabase.retryAllowed);
		int retryCount = 0;
		while (retryCount <= DistributedDatabase.retryAllowed) {
			retryCount ++;
			try {
				if (isConnected()) disconnect();
				withFriend = new Socket();
				withFriend.connect(ofFriend);
				toFriend = withFriend.getOutputStream();
				fromFriend = withFriend.getInputStream();
				return true;
			} catch (IOException e) {
				DistributedDatabase.Debug(e, "Connection Failed. Attempt #" + retryCount);
			}
			try {
				sleep(waitingTime);
			} catch (InterruptedException e) {
				DistributedDatabase.Error(e, "Sleep Interrupted");
			}
			waitingTime *= 2;
		}
		DistributedDatabase.Error("Connection Failed. Attempt #" + retryCount);
		return false;
	}
	
	/** Tell if the connection is active
	 * @return true if the connection is active and buffers work
	 */
	public boolean isConnected() {
		return withFriend != null && withFriend.isConnected() && 
			toFriend != null && fromFriend != null;
	}
	
	/** Close all handles and disconnect from a friend;
	 * 
	 * @return True if closed successfully.
	 */
	public boolean disconnect() {
		try {
			if (toFriend != null) toFriend.close();
			if (toFriend != null) fromFriend.close();
			if (withFriend.isConnected()) withFriend.close();
			return withFriend.isClosed();
		} catch (IOException e) {
			DistributedDatabase.Error("Error closing connection.");
			return false;
		}
	}
	
	/** Change the socket to be connected to another friend
	 *  If it fails to connect to the friend, it does not renew
	 *  connection with the previous friend.
	 *  @param otherFriend - the SocketAddress of the friend to connect to.
	 *  @return - true if connection was successful. False otherwise.
	 */
	public boolean changeFriend(SocketAddress otherFriend) {
		if (isListener) {
		DistributedDatabase.Debug("Listener thread tried to change friend");
			return false;
		}
		ofFriend = otherFriend;				// Think of a new friend
		return disconnect() && connect();   // Stop talking to oldFriend and start talking to the new one
	}
	
	/** Run the procedures of this class.
	 *  This is called internally. You shouldn't call it. 
	 */
	public void run() {
		if (connect()) {		
			if (isListener) while (listenToFriend()) continue;
			else while(tellFriend(true)) continue; // Wait for an ACK after every message sent
			DistributedDatabase.Debug("Conversation ended.");
			disconnect();
		}
	}
	
	/** Wait a message from friend and when one comes, handle it.
	 *  @return true if the message was handled OK, false is the message
	 *  was to stop talking to the friend.
	 */
	private boolean listenToFriend() {
		// Define some variables
		int bytesRead = 0;
		blockAttached = false;
		int BlockID, IP, Port, JobID;
		// Default action to take on something funny happening
		try {
			// Read Header
			bytesRead = this.fromFriend.read(this.headerBuffer);
			if (bytesRead < headerBuffer.length) {
				DistributedDatabase.Debug("Received a partial header of size " + bytesRead);
				return defaultAction;	}
			// Read Operation Code
			int opCode = DistributedDatabase.pullIntFromBuf(DistributedDatabase.dataKind.OPCODE, headerBuffer);
			if (this.opcodeList.length <= opCode) {
				DistributedDatabase.Debug("Unknown OPCODE: " + opCode);
				return defaultAction;	}
			DistributedDatabase.opCode Operation = opcodeList[opCode];
			// Read Block Number		
			BlockID = DistributedDatabase.pullIntFromBuf(DistributedDatabase.dataKind.BID, headerBuffer);
			// Read IP
			IP = DistributedDatabase.pullIntFromBuf(DistributedDatabase.dataKind.IP, headerBuffer);
			// Read Port
			Port = DistributedDatabase.pullIntFromBuf(DistributedDatabase.dataKind.PORT, headerBuffer);
			// Read JobID
			JobID = DistributedDatabase.pullIntFromBuf(DistributedDatabase.dataKind.JOB, headerBuffer);
			// Read BLOCK (if exists)
			blockAttached = IP == 0xFFFFFFFF; // FLAG == -1; (meaning flag is set).
			if (blockAttached) { // fromFriend.available() > 0) {
				bytesRead = this.fromFriend.read(this.blockBuffer);
				blockAttached = true;
				if (bytesRead < blockBuffer.length) {
					DistributedDatabase.Debug("Received a partial Block or oversized Header." + bytesRead);
					return defaultAction;	}	} // return defaultAction
			// Main Case Switch
			boolean noMissedNod = true;
			if (waitingForNod) {			// Awaiting an ACK
				waitingForNod = false;
				switch(Operation) {
				case DONE: handleDone(); return false; 
				case ACK: return true;		// ACK Received
				default:
					DistributedDatabase.Debug("Misplaced OPCODE: " + Operation.name() + ". Waiting for a nod.");
					noMissedNod = false;	// ACK never arrived.
				}
			} 
			// Non-ACK case switch (stuff goes here, even after a failed ACK);
			switch(Operation) { // Open for communication
			case REQUEST:  handleRequest (JobID, BlockID); 			 		break;
			case GRANT:	   handleGrant   (JobID, BlockID);    		 		break;
			case REDIRECT: handleRedirect(JobID, BlockID, IP, Port); 		break;
			case UPDATE:   handleUpdate  (JobID, BlockID, blockAttached);   break;
			case REGISTER: handleRegister(JobID, BlockID, IP, Port); 		break;
			case DONE: 	   handleDone(); return false;	// Signal end of this thread execution.
			case ACK: 									// Gets handled in waitForFriend()
			default:
				DistributedDatabase.Debug("Misplaced OPCODE: " + Operation.name());
				return defaultAction && noMissedNod;
			}
			if (DistributedDatabase.sendACKs) nodToFriend(); 				// Send ACK. Note that ACKs don't get ACKs :D
			return true;
		} catch (IOException e) {
			DistributedDatabase.Debug(e, "Friend hiccuped.");
			return defaultAction && connect(); // Maybe reconnect?
		}
	}
	
	/** Wait for a friend to acknowledge (send and ACK)
	 *  @return true if an ACK was received, false otherwise
	 */
	protected boolean waitForNod() {
		if (!DistributedDatabase.sendACKs) return true;  // Nodding disabled
		waitingForNod = true;
		return listenToFriend();
	}
	
	/** Send a nod (ACK) to the friend */
	synchronized private boolean nodToFriend() {
		// First prepare the packet, and then send it. I hope this works.
		// Note that there is no "waitForNod".. otherwise this will loop!
		// TODO: Prove to yourself that this works (or that it doesn't and fix it).
		return readyToSay(DistributedDatabase.opCode.ACK, 0, 0, 0, 0, null) && tellFriend(false);
	}
	
	/** Take data and send it off.
	 *  It is equivalent to a Producer in the Consumer-Producer model.
	 *   
	 * @param Operation - which OPCODE should be executed
	 * @param JobID - ID of the job that's being worked on. Set to 0 if doesn't matter.
	 * @param BlockID - ID of the block that is either being passed or referenced.
	 * @param IP - IP of the friend referencing. Set to 0 if doesn't matter.
	 * @param Port - Port of the friend referencing. Set to 0 if doesn't matter.
	 * @param Block - byte array with the block data that should be sent. Set to null if not used.
	 */
	synchronized protected boolean readyToSay(DistributedDatabase.opCode Operation, int JobID, int BlockID, int IP, int Port, byte[] Block) {
		// Set up Variables
		boolean toReturn = defaultAction;
		// Wait for the "Consumer" to be come ready
		if (moreToSay) {
			try {	wait();	} 
			catch (InterruptedException e) {
				DistributedDatabase.Debug(e, "Wait Interrupted.");	}
		}
		moreToSay = true; // A new message is being prepared.
		// Attach the Block if applicable (Populating the BlockBuffer)
		blockAttached = false;
		if (Block != null) {
			if (Block.length != blockBuffer.length) {
				toReturn = false;
				DistributedDatabase.Error("Block is not same length as buffer");
			} else {
				blockAttached = true;
				for (int i = 0; i < Block.length; i ++)
					blockBuffer[i] = Block[i];
				DistributedDatabase.pushIntToBuf(DistributedDatabase.dataKind.FLAG, 0xFFFFFFFF, headerBuffer);				
			}
		}
		blockBuffer = Block.clone();
		// Populating the HeaderBuffer
		headerBuffer = new byte[headerBuffer.length];
		DistributedDatabase.pushIntToBuf(DistributedDatabase.dataKind.OPCODE, Operation.ordinal(), headerBuffer);		
		DistributedDatabase.pushIntToBuf(DistributedDatabase.dataKind.BID, BlockID, headerBuffer);
		if (JobID != 0) DistributedDatabase.pushIntToBuf(DistributedDatabase.dataKind.JOB, JobID, headerBuffer);
		if (IP != 0) DistributedDatabase.pushIntToBuf(DistributedDatabase.dataKind.IP, IP, headerBuffer);
		if (Port != 0) DistributedDatabase.pushIntToBuf(DistributedDatabase.dataKind.PORT, Port, headerBuffer);
		notify();
		return toReturn;
	}
	
	/** Wait for an input to be ready and send it off
	 * 
	 * @return True if the sending was OK (ACK received and all), false otherwise.
	 */
	synchronized protected boolean tellFriend(boolean awaitNod) {
		boolean toReturn = defaultAction;
		try {
			if (!moreToSay)
				try {	wait();	} 
				catch (InterruptedException e) {
					DistributedDatabase.Debug(e, "Wait Interrupted.");	}
			if (moreToSay) {
				toFriend.write(headerBuffer);
				if (blockAttached)
					toFriend.write(blockBuffer);
				toFriend.flush();
				if (awaitNod && DistributedDatabase.sendACKs) toReturn = waitForNod();
				else toReturn = true;
			} else {
				toReturn = true;
			}
		} catch (IOException e) {
			DistributedDatabase.Error(e, "Writing to buffer failed.");
			toReturn = defaultAction;
		}
		moreToSay = false;
		notify();
		return toReturn;
	}
	
	/** handle an incoming Request to own a Block 
	 * @param JobID - not used; 
	 * @param BlockID - ID of the block referenced; */
	public void handleRequest (int JobID, int BlockID) {
		DistributedDatabase.Error("handleRequest not implemented."); }
	
	/** handle the fact that we own block specified by BlockID 
	 * @param JobID - not used; 
	 * @param BlockID - ID of the block referenced; */
	public void handleGrant   (int JobID, int BlockID) {
		DistributedDatabase.Error("handleGrant not implemented."); }
	
	/** handle being redirected to another friend  
	 * @param JobID - not used; 
	 * @param BlockID - ID of the block referenced;
	 * @param IP - specifies the IP of a friend to connect to;
	 * @param Port - specifies he Port of a friend to connect to;
	 */
	public void handleRedirect(int JobID, int BlockID, int IP, int Port) {
		DistributedDatabase.Error("handleRedirect not implemented."); }
	
	/** handle an incoming update for a block you own 
	 * @param JobID - not used; 
	 * @param BlockID - ID of the block referenced; 
	 * @param blockAttached - True if blockBuffer has been populated; */
	public void handleUpdate (int JobID, int BlockID, boolean blockAttached) {
		DistributedDatabase.Error("handleUpdate not implemented."); }
	
	/** handle a friend's address.  
	 * @param JobID - not used; 
	 * @param BlockID - ID of the block referenced; 
	 * @param IP - Integer value of the IP of the client that's registering;
	 * @param Port - Port on which the client accepts incoming connections; */
	public void handleRegister(int JobID, int BlockID, int IP, int Port) {
		DistributedDatabase.Error("handleRegister not implemented."); }
	
	/** handle an order to shut down */
	public void handleDone() {
		nodToFriend(); // Let the friend know that the command was received.
	}
}
