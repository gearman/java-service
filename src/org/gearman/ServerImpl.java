package org.gearman;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.gearman.core.GearmanCodec;
import org.gearman.core.GearmanCompletionHandler;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanConnectionHandler;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanVariables;
import org.gearman.util.ConcurrentHashSet;

class ServerImpl implements GearmanServer, GearmanConnectionHandler<ServerClient>{

	private final Gearman gearman;
	
	private final Set<Integer> openPorts = new ConcurrentHashSet<Integer>();
	private final Set<ServerClient> clients = new ConcurrentHashSet<ServerClient>();
	private final ServerInstructionSet instructSet = new ServerInstructionSet(this);
	
	public ServerImpl(final Gearman gearman) {
		this.gearman = gearman;
	}
	
	public final Set<ServerClient> getClientSet() {
		return Collections.unmodifiableSet(this.clients);
	}
	
	@Override
	public void closeAllPorts() {
		for(Integer i : openPorts) {
			this.closePort(i);
		}
	}

	@Override
	public boolean closePort(int port) {
		return this.gearman.getGearmanConnectionManager().closePort(port);
	}

	@Override
	public <X> void createGearmanConnection(GearmanConnectionHandler<X> handler) {
		new LocalConnection<ServerClient,X>(this,handler);
	}

	@Override
	public Set<Integer> getOpenPorts() {
		return Collections.unmodifiableSet(this.openPorts);
	}

	@Override
	public void openPort() throws IOException {
		this.openPort(GearmanVariables.DEFAULT_PORT);
	}

	@Override
	public void openPort(int port) throws IOException {
		this.gearman.getGearmanConnectionManager().openPort(port, this);
	}

	@Override
	public <X> void openPort(int port, GearmanCodec<X> codec) throws IOException {
		this.gearman.getGearmanConnectionManager().openPort(port, this, codec);
	}

	@Override
	public Gearman getGearman() {
		return this.gearman;
	}

	@Override
	public boolean isShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAccept(GearmanConnection<ServerClient> conn) {
		final ServerClient client = new ServerClientImpl(conn);
		conn.setAttachment(client);
		this.clients.add(client);
	}

	@Override
	public void onDisconnect(GearmanConnection<ServerClient> conn) {
		conn.getAttachment().close();
		
		final boolean b = this.clients.remove(conn.getAttachment());
		assert b;
	}

	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<ServerClient> conn) {
		assert packet!=null;
		assert conn.getAttachment()!=null;
		
		try {
			this.instructSet.execute(packet, conn.getAttachment());
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			assert handler instanceof ServerImpl;
			
			
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
		public <A> void sendPacket(GearmanPacket packet, A attachment, GearmanCompletionHandler<A> callback) {
			if(this.isClosed) {
				if(callback!=null)
					callback.onFail(new IOException("GearmanConnection Closed"), attachment);
				return;
			}
			
			this.peer.handler.onPacketReceived(packet, peer);
			if(callback!=null) callback.onComplete(attachment);
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
	}
}
