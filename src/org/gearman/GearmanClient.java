/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
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
	 */
	public GearmanJobReturn submitJob(String functionName, byte[] data);
	
	public <A> void submitJob(String functionName, byte[] data, A attachment, GearmanJobEventCallback<A> callback);
	public <A> void submitJob(String functionName, byte[] data, GearmanJobPriority priority, A attachment, GearmanJobEventCallback<A> callback);
	
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
	 */
	public GearmanJobReturn submitJob(String functionName, byte[] data, GearmanJobPriority priority);
	
	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @return
	 * 		The job return used to poll submit status
	 */
	public GearmanJobReturn submitBackgroundJob(String functionName, byte[] data);
	
	public <A> void submitBackgroundJob(String functionName, byte[] data, A attachment, GearmanJobEventCallback<A> callback);
	public <A> void submitBackgroundJob(String functionName, byte[] data, GearmanJobPriority priority, A attachment, GearmanJobEventCallback<A> callback);
	
	/**
	 * Submits a background job to a registered job server
	 * @param functionName
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param priority
	 * 		gearman job priority
	 * @return
	 * 		The job return used to poll submit status
	 */
	public GearmanJobReturn submitBackgroundJob(String functionName, byte[] data, GearmanJobPriority priority);
	
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