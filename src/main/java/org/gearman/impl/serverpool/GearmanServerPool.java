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

package org.gearman.impl.serverpool;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanClient;
import org.gearman.GearmanLostConnectionPolicy;
import org.gearman.GearmanServer;
import org.gearman.GearmanService;
import org.gearman.GearmanWorker;

/**
 * Both {@link GearmanClient}s and {@link GearmanWorker}s are <code>GearmanServerPool</code>s.
 * A gearman server pool allows the user manage the servers within a particular service 
 * @author isaiah
 */
public interface GearmanServerPool extends GearmanService {
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
	 * Returns the default reconnect period
	 * @param unit
	 * 		The time unit
	 * @return
	 * 		The about of time before the service attempts to reconnect to a disconnected server
	 */
	public long getReconnectPeriod(TimeUnit unit);
	
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
	
	public boolean removeServer(GearmanServer server);
	public void setClientID(String id);
	public String getClientID();
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
	
	/**
	 * Sets the default reconnect period. When a connection is unexpectedly disconnected, the
	 * will wait a period of time before attempting to reconnect unless otherwise specified
	 * by the {@link GearmanLostConnectionPolicy}
	 * @param time
	 * 		The amount of time before a reconnect is attempted unless otherwise specified
	 * 		by the {@link GearmanLostConnectionPolicy}
	 * @param unit
	 * 		The time unit
	 */
	public void setReconnectPeriod(long time, TimeUnit unit);
}
