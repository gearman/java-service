/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Copyright (C) 2009 by Robert Stewart <robert@wombatnation.com>
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman;

import java.net.InetSocketAddress;
import java.util.Set;

import org.gearman.core.GearmanCompletionHandler;

/**
 *
 * A Gearman Worker is responsible for executing jobs it receives from the Job
 * Server. A Worker registers with the Job Server the types of jobs that it may
 * execute, the server will use this information, along with other attributes,
 * to determine which Worker will execute a particular job request. As data is
 * generated or as a job's state changes, the worker passes this information
 * back to the Job Server.
 */
public interface GearmanWorker extends GearmanService{

    /**
     * Register a new Gearman Job Server with the worker.
     * @param conn {@link GearmanJobServerConnection} connected to the
     *          Gearman Job Server
     *
     * @return returns true if a connection to the server was established and
     *         the server was added to the worker, else false.
     *
     * @throws IllegalArgumentException If an invalid connection has been
     *         specified.
     *
     * @throws IllegalStateException If the worker has already been stopped.
     */
    public <A> void addServer(InetSocketAddress adrs, A att, GearmanCompletionHandler<A> callback);
    public <A> void addServer(String host, int port, A att, GearmanCompletionHandler<A> callback);
    public <A> void addServer(GearmanServer srvr, A att, GearmanCompletionHandler<A> callback);
    

    public void setMaximumConcurrency(int threads);
    public int getMaximumConcurrency();
    /**
     * Has a connection to the specified Gearman Job Server been registerd with
     * this worker.
     *
     * @param conn The connection to the specified Gearman Job Server.
     *
     * @return  True if the Gearman Job Server has been registerd with the worker,
     * otherwise false.
     */
    public boolean hasServer(InetSocketAddress address);
    public boolean hasServer(String host, int port);
    public boolean hasServer(GearmanServer srvr);    

    /**
     * Registers a particular {@link GearmanFunction} with the worker. Once a
     * function has been registered with a worker, the worker is capable of
     * executing any {@link org.gearman.client.Job} that matches the
     * registered function. Upon registering a function, the Worker notifies all
     * Gearman Job Servers that is can accept any job that contains the
     * applicable function.
     *
     * @param function The function being registered with the Worker.
     */
    public GearmanFunction addFunction(String name, GearmanFunction function);
    public GearmanFunction addFunction(String name, GearmanFunction function, long timout);
    
    public GearmanFunction getFunction(String name);
    public long getFunctionTimeout(String name);

    /**
     * Retrieve the names of all functions that have been registered with this
     * worker. If no functions have been registered, any empty set should be
     * returned.
     *
     * @return The name of all registered functions.
     */
    public Set<String> getRegisteredFunctions();

    /**
     * Set the ID for a particular worker instance.  This enables monitoring and
     * reporting commands to uniquely identify specific workers.
     *
     * @param id The ID.
     */
    public void setWorkerID(String id);

    /**
     * Retrieves the ID used by this worker instance.
     *
     * @return worker ID
     */
    public String getWorkerID();
    
    public boolean removeServer(InetSocketAddress adrs);
    public boolean removeServer(String host, int port);
    public boolean removeServer(GearmanServer server);
    public void removeAllServers();
    
    public int getServerCount();
    
    /**
     * Unregisters a particular {@link GearmanFunction} from the worker. Once
     * a function has been unregistered from the Worker, a Worker will no longer
     * accept jobs which require the execution of the unregistered function.
     *
     * @param functionName
     */
    public boolean removeFunction(String functionName);

    /**
     * Unregisters all{@link GearmanFunction} from the worker. The effect of
     * which is that the worker will not execute any new jobs.
     *
     */
    public void unregisterAll();

}
