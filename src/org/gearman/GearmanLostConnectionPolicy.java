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
