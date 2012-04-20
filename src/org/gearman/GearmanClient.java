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

package org.gearman;

import java.io.IOException;
import java.util.Collection;

/**
 * The gearman client is used to submit jobs to the job server.
 * @author isaiah
 */
public interface GearmanClient extends GearmanService {
	
	/**
	 * Polls for the job status. This is a blocking operation. The current thread may block and wait
	 * for the operation to complete
	 * @param jobHandle
	 * 		The job handle of the of the job in question. 
	 * @return
	 * 		The job status of the job in question.
	 * @throws IOException
	 * 		If an I/O exception occurs while performing this operation 
	 */
	public GearmanJobStatus getStatus(byte[] jobHandle) throws IOException;
	
	/**
	 * Sends a job to a registered job server.
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @return
	 * 		The job return used to poll result data
	 * @throws NullPointerException
	 * 		if the function name is <code>null</code>
	 */
	public GearmanJobReturn submitJob(String functionName, byte[] data);
	
	/**
	 * Sends a job to a registered job server.
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param attachment
	 * 		An object used to identify this job from within the 
	 * @param callback
	 * 		An asynchronous callback object used to receive result data
	 * @return
	 * 		A joining object used to synchronize jobs
	 * @throws NullPointerException
	 * 		if the function name is <code>null</code> or the callback is null
	 */
	public <A> GearmanJoin<A> submitJob(String functionName, byte[] data, A attachment, GearmanJobEventCallback<A> callback);
	
	/**
	 * Sends a job to a registered job server.
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @return
	 * 		The job return used to poll result data
	 * @throws NullPointerException
	 * 		if the function name is <code>null</code>
	 */
	public GearmanJobReturn submitJob(String functionName, byte[] data, GearmanJobPriority priority);
	
	/**
	 * Sends a job to a registered job server.
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @param attachment
	 * 		An object used to identify this job from within the
	 * @param callback
	 * 		An asynchronous callback object used to receive result data
	 * @return
	 * 		A joining object used to synchronize jobs
	 * @throws NullPointerException
	 * 		If the function name or callback is <code>null</code>
	 */
	public <A> GearmanJoin<A> submitJob(String functionName, byte[] data, GearmanJobPriority priority, A attachment, GearmanJobEventCallback<A> callback);
	
	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @return
	 * 		The job return used to poll submit operation status
	 * @throws NullPointerException
	 * 		If the function name is <code>null</code>
	 */
	public GearmanJobReturn submitBackgroundJob(String functionName, byte[] data);
	
	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param attachment
	 * 		An object used to identify this job from within the
	 * @param callback
	 * 		An asynchronous callback object used to receive submit operation status
	 * @return
	 * 		A joining object used to synchronize jobs
	 * @throws NullPointerException
	 * 		if the function name or callback is <code>null</code>
	 */
	public <A> GearmanJoin<A> submitBackgroundJob(String functionName, byte[] data, A attachment, GearmanJobEventCallback<A> callback);
	
	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @return
	 * 		The job return used to poll submit operation status
	 * @throws NullPointerException
	 * 		If the function name is <code>null</code>
	 */
	public GearmanJobReturn submitBackgroundJob(String functionName, byte[] data, GearmanJobPriority priority);
	
	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @param attachment
	 * 		An object used to identify this job from within the
	 * @param callback
	 * 		An asynchronous callback object used to receive submit operation status
	 * @return
	 * 		A joining object used to synchronize jobs
	 * @throws NullPointerException
	 * 		if the function name or callback is <code>null</code>
	 */
	public <A> GearmanJoin<A> submitBackgroundJob(String functionName, byte[] data, GearmanJobPriority priority, A attachment, GearmanJobEventCallback<A> callback);
	
	/**
	 * Adds a {@link GearmanServer} to the service.<br>
	 * <br>
	 * Note: connections are not made to the server at this time. A connection is only established when needed
	 * @param server
	 * 		The gearman server to add
	 * @return
	 * 		<code>true</code> if the server was added to the service
	 */
	public boolean addServer(GearmanServer server);
	
	/**
	 * Returns the number of servers managed by this service
	 * @return
	 * 		The number of servers managed by this service
	 */
	public int getServerCount();
	
	/**
	 * Removes all servers from this service
	 */
	public void removeAllServers();
	
	/**
	 * Removes the given server from the list of available server to
	 * @param server
	 * 		The server to remove
	 * @return
	 * 		<code>true</code> if the service contained the given server and it was successfully removed. <code>false</code> if the service did not contain the given server
	 */
	public boolean removeServer(GearmanServer server);
	
	/**
	 * Sets the client ID
	 * @param id
	 * 		the new client ID
	 */
	public void setClientID(String id);
	
	/**
	 * Gets the current client ID
	 * @return
	 * 		The current client ID
	 */
	public String getClientID();
	
	/**
	 * Tests if this client has the given server
	 * @param server
	 * 		The given server
	 * @return
	 * 		<code>true</code> if this client contains the given server
	 */
	public boolean hasServer(GearmanServer server);
	
	/**
	 * Returns the collection of servers this service is managing
	 * @return
	 * 		The collection of servers this service is managing
	 */
	public Collection<GearmanServer> getServers();
	
	/**
	 * Sets the {@link GearmanLostConnectionPolicy}. The lost connection policy describes
	 * what should be done in the event that the server unexpectedly disconnects
	 * @param policy
	 * 		The policy for handling unexpected disconnects
	 */
	public void setLostConnectionPolicy(GearmanLostConnectionPolicy policy);
}