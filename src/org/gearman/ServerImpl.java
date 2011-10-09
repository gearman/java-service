package org.gearman;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanCodec;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanConnectionHandler;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanConstants;

class ServerImpl implements GearmanServer, GearmanConnectionHandler<ServerClient>{

	private final Gearman gearman;
	
	private final Set<Integer> openPorts = new HashSet<Integer>();
	private final Set<ServerClient> clients = new HashSet<ServerClient>();
	private final ServerInstructionSet instructSet = new ServerInstructionSet(this);
	
	private boolean isShutdown = false;
	private boolean isGearmanCloseOnShutdown = false;
	
	private final GearmanLogger logger;
	
	public ServerImpl(final Gearman gearman) {
		this.gearman = gearman;
		this.logger = GearmanLogger.createGearmanLogger(gearman, this);
	}
	
	public final Set<ServerClient> getClientSet() {
		return Collections.unmodifiableSet(this.clients);
	}
	
	@Override
	public void closeAllPorts() {
		synchronized(this.openPorts) {
			for(Integer i : openPorts) {
				this.closePort(i);
			}
		}
	}

	@Override
	public boolean closePort(int port) {
		synchronized(this.openPorts) {
			if(!this.openPorts.remove(port)) return false;
		}
		
		return this.gearman.getGearmanConnectionManager().closePort(port);
	}


	@Override
	public <A> void createGearmanConnection(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServer, ConnectCallbackResult> failHandler) {
		if(this.isShutdown()) {
			failHandler.onComplete(this, ConnectCallbackResult.SERVER_SHUTDOWN);
			return;
		}
		new LocalConnection<ServerClient,A>(this,handler);
	}

	@Override
	public Set<Integer> getOpenPorts() {
		return Collections.unmodifiableSet(this.openPorts);
	}

	@Override
	public void openPort() throws IOException {
		this.openPort(GearmanConstants.DEFAULT_PORT);
	}

	@Override
	public void openPort(int port) throws IOException {
		this.gearman.getGearmanConnectionManager().openPort(port, this);
		
		synchronized(this.openPorts) {
			this.openPorts.add(port);
		}
	}

	@Override
	public <X> void openPort(int port, GearmanCodec<X> codec) throws IOException {
		this.gearman.getGearmanConnectionManager().openPort(port, this, codec);
		
		synchronized(this.openPorts) {
			this.openPorts.add(port);
		}
	}

	@Override
	public Gearman getGearman() {
		return this.gearman;
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public void shutdown() {
		synchronized(this) {
			if(this.isShutdown) return;
			this.isShutdown = true;
		}
		
		this.closeAllPorts();
		
		if(this.isGearmanCloseOnShutdown) 
			gearman.shutdown();
		else
			gearman.onServiceShutdown(this);
	}

	@Override
	public void onAccept(GearmanConnection<ServerClient> conn) {
		logger.log(GearmanLogger.toString(conn) + " : Connected");
		
		final ServerClient client = new ServerClientImpl(conn, logger);
		conn.setAttachment(client);
			
		synchronized(this.clients){
			this.clients.add(client);
		}
	}
	
	@Override
	public void onDisconnect(GearmanConnection<ServerClient> conn) {
		logger.log(GearmanLogger.toString(conn) + " : Disconnected");
		
		conn.getAttachment().close();
		
		final boolean b;
		
		synchronized(this.clients) {
			b = this.clients.remove(conn.getAttachment());
			assert b;
		}
	}
	
	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<ServerClient> conn) {
		logger.log(GearmanLogger.toString(conn) + " : IN : " + packet.getPacketType().toString());
		
		assert packet!=null;
		assert conn.getAttachment()!=null;
		
		try {
			this.instructSet.execute(packet, conn.getAttachment());
		} catch (Exception e) {
			logger.log(e);
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
			if(this.isClosed) {
				if(callback!=null)
					callback.onComplete(packet, SendCallbackResult.SERVICE_SHUTDOWN);
				return;
			}
			
			this.peer.handler.onPacketReceived(packet, peer);
			if(callback!=null) callback.onComplete(packet, SendCallbackResult.SEND_SUCCESSFUL);
		}

	}
	
	void closeGearmanOnShutdown(boolean value) {
		this.isGearmanCloseOnShutdown = value;
	}
	
	@SuppressWarnings("unused")
	private final void printClientSize() {
		System.out.println(this.clients.size());
	}

	@Override
	public void setLoggerID(String loggerId) {
		this.logger.setLoggerID(loggerId);
	}

	@Override
	public String getLoggerID() {
		return this.logger.getLoggerID();
	}
}
