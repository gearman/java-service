/*
 * Copyright (c) 2012, Isaiah van der Elst (isaiah.v@comcast.net)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.gearman.impl.server.local;

import java.io.IOException;

import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanConnection.SendCallbackResult;
import org.gearman.impl.util.ByteArray;


/**
 * This Client class represent the client on the server size.  It holds state
 * information, like what functions it can perform, and connection status.
 * @author isaiah
 *
 */
interface Client {
	
	/**
	 * Adds a disconnect listener.  The listener will be triggered on the event this ServerClient
	 * @param listener
	 * 		The disconnect listener
	 * @return
	 * 		true if this client did not already contain the specified listener
	 */
	public boolean addDisconnectListener (ClientDisconnectListener listener);
	
	/**
	 * Adds a function to the set of functions for this ServerClient
	 * @param func
	 * 		The function to add
	 * @return
	 * 		true if the function was not already in the function set for this ServerClient
	 */
	public boolean can_do(Function func);
	
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
	public Iterable<Function> getFunctions();
	
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
	public boolean removeDisconnectListener (ClientDisconnectListener listener);
	
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
	public void sendExceptionPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback);
			
	/**
	 * Sends packets to the client asynchronously.<br>
	 * <br>
	 * Note: I/O exceptions are not caught by this method.  If the control flow depends on the success
	 * or failure of this method, you should use the synchronous version
	 * @param packet
	 * 		The exception packet to send to the client
	 */
	public void sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback);
	
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
