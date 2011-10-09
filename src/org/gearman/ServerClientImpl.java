package org.gearman;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanConstants;
import org.gearman.core.GearmanConnection.SendCallbackResult;
import org.gearman.util.ByteArray;

class ServerClientImpl implements ServerClient {

	private final GearmanConnection<?> conn;
	
	private final GearmanLogger logger;
	
	/** The set of all functions that this worker can perform */
	private final ConcurrentHashMap<ByteArray,ServerFunction> funcMap = new ConcurrentHashMap<ByteArray,ServerFunction>();
	/** The set of all disconnect listeners */
	private final Set<ServerClientDisconnectListener> disconnectListeners = new HashSet<ServerClientDisconnectListener>();
	/** Indicates if the client is to be notified when the next job comes in */ 
	private boolean isSleeping	 = false;
	/** The client id */
	private String clientID	 = "-";
	/** Indicates if exception packets should be forward to clients*/
	private boolean isForwardsExceptions = false;
	/** Indicates if this ServerClient is closed */
	private boolean isClosed = false;
	
	private final SendCallback defaultCallback = new SendCallback(null); 
	
	public ServerClientImpl(final GearmanConnection<?> conn, GearmanLogger logger) {
		this.conn = conn;
		this.logger = logger;
	}
	
	@Override
	public boolean addDisconnectListener(ServerClientDisconnectListener listener) {
		assert listener!=null;
		
		synchronized(this) {
			if(this.isClosed) {
				listener.onDisconnect(this);
				return true;
			} else {
				synchronized(this.disconnectListeners) {
					return this.disconnectListeners.add(listener);
				}
			}
		}
	}

	@Override
	public boolean can_do(ServerFunction func) {
		assert func!=null;
		
		final boolean value = funcMap.putIfAbsent(func.getName(), func)==null;
		if(value) {
			func.addNoopable(this);
		}
		return value;
	}
	
	@Override
	public boolean cant_do(ByteArray funcName) {
		final ServerFunction value = funcMap.remove(funcName);
		value.removeNoopable(this);
		
		return value!=null;
	}
	
	@Override
	public synchronized void close() {
		if(this.isClosed) return;
		this.isClosed = true;
		
		try { this.conn.close(); }
		catch (IOException e) {
			logger.log(e);
		}
		
		synchronized(this.disconnectListeners) {
			for(ServerClientDisconnectListener l: this.disconnectListeners) {
				l.onDisconnect(this);
			}
			this.disconnectListeners.clear();
		}
		for(ServerFunction func : this.funcMap.values()) {
			func.removeNoopable(this);
		}
		
		this.funcMap.clear();
	}
	
	@Override
	public String getClientId() {
		return this.clientID;
	}
	
	@Override
	public Iterable<ServerFunction> getFunctions() {
		return this.funcMap.values();
	}
	
	@Override
	public int getLocalPort() {
		return this.conn.getLocalPort();
	}
	
	@Override
	public int getPort() {
		return this.conn.getPort();
	}
	
	@Override
	public GearmanPacket getStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("NA ");
		sb.append(this.conn.getHostAddress());
		sb.append(" ");
		sb.append(this.clientID);
		sb.append(" : ");
		
		for(ServerFunction f : this.funcMap.values()) {
			sb.append(f.getName().toString(GearmanConstants.UTF_8));
			sb.append(' ');
		}
		
		sb.append('\n');
		
		return GearmanPacket.createTEXT(sb.toString());
	}
	
	@Override
	public void grabJob() {
		for(ServerFunction func : this.funcMap.values()) {
			if(func.grabJob(this))
				return;
		}
		this.sendPacket(GearmanPacket.NO_JOB, null);
	}
	
	@Override
	public void grabJobUniq() {
		for(ServerFunction func : this.funcMap.values()) {
			if(func.grabJobUniqueID(this))
				return;
		}
		this.sendPacket(GearmanPacket.NO_JOB, null);
	}

	@Override
	public boolean isClosed() {
		return this.isClosed;
	}
	
	@Override
	public boolean isForwardsExceptions() {
		return this.isForwardsExceptions;
	}
	
	@Override
	public void noop() {
		synchronized(funcMap) {
			if(!isSleeping) return;
			this.isSleeping=false;
			
			this.sendPacket(GearmanPacket.NOOP, null);
		}
	}	
	
	@Override
	public boolean removeDisconnectListener(ServerClientDisconnectListener listener) {
		synchronized(this.disconnectListeners) {
			return this.disconnectListeners.remove(listener);
		}
	}
	
	@Override
	public void reset() {
		// TODO
	}
	
	@Override
	public void sendExceptionPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
		assert packet.getPacketType().equals(GearmanPacket.Type.WORK_EXCEPTION);
		if(this.isForwardsExceptions)
			this.sendPacket(packet, callback);
	}
	
	@Override
	public void sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
		logger.log(GearmanLogger.toString(conn) + " : OUT : " + packet.getPacketType().toString());
		this.conn.sendPacket(packet, callback==null? this.defaultCallback : new SendCallback(callback));
	}
	
	@Override
	public void setClientId(String id) {
		this.clientID = id;
	}
	
	@Override
	public void setForwardsExceptions(boolean value) {
		this.isForwardsExceptions = value;
	}
	
	@Override
	public void sleep() {
		synchronized(funcMap) { this.isSleeping=true; }
		
		for(ServerFunction func : this.funcMap.values()) {
			if(!func.queueIsEmpty()) {
				this.noop();
				return;
			}
		}
	}
	
	@Override
	protected final void finalize() throws Throwable {
		this.close();
	}
	
	private final class SendCallback implements GearmanCallbackHandler<GearmanPacket, SendCallbackResult> {
		private final GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback;
		
		private SendCallback(GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
			this.callback = callback;
		}
		
		@Override
		public void onComplete(GearmanPacket data, SendCallbackResult result) {
			try {
				if(callback!=null)
					callback.onComplete(data, result);
			} finally {
				if(!result.isSuccessful()) {
					logger.log(Level.WARNING, GearmanLogger.toString(conn) + " : FAILED TO SEND PACKET : " + data.getPacketType().toString());
				}
			}
		}
	}
}
