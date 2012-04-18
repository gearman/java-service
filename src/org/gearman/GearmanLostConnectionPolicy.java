/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

/**
 * Specifies what to do when a job server unexpectedly disconnects or a service cannot connect
 * to a job server.
 * 
 * @author isaiah
 *
 */
public interface GearmanLostConnectionPolicy {
	
	/**
	 * Called when a gearman service fails to connect to a remote job server or is unexpectedly
	 * disconnected.<br>
	 * <br>
	 * If null is returned or some runtime exception is thrown, the default policy will be taken.
	 * The default policy is normally to reconnect after a period of time.
	 * 
	 * @param server
	 * 		The server in question 
	 * @param grounds
	 * 		The grounds for calling this method
	 * @return
	 * 		An {@link GearmanLostConnectionAction} telling the gearman service what actions to take
	 */
	public GearmanLostConnectionAction lostConnection(GearmanServer server, GearmanLostConnectionGrounds grounds);
	
	/**
	 * Called when a gearman service fails to connect to a local job server or is unexpectedly
	 * disconnected.<br>
	 * <br>
	 * Servers running in the local address space only cause connection failures if it's been
	 * shutdown. Reconnecting to a shutdown local job server is not an option. Therefore
	 * they're always removed from the service. This method notifies the user that the server is
	 * being removed from the service.
	 * 
	 * @param server
	 * 		The local gearman server in question
	 */
	public void shutdownServer(GearmanServer server);
}
