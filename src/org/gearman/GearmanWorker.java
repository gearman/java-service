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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A Gearman Worker is responsible for executing the jobs it receives from the
 * job server. A Worker registers with the Job Server the types of jobs that it
 * may execute, the server will use this information, along with other
 * attributes, to determine which Worker will execute a particular job request.
 * As data is generated or as a job's state changes, the worker passes this
 * information back to the Job Server.
 */
public interface GearmanWorker extends GearmanService {

	/**
	 * Sets the maximum number of jobs that can execute at a given time
	 * 
	 * @param maxConcurrentJobs
	 *            The maximum number of jobs that can execute at a given time
	 */
	public void setMaximumConcurrency(int maxConcurrentJobs);

	/**
	 * The maximum number of jobs that may execute at a given time
	 * 
	 * @return The number of jobs that that may execute concurrently
	 */
	public int getMaximumConcurrency();

	/**
	 * Registers a particular {@link GearmanFunction} with the worker. Once a
	 * function has been registered with a worker, the worker is capable of
	 * executing any job that matches the registered function. Upon registering
	 * a function, the Worker notifies all Gearman Job Servers that is can
	 * accept any job that contains the applicable function.
	 * 
	 * @param name
	 *            The gearman function name
	 * @param function
	 *            The function being registered with the worker.
	 * @return The gearman function who was previously assigned to the given
	 *         function name
	 */
	public GearmanFunction addFunction(String name, GearmanFunction function);

	/**
	 * Returns the gearman function associated with the given function name
	 * 
	 * @param name
	 *            The function name
	 * @return The gearman function registered with the given function name
	 */
	public GearmanFunction getFunction(String name);

	/**
	 * Retrieve the names of all functions that have been registered with this
	 * worker. If no functions have been registered, any empty set should be
	 * returned.
	 * 
	 * @return The name of all registered functions.
	 */
	public Set<String> getRegisteredFunctions();

	/**
	 * Unregisters a particular {@link GearmanFunction} from the worker. Once a
	 * function has been unregistered from the Worker, a Worker will no longer
	 * accept jobs which require the execution of the unregistered function.
	 * 
	 * @param functionName
	 *            The name of the function to unregister
	 */
	public boolean removeFunction(String functionName);

	/**
	 * Unregisters all{@link GearmanFunction} from the worker. The effect of
	 * which is that the worker will not execute any new jobs.
	 */
	public void removeAllFunctions();

	/**
	 * Adds a {@link GearmanServer} to the service.<br>
	 * <br>
	 * Note: connections are not made to the server at this time. A connection
	 * is only established when needed
	 * 
	 * @param server
	 *            The gearman server to add
	 * @return <code>true</code> if the server was added to the service
	 */
	public boolean addServer(GearmanServer server);

	/**
	 * Returns the default reconnect period
	 * 
	 * @param unit
	 *            The time unit
	 * @return The about of time before the service attempts to reconnect to a
	 *         disconnected server
	 */
	public long getReconnectPeriod(TimeUnit unit);

	/**
	 * Returns the number of servers managed by this service
	 * 
	 * @return The number of servers managed by this service
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
	 * 
	 * @return The collection of servers this service is managing
	 */
	public Collection<GearmanServer> getServers();

	/**
	 * Sets the {@link GearmanLostConnectionPolicy}. The lost connection policy
	 * describes what should be done in the event that the server unexpectedly
	 * disconnects
	 * 
	 * @param policy
	 *            The policy for handling unexpected disconnects
	 */
	public void setLostConnectionPolicy(GearmanLostConnectionPolicy policy);

	/**
	 * Sets the default reconnect period. When a connection is unexpectedly
	 * disconnected, the will wait a period of time before attempting to
	 * reconnect unless otherwise specified by the
	 * {@link GearmanLostConnectionPolicy}
	 * 
	 * @param time
	 *            The amount of time before a reconnect is attempted unless
	 *            otherwise specified by the {@link GearmanLostConnectionPolicy}
	 * @param unit
	 *            The time unit
	 */
	public void setReconnectPeriod(long time, TimeUnit unit);
}
