/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
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
