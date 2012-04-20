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

import org.gearman.properties.GearmanProperties;
import org.gearman.properties.PropertyName;

/**
 * A <code>Gearman</code> object defines a gearman systems and creates gearman
 * services. These services include {@link GearmanWorker}s,
 * {@link GearmanClient}s, and {@link GearmanServer}s. All services created by
 * the same <code>Gearman</code> object are said to be in the same system, and
 * all services in the same system share system wide thread resources.
 * 
 * @author isaiah.v
 */
public abstract class Gearman implements GearmanService {

	/**
	 * Creates a new {@link Gearman} instance
	 * 
	 * @return A new {@link Gearman} instance
	 */
	public static Gearman createGearman() {

		try {
			final String className = GearmanProperties
					.getProperty(PropertyName.GEARMAN_CLASSNAME);

			final Class<?> implClass = Class.forName(className);
			final Object o = implClass.newInstance();

			return (Gearman) o;
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {

			/*
			 * If an exception occurs, there is something wrong with the class
			 * name or gearman implementation. In both cases the issue is
			 * unrecoverable. An error must be thrown
			 */

			throw new Error("failed to initialize gearman", e);
		}
	}

	/**
	 * Returns the current java-gearman-service version
	 * 
	 * @return The current version
	 */
	public abstract String getVersion();

	/**
	 * Returns the default port number used by this system
	 * 
	 * @return the default port number
	 */
	public abstract int getDefaultPort();

	/**
	 * Starts a new local gearman job server running in the current address
	 * space, using the default port
	 * 
	 * @return A new gearman server instance
	 * @throws IOException
	 *             If an IO exception occurs while attempting to open the
	 *             default port
	 * @see Gearman#getDefaultPort()
	 */
	public abstract GearmanServer startGearmanServer() throws IOException;

	/**
	 * Starts a new local gearman job server running in the current address
	 * space.
	 * 
	 * @param port
	 *            The port number this server will listen on.
	 * @return A new gearman server instance
	 * @throws IOException
	 *             If an IO exception occurs while attempting to open the given
	 *             port
	 */
	public abstract GearmanServer startGearmanServer(int port)
			throws IOException;

	/**
	 * Starts a new local gearman job server running in the current address
	 * space.
	 * 
	 * @param port
	 *            The port numbers this server should listen on.
	 * @param persistence
	 *            An application hook used to tell the server how to persist
	 *            jobs
	 * @return A new gearman server instance
	 * @throws IOException
	 *             If an IO exception occurs while attempting to open the given
	 *             port
	 */
	public abstract GearmanServer startGearmanServer(int port,
			GearmanPersistence persistence) throws IOException;

	/**
	 * Creates an object representing a remote gearman job server
	 * 
	 * @param host
	 *            The address of the remote job server
	 * @param port
	 *            The port number the job server is listening on
	 * @return An object representing a remote gearman job server
	 */
	public abstract GearmanServer createGearmanServer(String host, int port);

	/**
	 * Creates a new gearman worker
	 * 
	 * @return A new gearman worker
	 */
	public abstract GearmanWorker createGearmanWorker();

	/**
	 * Creates a new {@link GearmanClient}
	 * 
	 * @return a new gearman client
	 */
	public abstract GearmanClient createGearmanClient();
}
