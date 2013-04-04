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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.Queue;

import static org.gearman.context.GearmanContext.LOGGER;

final class SocketImpl<A> implements Socket<A>, CompletionHandler<Integer, Object> {
	
	private final AsynchronousSocketChannel socketChannel;
	private final InetSocketAddress local;
	private final InetSocketAddress remote;
	
	private final SocketHandler<A> handler;
	
	private A att;
	private ByteBuffer buffer;
	
	private final Queue<Writter<?>> writters;
	private boolean isWriting;
	
	private boolean isClosed = false;
	
	SocketImpl(AsynchronousSocketChannel socketChannel, SocketHandler<A> handler) throws IOException {
		this.local = (InetSocketAddress) socketChannel.getLocalAddress();
		this.remote = (InetSocketAddress) socketChannel.getRemoteAddress();
		
		this.socketChannel = socketChannel;
		
		this.handler = handler;
		this.buffer = handler.createSocketBuffer();
		
		writters = new LinkedList<Writter<?>>();
	}
	
	@Override
	public void close() {
		synchronized(this) {
			if(this.isClosed) return;
			this.isClosed = true;
		}
		
		synchronized(this.writters) {
			if(!this.isWriting)
				this.closeConnection();
		}
	}

	@Override
	public A getAttachment() {
		return att;
	}

	@Override
	public ByteBuffer getByteBuffer() {
		return buffer;
	}

	@Override
	public InetAddress getInetAddress() {
		return this.remote.getAddress();
	}

	@Override
	public boolean getKeepAlive() throws IOException {
		return this.socketChannel.getOption(StandardSocketOptions.SO_KEEPALIVE);
	}

	@Override
	public InetAddress getLocalAddress() {
		return this.local.getAddress();
	}

	public final void read() {
		this.socketChannel.read(this.buffer, null, this);
	}
	
	@Override
	public int getLocalPort() {
		return this.local.getPort();
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return this.local;
	}

	@Override
	public int getPort() {
		return this.remote.getPort();
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return this.remote;
	}

	@Override
	public boolean getTcpNoDelay() throws IOException {
		return this.socketChannel.getOption(StandardSocketOptions.TCP_NODELAY);
	}

	@Override
	public boolean isClosed() {
		return this.isClosed;
	}

	@Override
	public void setAttachment(A att) {
		this.att = att;
	}

	@Override
	public void setByteBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public void setKeepAlive(boolean on) throws IOException {
		this.socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, on);
	}

	@Override
	public void setTcpNoDelay(boolean on) throws IOException {
		this.socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, on);
	}

	@Override
	public <A2> void write(ByteBuffer data, A2 att, CompletionHandler<ByteBuffer, A2> callback) {
		synchronized(this.writters) {
			this.writters.add(new Writter<A2>(data, att, callback));
			
			if(this.isWriting) return;
			this.isWriting=true;
		}
		this.writeNext();
	}
	
	private final void writeNext() {
		assert this.isWriting;
		
		final Writter<?> writter;
		synchronized(this.writters) {
			writter = this.writters.poll();;
			
			if(writter==null) {
				this.isWriting = false;
				if(this.isClosed && this.writters.isEmpty())
					this.closeConnection();
				return;
			}	
			this.isWriting=true;
		}
		writter.write();
	}
	
	@Override
	public void completed(Integer result, Object attachment) {
		// result equals the number of bytes read in, or -1 if EOF was reached
		
		// Attachment is null
		
		if(result==-1) {
			// EOF
			
			this.closeConnection();
			return;
		}
		
		this.handler.onRead(result,this);
		this.socketChannel.read(buffer, null, this);
	}
	
	@Override
	public void failed(Throwable exc, Object attachment) {
		// Read throws:
		//  * IllegalArgumentException
		//  * ReadPendingException 
		//  * NotYetConnectedException
		//  * ShutdownChannelGroupException 
		
		// Attachment is null
		
		// An IOException is sometimes thrown when the server suddenly disconnects
		if(exc instanceof IOException) {
			this.writters.clear();
			this.close();
			return;
		}
		
		// None of the thrown exceptions should ever be thrown
		// TODO log error
		assert false;
	}
	
	private final void closeConnection() {
		if(!this.socketChannel.isOpen()) return;
		try {
			this.socketChannel.shutdownOutput();
			this.socketChannel.shutdownInput();
			this.socketChannel.close();
		} catch (IOException ioe) {
			LOGGER.warn("Failed to close connection", ioe);
		} catch (Throwable th) {
			LOGGER.warn("Unexspected Exception", th);
		} finally {
			this.handler.onDisconnect(this);
		}
	}
	
	private final class Writter<A2> implements CompletionHandler<Integer, Object> {
		private final ByteBuffer data;
		private final A2 att;
		private final CompletionHandler<ByteBuffer, A2> callback;
		
		public Writter(ByteBuffer data, A2 att, CompletionHandler<ByteBuffer, A2> callback) {
			this.data = data;
			this.att = att;
			this.callback = callback;
		}
	
		public void write() {
			if(this.data.hasRemaining()) {
				SocketImpl.this.socketChannel.write(this.data, null, this);
			} else {
				SocketImpl.this.writeNext();
				
				try {
					if(this.callback!=null) this.callback.completed(data, att);
				} catch (Throwable th) {
					// user threw exception
					th.printStackTrace();
				}
			}
		}
		
		@Override
		public void completed(Integer result, Object attachment) {
			this.write();
		}

		@Override
		public void failed(Throwable exc, Object attachment) {
			SocketImpl.this.writeNext();
			if(this.callback!=null) this.callback.failed(exc, att);
		}
	}
}
