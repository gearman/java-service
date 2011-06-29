package org.gearman.reactor;

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
		
		if(this.writters.isEmpty())
			this.closeConnection();
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
		try {
			this.socketChannel.close();
		} catch (IOException ioe) {
			//TODO log error
			System.out.println(ioe.getMessage());
		} catch (Throwable th) {
			System.out.println(th.getMessage());
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
