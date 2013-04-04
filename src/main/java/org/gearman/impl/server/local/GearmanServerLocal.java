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

package org.gearman.impl.server.local;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.gearman.GearmanPersistence;
import org.gearman.impl.GearmanImpl;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnection;
import org.gearman.impl.core.GearmanConnectionHandler;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanConnectionManager.ConnectCallbackResult;
import org.gearman.impl.server.GearmanServerInterface;
import org.gearman.impl.server.ServerShutdownListener;
import org.gearman.impl.util.GearmanUtils;

import static org.gearman.context.GearmanContext.LOGGER;

public class GearmanServerLocal implements GearmanServerInterface, GearmanConnectionHandler<Client> {
	
	private final String id;
	
	private final GearmanImpl gearman;
	private final Interpreter interpreter;
	
	private final Set<Client> clients = Collections.synchronizedSet(new HashSet<Client>());
	private final int openPort;
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	
	private final String hostName;
	
	private boolean isShutdown = false;
	
	private final Set<ServerShutdownListener> listeners = new HashSet<>();
	
	public GearmanServerLocal(GearmanImpl gearman, GearmanPersistence persistence, int port) throws IOException {
		this(gearman, persistence, createID(port), port);
	}
	
	public GearmanServerLocal(GearmanImpl gearman, GearmanPersistence persistence, String serverID, int port) throws IOException {
		this.gearman = gearman;
		this.openPort = port;
		
		String host;
		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			host = "localhost";
		}
		
		this.hostName = host;
		this.interpreter = new Interpreter(this, persistence);
		
		try {
			gearman.getGearmanConnectionManager().openPort(port, this);
		} catch (IOException ioe) {
			LOGGER.error("failed to open port: " + port, ioe);
			throw ioe;
		}
		
		this.id = serverID;
	}
	
	private static final String createID(int openPort) {
		final StringBuilder sb = new StringBuilder("local");
		sb.append(openPort);		
		return sb.toString();
	}
	
	Set<Client> getClientSet() {
		return this.clients;
	}
	
	@Override
	public boolean isLocalServer() {
		return true;
	}

	@Override
	public String getHostName() {
		return hostName;
	}

	@Override
	public void shutdown() {
		try {
			this.lock.writeLock().lock();
			this.isShutdown = true;
		} finally {
			this.lock.writeLock().unlock();
		}
		
		this.gearman.getGearmanConnectionManager().closePort(openPort);
		
		for(Client client : clients) {
			client.close();
		}
		
		for(ServerShutdownListener l : listeners) {
			l.onShutdown(this);
		}
		
		this.getGearman().onServiceShutdown(this);
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public GearmanImpl getGearman() {
		return this.gearman;
	}
	
	@Override
	public String toString() {
		return this.id;
	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof GearmanServerLocal))
			return false;
		return this.toString().equals(o.toString());
	}

	@Override
	public <A> void createGearmanConnection(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServerInterface, ConnectCallbackResult> failCallback) {
		try {
			this.lock.readLock().lock();
			
			if(this.isShutdown()) {
				failCallback.onComplete(this, ConnectCallbackResult.SERVICE_SHUTDOWN);
				return;
			}
			
			new LocalConnection<Client,A>(this,handler);
			
		} finally {
			this.lock.readLock().unlock();
		}
	}
	
	@Override
	public void finalize() throws Throwable {
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
			assert handler instanceof GearmanServerLocal;
			
			
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
		public void sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, org.gearman.impl.core.GearmanConnection.SendCallbackResult> callback) {
			if(this.isClosed) {
				if(callback!=null)
					callback.onComplete(packet, SendCallbackResult.SERVICE_SHUTDOWN);
				return;
			}
			
			this.peer.handler.onPacketReceived(packet, peer);
			if(callback!=null) callback.onComplete(packet, SendCallbackResult.SEND_SUCCESSFUL);
		}
	}

	@Override
	public int getPort() {
		return this.openPort;
	}

	@Override
	public void onAccept(GearmanConnection<Client> conn) {
		try {
			this.lock.readLock().lock();
			if(this.isShutdown()) {
				conn.close();
				return;
			}
			
			LOGGER.info(GearmanUtils.toString(conn) + " : Connected");
			
			final Client client = new ClientImpl(conn);
			conn.setAttachment(client);
				
			this.clients.add(client);
		} catch (IOException e) {
			LOGGER.warn("failed to close connection", e);
		} finally {
			this.lock.readLock().unlock();
		}
	}

	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Client> conn) {
		LOGGER.info(GearmanUtils.toString(conn) + " : IN  : " + packet.getPacketType().toString());
		
		assert packet!=null;
		assert conn.getAttachment()!=null;
		
		try {
			this.interpreter.execute(packet, conn.getAttachment());
		} catch (Exception e) {
			LOGGER.error("failed to execute packet: "+packet.getPacketType().toString(),e);
		}
	}

	@Override
	public void onDisconnect(GearmanConnection<Client> conn) {
		LOGGER.info(GearmanUtils.toString(conn) + " : Disconnected");
		
		Client client = conn.getAttachment();
		conn.setAttachment(null);
		if(client!=null) {
			client.close();
			this.clients.remove(client);
		}
	}

	@Override
	public void addShutdownListener(ServerShutdownListener listener) {
		try {
			this.lock.readLock().lock();
			if(this.isShutdown) throw new IllegalStateException("service is shutdown");
		
			synchronized(this.listeners) {
				this.listeners.add(listener);
			}
		} finally {
			this.lock.readLock().unlock();
		}
	}

	@Override
	public void removeShutdownListener(ServerShutdownListener listener) {
		try {
			this.lock.readLock().lock();
			if(this.isShutdown) throw new IllegalStateException("service is shutdown");
			
			synchronized(this.listeners) {
				this.listeners.remove(listener);
			}
		} finally {
			this.lock.readLock().unlock();
		}
	}
	
}
