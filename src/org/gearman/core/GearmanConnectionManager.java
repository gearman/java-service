package org.gearman.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nioReactor.core.ExceptionHandler;
import nioReactor.core.Reactor;
import nioReactor.core.Socket;
import nioReactor.core.SocketHandler;

public class GearmanConnectionManager {
	
	private final Reactor reactor;
	
	public GearmanConnectionManager() throws IOException {
		this(Executors.newCachedThreadPool());
	}
	
	public GearmanConnectionManager(final ExecutorService executor) throws IOException {
		if(executor==null) throw new IllegalArgumentException("executor is null");
		
		this.reactor = new Reactor(1, executor);
	}
	
	public final <X> void openPort(final int port, final GearmanConnectionHandler<X> handler) throws IOException {
		final SocketHandler<SocketHandlerImpl<X,Integer>.Connection> sHandler = new SocketHandlerImpl<X,Integer>(handler, new StandardCodec());
		this.reactor.openPort(port, sHandler);
	}
	
	public final <X,Y> void openPort(final int port, final GearmanConnectionHandler<X> handler, GearmanCodec<Y> codec) throws IOException {
		final SocketHandler<SocketHandlerImpl<X,Y>.Connection> sHandler = new SocketHandlerImpl<X,Y>(handler, codec);
		this.reactor.openPort(port, sHandler);
	}
	
	public final <X,A> void createGearmanConnection(final InetSocketAddress adrs, final GearmanConnectionHandler<X> handler, final A attachment, final GearmanFailureHandler<A> failHandler) {
		final SocketHandler<SocketHandlerImpl<X,Integer>.Connection> sHandler = new SocketHandlerImpl<X,Integer>(handler, new StandardCodec());
		final FailWrapper<A,IOException> w = new FailWrapper<A,IOException>(attachment,failHandler);
		this.reactor.openSocket(adrs, sHandler , w);
	}
	
	public final <X,Y,A> void createGearmanConnection(final InetSocketAddress adrs, final GearmanConnectionHandler<X> handler, final GearmanCodec<Y> codec, final A attachment, final GearmanFailureHandler<A> failHandler) {
		final SocketHandler<SocketHandlerImpl<X,Y>.Connection> sHandler = new SocketHandlerImpl<X,Y>(handler, codec);
		final FailWrapper<A,IOException> w = new FailWrapper<A,IOException>(attachment,failHandler);
		this.reactor.openSocket(adrs, sHandler, w);
	}
	
	public final void shutdown() {
		this.reactor.shutdown();
	}
	
	public final boolean closePort(final int port) {
		return this.reactor.closePort(port);
	}
	
	public final void closeAllPorts() {
		this.reactor.closeAllPorts();
	}
	
	public final Set<Integer> getOpenPorts() {
		return this.reactor.getOpenPorts();
	}
	
	private static final class SocketHandlerImpl<X,Y> implements SocketHandler<SocketHandlerImpl<X,Y>.Connection> {
	
		private final GearmanConnectionHandler<X> handler;
		private final GearmanCodec<Y> codec;
		
		private SocketHandlerImpl(GearmanConnectionHandler<X> handler, GearmanCodec<Y> codec) {
			if(handler==null || codec==null) {
				throw new IllegalArgumentException("Parameter is null");
			}
			
			this.handler = handler;
			this.codec = codec;
		}

		@Override
		public final ByteBuffer createSocketBuffer() {
			return this.codec.createByteBuffer();
		}

		@Override
		public final void onAccept(final Socket<Connection> socket) {
			try {
				socket.setTcpNoDelay(true);
				socket.setKeepAlive(true);
				socket.setSoLinger(true, 1000);
				
				socket.setAttachment(new Connection(socket));
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public final void onDisconnect(final Socket<Connection> socket) {
			final Connection conn = socket.getAttachment();
			assert conn!=null;
			
			this.handler.onDisconnect(conn);
			conn.codecAtt=null;
			conn.connAtt=null;
		}

		@Override
		public final void onRead(final Socket<Connection> socket) {
			assert socket.getAttachment()!=null;
			this.codec.decode(socket.getAttachment());
		}
		
		private final class Connection implements GearmanConnection<X>, GearmanCodecChannel<Y> {
			
			private final Socket<?> socket;
			
			private X connAtt;
			private Y codecAtt;
			
			private Connection(final Socket<?> socket) {
				this.socket = socket;
				SocketHandlerImpl.this.codec.init(this);
				SocketHandlerImpl.this.handler.onAccept(this);
			}
			
			@Override
			public final X getAttachment() {
				return connAtt;
			}

			@Override
			public final String getHostAddress() {
				return this.socket.getInetAddress().getHostAddress();
			}

			@Override
			public final int getLocalPort() {
				return this.socket.getLocalPort();
			}

			@Override
			public final int getPort() {
				return this.socket.getPort();
			}

			@Override
			public <A> void sendPacket(final GearmanPacket packet, final A attachment, final GearmanCompletionHandler<A> callback) {
				System.out.println("["+this.getHostAddress()+":"+this.getPort()+"] : OUT :"+packet.getPacketType()); //TODO delete line
				
				final byte[] data = SocketHandlerImpl.this.codec.encode(packet);
				
				final CompleteWrapper<A, IOException> w = new  CompleteWrapper<A, IOException>(attachment,callback);
				
				this.socket.write(data,w,w);
			}

			@Override
			public final void setAttachment(final X att) {
				this.connAtt = att;
			}

			@Override
			public final ByteBuffer getBuffer() {
				return this.socket.getByteBuffer();
			}

			@Override
			public final Y getCodecAttachement() {
				return this.codecAtt;
			}

			@Override
			public final void onDecode(final GearmanPacket packet) {
				System.out.println("["+this.getHostAddress()+":"+this.getPort()+"] : IN : "+packet.getPacketType()); //TODO delete line
				SocketHandlerImpl.this.handler.onPacketReceived(packet, this);
			}

			@Override
			public final void setBuffer(final ByteBuffer buffer) {
				this.socket.setByteBuffer(buffer);
			}

			@Override
			public final void setCodecAttachement(final Y att) {
				this.codecAtt = att;
			}

			@Override
			public void close() throws IOException {
				socket.close();
			}

			@Override
			public boolean isClosed() {
				return socket.isClosed();
			}
		}
	}
	
	/**
	 * TODO Fix the nioReactor project to better support the GearmanCompletionHandler 
	 * @author isaiah
	 *
	 * @param <A>
	 * @param <Z>
	 */
	private static final class CompleteWrapper<A,Z extends Exception> implements Runnable, ExceptionHandler<Z>{
		private final A att;
		private final GearmanCompletionHandler<A> handler;
		
		public CompleteWrapper(A att, GearmanCompletionHandler<A> handler) {
			this.att = att;
			this.handler = handler;
		}
		
		@Override
		public void run() {
			if(handler!=null)handler.onComplete(att);
		}

		@Override
		public void onException(Z exception) {
			if(handler!=null)handler.onFail(exception, att);
		}
	}
	
	/**
	 * TODO Fix the nioReactor project to better support the GearmanCompletionHandler 
	 * @author isaiah
	 *
	 * @param <A>
	 * @param <Z>
	 */
	private static final class FailWrapper<A,Z extends Exception> implements ExceptionHandler<Z>{
		private final A att;
		private final GearmanFailureHandler<A> handler;

		public FailWrapper(A att, GearmanFailureHandler<A> handler) {
			this.att = att;
			this.handler = handler;
		}
		
		@Override
		public void onException(Z exception) {
			if(handler!=null)handler.onFail(exception, att);				
		}
	}
}
