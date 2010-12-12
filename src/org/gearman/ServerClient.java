package org.gearman;

import java.io.IOException;

import org.gearman.core.GearmanCompletionHandler;
import org.gearman.core.GearmanPacket;


/**
 * The ServerClient represent the client on the server size.  It holds state
 * information, like what functions it can perform, and connection status.
 * @author isaiah
 *
 */
interface ServerClient {
	
	/**
	 * Adds a disconnect listener.  The listener will be triggered on the event this ServerClient
	 * @param listener
	 * 		The disconnect listener
	 * @return
	 * 		true if this client did not already contain the specified listener
	 */
	public boolean addDisconnectListener (ServerClientDisconnectListener listener);
	
	/**
	 * Adds a function to the set of functions for this ServerClient
	 * @param func
	 * 		The function to add
	 * @return
	 * 		true if the function was not already in the function set for this ServerClient
	 */
	public boolean can_do(ServerFunction func);
	
	/**
	 * Closes the current connection between the server and the client.
	 */
	public void close();
	
	/**
	 * Returns the current client ID
	 * @return the current client ID
	 */
	public String getClientId();
	
	/**
	 * Returns an Iterable for the set of functions this ServerClient can perform
	 * @return an Iterable for the set of functions this ServerClient can perform.
	 */
	public Iterable<ServerFunction> getFunctions();
	
	/**
	 * Returns the port number this ServerClient is using/used to connect to the Java
	 * Gearman Server
	 * 
	 * @return
	 * 		The port number this ServerClient is using/used to connect to the Java
	 * 		Gearman Server
	 */
	public int getLocalPort();
	
	/**
	 * Returns the port number for this ServerClient
	 * 
	 * @return
	 * 		The port number this ServerClient is using/used to connect to the Java
	 * 		Gearman Server
	 */
	public int getPort();
	
	/**
	 * Returns the current status for this ServerClient. The status is a utf-8 string as a byte
	 * array in the following format:
	 * 
	 * FD IP-ADDRESS CLIENT-ID : FUNCTION ...
	 * 
	 * FD = File Descriptor (Not applicable to the Java version of the server, just use "NA")
	 * IP-ADDRESS = The IP address of the client
	 * CLIENT-ID = The client ID
	 * FUNCTION ... = The function names associated with this ServerClient
	 * 
	 * @return
	 * 		The status string as specified in the description
	 */
	public GearmanPacket getStatus();
	
	/**
	 * Tries to grab a job from one of the functions specified by the addFunction() method 
	 * @return
	 * 		Returns the next available job or null if no job is available
	 */
	public void grabJob();
	
	/**
	 * Tests if the this ServerClient has been closed
	 * @return
	 * 		true if this ServerClient is closed
	 */
	public boolean isClosed();
	
	/**
	 * Tests if exception packets should be forwarded to the client
	 * @return
	 * 		True if WORK_EXCEPTION packets should be forwarded to the client,
	 * 		false if not
	 */
	public boolean isForwardsExceptions();

	/**
	 * Sends a NOOP packet to the client if the ServerClient is sleeping at the time of the
	 * call 
	 */
	public void noop();
	
	/**
	 * Removes a disconnect listener from this ServerClient
	 * @param listener
	 * 		The listener to remove
	 * @return
	 * 		true if this ServerClient contained the specified listener
	 */
	public boolean removeDisconnectListener (ServerClientDisconnectListener listener);
	
	/**
	 * Removes a function from the set of functions for this ServerClient 
	 * @param func
	 * 		The function to remove
	 * @return
	 * 		true if this ServerClient contained the specified function
	 */
	public boolean cant_do(ByteArray funcName);
	
	/**
	 * Removes all function
	 */
	public void reset();
		
	/**
	 * Sends exception packets to the client.  If this ServerClient does not forward
	 * exception packets, the packet will be dropped
	 * @param packet
	 * 		The exception packet to send to the client
	 * @throws IOException
	 * 		if any I/O exception occurs.  Namely, the ServerClient is closed
	 */
	public <A> void sendExceptionPacket(GearmanPacket packet, A att, GearmanCompletionHandler<A> callback);
			
	/**
	 * Sends packets to the client asynchronously.<br>
	 * <br>
	 * Note: I/O exceptions are not caught by this method.  If the control flow depends on the success
	 * or failure of this method, you should use the synchronous version
	 * @param packet
	 * 		The exception packet to send to the client
	 */
	public <A> void sendPacket(GearmanPacket packet, A att, GearmanCompletionHandler<A> callback);
	
	/**
	 * Set's the client's ID
	 * @param id
	 * 		The new ID for this ServerClient
	 */
	public void setClientId(String id);
	
	/**
	 * Specifies if exception packets should be forwarded to the user client
	 * @param value
	 * 		If true, exception packets will be sent to the client.  If false,
	 * 		exception packets will be dropped
	 */
	public void setForwardsExceptions(boolean value);
	
	/**
	 * Places the ServerClient in sleep mode.
	 */
	public void sleep();
	
	public void grabJobUniq();
}
