package org.gearman.core;

import java.io.IOException;

public interface GearmanConnection<X> {
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
	
	/**
	 * Sends a GearmanPacket to the server.  
	 * 
	 * @param packet
	 * 		The packet to be sent
	 * @throws IOException
	 * 		Thrown if any I/O errors occur.
	 */
	public <A> void sendPacket(GearmanPacket packet, A attachment, GearmanCompletionHandler<A> callback);
	
	public int getPort();
	public int getLocalPort();
	public String getHostAddress();
	public boolean isClosed();
	public void close() throws IOException;
}