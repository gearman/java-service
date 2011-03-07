package org.gearman.dbg;

import java.io.IOException;
import java.util.Set;

import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanCodec;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanConnectionHandler;
import org.gearman.core.GearmanConnectionManager;
import org.gearman.core.GearmanPacket;

/**
 * A mock gearman job server for testing. 
 * 
 * @author isaiah
 */
public class GearmanMockServer implements GearmanConnectionHandler<Object>, GearmanServer {
	
	GearmanConnectionManager man = new GearmanConnectionManager();
	
	public GearmanMockServer() throws IOException {}

	@Override
	public void onAccept(GearmanConnection<Object> conn) {
		System.out.println(this.toString(conn) + " : Connected");
	}

	@Override
	public void onDisconnect(GearmanConnection<Object> conn) {
		System.out.println(this.toString(conn) + " : Disconnected");
	}

	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
		System.out.println(this.toString(conn) + " : IN : "+packet.getPacketType());
	}
	
	private final String toString(GearmanConnection<Object> conn) {
		return "["+conn.getHostAddress() + ":" + conn.getPort() +"]";
	}
	
	
	public final void sendPacket(GearmanPacket packet, GearmanConnection<Object> conn) {
		System.out.println(this.toString(conn) + " : OUT : "+packet.getPacketType());
		conn.sendPacket(packet, null);
	}

	@Override
	public final void closeAllPorts() {
		man.closePorts();
	}

	@Override
	public final boolean closePort(int port) {
		return man.closePort(port);
	}

	@Override
	public final <A> void createGearmanConnection(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServer, ConnectCallbackResult> failHandler) {
		if(this.isShutdown()) {
			failHandler.onComplete(this, ConnectCallbackResult.SERVER_SHUTDOWN);
			return;
		}
		new LocalConnection<Object,A>(this,handler);
	}

	@Override
	public final Set<Integer> getOpenPorts() {
		return man.getOpenPorts();
	}

	@Override
	public final void openPort() throws IOException {
		man.openPort(4730, this);		
	}

	@Override
	public final void openPort(int port) throws IOException {
		man.openPort(port, this);
	}

	@Override
	public final <X> void openPort(int port, GearmanCodec<X> codec) throws IOException {
		man.openPort(port, this, codec);
	}

	@Override
	public final Gearman getGearman() {
		return null;
	}

	@Override
	public final boolean isShutdown() {
		return man.isShutdown();
	}

	@Override
	public final void shutdown() {
		man.shutdown();
	}
	
	@Override
	protected void finalize() throws Throwable{
		super.finalize();
		this.shutdown();
	}
	
	private static final class LocalConnection<X,Y> implements GearmanConnection<X> {
		
		private final LocalConnection<Y,X> peer;
		private final GearmanConnectionHandler<X> handler;
		private X att;
		
		private boolean isClosed = false;
		
		public LocalConnection(GearmanConnectionHandler<X> handler, GearmanConnectionHandler<Y> peerHandler) {
			/*
			 * This method can only be called by the ServerImpl class. This is because the onAccept method
			 * may cause problems if implemented by some other layer
			 */
			assert handler != null;
			assert peerHandler != null;
			assert handler instanceof GearmanMockServer;
			
			
			this.handler = handler;
			
			this.peer = new LocalConnection<Y,X>(peerHandler, this);
			
			this.handler.onAccept(this);
			this.peer.handler.onAccept(peer);
		}
		
		private LocalConnection(GearmanConnectionHandler<X> handler, LocalConnection<Y,X> peer) {
			assert handler != null;
			assert peer != null;
			
			this.handler = handler;
			this.peer = peer;
		}
		
		@Override
		public final void close() throws IOException {
			synchronized(this) {
				if(this.isClosed) return;
				this.isClosed = true;
			}
			
			this.peer.close();
			this.handler.onDisconnect(this);
		}

		@Override
		public final String getHostAddress() {
			return "localhost";
		}

		@Override
		public final int getLocalPort() {
			return -1;
		}

		@Override
		public final int getPort() {
			return -1;
		}

		
		@Override
		public final X getAttachment() {
			return att;
		}

		@Override
		public final void setAttachment(final X att) {
			this.att = att;
		}
		
		@Override
		protected final void finalize() throws Throwable{
			this.close();
		}

		@Override
		public boolean isClosed() {
			return this.isClosed;
		}

		@Override
		public void sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, org.gearman.core.GearmanConnection.SendCallbackResult> callback) {
			// TODO Auto-generated method stub
			if(this.isClosed) {
				if(callback!=null)
					callback.onComplete(packet, SendCallbackResult.SERVICE_SHUTDOWN);
				return;
			}
			
			this.peer.handler.onPacketReceived(packet, peer);
			if(callback!=null) callback.onComplete(packet, SendCallbackResult.SEND_SUCCESSFUL);
		}

	}
}
