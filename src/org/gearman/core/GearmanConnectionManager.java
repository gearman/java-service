package org.gearman.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.gearman.core.GearmanConnection.SendCallbackResult;

import nioReactor.core.ExceptionHandler;
import nioReactor.core.Reactor;
import nioReactor.core.Socket;
import nioReactor.core.SocketHandler;

public class GearmanConnectionManager {
	
	public enum ConnectCallbackResult implements GearmanCallbackResult {
		CONNECTION_FAILED;
		
		@Override
		public boolean isSuccessful() { return false; }
	}
	
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
	
	public final <X> void createGearmanConnection(final InetSocketAddress adrs, final GearmanConnectionHandler<X> handler, GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult> failCallback) {
		this.createGearmanConnection(adrs, handler, new StandardCodec(), failCallback);
	}
	
	public final <X,Y> void createGearmanConnection(final InetSocketAddress adrs, final GearmanConnectionHandler<X> handler, final GearmanCodec<Y> codec, GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult> failCallback) {
		final SocketHandler<SocketHandlerImpl<X,Y>.Connection> sHandler = new SocketHandlerImpl<X,Y>(handler, codec);
		final ConnectWrapper cw = new ConnectWrapper(adrs,failCallback);
		this.reactor.openSocket(adrs, sHandler, cw);
	}
	
	public final void shutdown() {
		this.reactor.shutdown();
	}
	
	public final boolean isShutdown() {
		return this.reactor.isShutdown();
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
				// TODO System.out.println("["+this.getHostAddress()+":"+this.getPort()+"] : IN : "+packet.getPacketType()); //TODO delete line
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

			@Override
			public void sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
				final byte[] data = SocketHandlerImpl.this.codec.encode(packet);
				final CompleteWrapper2 wrapper = new CompleteWrapper2(packet,callback);
				this.socket.write(data,wrapper,wrapper);
			}
		}
	}
	
	/**
	 * TODO Fix the nioReactor project to better support the GearmanCompletionHandler 
	 * @author isaiah
	 *
	 */
	private static final class CompleteWrapper2 implements Runnable, ExceptionHandler<IOException> {

		private final GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback;
		private final GearmanPacket packet;
		
		public CompleteWrapper2(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
			this.packet = packet;
			this.callback = callback;
		}
		
		@Override
		public void run() {
			if(this.callback!=null)
				this.callback.onComplete(packet, SendCallbackResult.SEND_SUCCESSFUL);
		}

		@Override
		public void onException(IOException exception) {
			if(this.callback!=null)
				this.callback.onComplete(packet, SendCallbackResult.SEND_FAILED);
		}
		
	}
	
	/**
	 * TODO Fix the nioReactor project to better support the GearmanCompletionHandler 
	 * @author isaiah
	 */
	private static final class ConnectWrapper implements ExceptionHandler<IOException>{
		GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult> failCallback;
		InetSocketAddress adrs;
		
		public ConnectWrapper(InetSocketAddress adrs, GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult> failCallback) {
			this.adrs = adrs;
			this.failCallback = failCallback;
		}
		@Override
		public void onException(IOException exception) {
			this.failCallback.onComplete(adrs, ConnectCallbackResult.CONNECTION_FAILED);
		}
	}
}
