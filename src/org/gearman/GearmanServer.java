/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

import java.util.Collection;

/**
 * The gearman job server receives jobs from the client and distributes them to available workers
 * @author isaiah
 */
public interface GearmanServer extends GearmanService {
	
	/**
	 * Tests if this GearmanServer is running within this process.
	 * @return
	 * 		<code>true</code> if this server is running in the local address space 
	 */
	public boolean isLocalServer();
	
	/**
	 * Returns the port numbers the server is listening on
	 * @return
	 * 		The port number the server is listening on
	 */
	public Collection<Integer> getPorts();
	
	/**
	 * Returns the host name for this server instance.
	 * @return
	 * 		The host name for this server instance
	 */
	public String getHostName();
	
	/**
	 * The server ID
	 * @return
	 * 		The server ID
	 */
	public String getServerID();
}
