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

package org.gearman.impl.core;

import java.io.IOException;

public interface GearmanConnection<X> {
	public static enum SendCallbackResult implements GearmanCallbackResult{
		SEND_SUCCESSFUL,
		SEND_FAILED,
		SERVICE_SHUTDOWN;

		@Override
		public boolean isSuccessful() {
			return this.equals(SEND_SUCCESSFUL);
		}
	}
	
	/**
	 * A user may want to attach an object to maintain the state of communication. 
	 * @param obj
	 * 		The object to attach
	 */
	public void setAttachment(X att);
	
	/**
	 * Gets the attached object, if an object has been attached
	 * @return
	 * 		The attached object
	 */
	public X getAttachment();
	
	public void sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket,SendCallbackResult> callback);
	
	public int getPort();
	public int getLocalPort();
	public String getHostAddress();
	public boolean isClosed();
	public void close() throws IOException;
	
	// TODO create an optional response timeout mechanism
	
	/*
	 *  TODO create a ping timeout mechanism
	 *  I'm thinking the connection will ping the server with an ECHO packet by default.
	 *  However, it should be overwritable. The worker may overwrite it to a NOOP packet
	 *  when it's waiting to work.
	 *  
	 *  Also, it needs to be configurable to allow the client to disconnect after a period
	 *  of time
	 */
}