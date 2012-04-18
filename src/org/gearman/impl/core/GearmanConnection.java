/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
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
}