/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

import java.io.IOException;

import org.gearman.config.GearmanProperties;
import org.gearman.config.PropertyName;

/**
 * A <code>Gearman</code> object defines a gearman systems and creates gearman
 * services. These services include {@link GearmanWorker}s,
 * {@link GearmanClient}s, and {@link GearmanServer}s. All services created by
 * the same <code>Gearman</code> object are said to be in the same system, and
 * all services in the same system share system wide thread resources.
 * 
 * @author isaiah.v
 */
public abstract class Gearman implements GearmanService{
	
	/**
	 * Creates a new {@link Gearman} instance
	 * @return
	 * 		A new {@link Gearman} instance  
	 */
	public static Gearman createGearman() {
		
		try {
			final String className = GearmanProperties.getProperty(PropertyName.GEARMAN_CLASSNAME);
			
			final Class<?> implClass = Class.forName(className);
			final Object o = implClass.newInstance();
			
			return (Gearman)o;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			
			/*
			 * If an exception occurs, there is something wrong with the class name or gearman
			 * implementation. In both cases the issue is unrecoverable. An error must be thrown 
			 */
			
			throw new Error("failed to initialize gearman",e);
		}
	}
	
	/**
	 * Returns the current java-gearman-service version
	 * @return
	 * 		The current version
	 */
	public abstract String getVersion();
	
	/**
	 * Returns the default port number
	 * @return
	 * 		the default port number
	 */
	public abstract int getDefaultPort();
	
	/**
	 * Starts a new local gearman job server running in the current address space, using
	 * the default port
	 * @return
	 * 		A new gearman server instance
	 * @throws IOException
	 * 		If an IO exception occurs while attempting to open the default port
	 * @see Gearman#getDefaultPort()
	 */
	public abstract GearmanServer startGearmanServer() throws IOException;
	
	/**
	 * Starts a new local gearman job server running in the current address space. 
	 * @param port
	 * 		The port number this server will listen on.
	 * @return
	 * 		A new gearman server instance
	 * @throws IOException
	 * 		If an IO exception occurs while attempting to open the given port
	 */
	public abstract GearmanServer startGearmanServer(int port) throws IOException;
	
	/**
	 * Starts a new local gearman job server running in the current address space. 
	 * @param port
	 * 		The port numbers this server should listen on.
	 * @param persistence
	 * 		An application hook used to tell the server how to persist jobs
	 * @return
	 * 		A new gearman server instance
	 * @throws IOException
	 * 		If an IO exception occurs while attempting to open the given port
	 */
	public abstract GearmanServer startGearmanServer(int port, GearmanPersistence persistence) throws IOException;
	
	/**
	 * Creates an object representing a remote gearman job server
	 * @param host
	 * 		The address of the remote job server
	 * @param port
	 * 		The port number the job server is listening on
	 * @return
	 * 		An object representing a remote gearman job server
	 */
	public abstract GearmanServer createGearmanServer(String host, int port);
	
	/**
	 * Creates a new gearman worker
	 * @return
	 * 		A new gearman worker
	 */
	public abstract GearmanWorker createGearmanWorker();
	
	/**
	 * Creates a new {@link GearmanClient}
	 * @return
	 * 		a new gearman client
	 */
	public abstract GearmanClient createGearmanClient();
}
