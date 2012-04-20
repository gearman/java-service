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

package org.gearman.impl.reactor;

import java.nio.ByteBuffer;

/**
 * The event handler for a set of sockets. The event handler allows the user to define
 * what actions to take when socket events are triggered.  
 * 
 * @author isaiah.v
 * @param <X>
 *            Socket Attachment Type
 */
public interface SocketHandler<X> {

	/**
	 * The <i>onAccept</i> event is triggered when a new connection has been established.
	 * This can be due to a server accepting a new connection
	 * or a client being created using the <i>openSocket</i> method in the
	 * {@link Reactor} class.<br>
	 * <br>
	 * This method is guaranteed to run before the new socket triggers any
	 * <i>onRead</i> or <i>onDisconnect</i> events. This will allow the user to
	 * set the state of the socket and the application-layer before any I/O
	 * event occur.
	 * 
	 * @param socket
	 *            The newly created socket
	 */
	public abstract void onAccept(Socket<X> socket);

	/**
	 * The <i>onDisconnect</i> event is triggered when the connection is lost.
	 * 
	 * @param socket
	 *            The socket who has lost the connection
	 */
	public abstract void onDisconnect(Socket<X> socket);

	/**
	 * The <i>onRead</i> event is triggered after an attempt to write information
	 * from the TCP buffer into the socket's buffer has been made.<br>
	 * 
	 * @param socket
	 *            The socket who received data
	 */
	public abstract void onRead(Integer bytes, Socket<X> socket);

	/**
	 * Creates a new {@link ByteBuffer}.<br>
	 * <br>
	 * This will define the byte buffer for newly created sockets. The user
	 * should set the buffer's applicable state variables so it's ready to
	 * read data off the socket
	 * 
	 * @return A new buffer
	 */
	public ByteBuffer createSocketBuffer();
}