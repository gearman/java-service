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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnection;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanConnection.SendCallbackResult;
import org.gearman.impl.util.ByteArray;
import org.gearman.impl.util.GearmanUtils;

import static org.gearman.context.GearmanContext.LOGGER;

class ClientImpl implements Client {
	
	private final GearmanConnection<?> conn;
	
	/** The set of all functions that this worker can perform */
	private final ConcurrentHashMap<ByteArray,Function> funcMap = new ConcurrentHashMap<ByteArray,Function>();
	/** The set of all disconnect listeners */
	private final Set<ClientDisconnectListener> disconnectListeners = new HashSet<ClientDisconnectListener>();
	/** Indicates if the client is to be notified when the next job comes in */ 
	private boolean isSleeping	 = false;
	/** The client id */
	private String clientID	 = "-";
	/** Indicates if exception packets should be forward to clients*/
	private boolean isForwardsExceptions = false;
	/** Indicates if this ServerClient is closed */
	private boolean isClosed = false;
	
	private final SendCallback defaultCallback = new SendCallback(null); 
	
	public ClientImpl(final GearmanConnection<?> conn) {
		this.conn = conn;
	}
	
	@Override
	public boolean addDisconnectListener(ClientDisconnectListener listener) {
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
	public boolean can_do(Function func) {
		assert func!=null;
		
		final boolean value = funcMap.putIfAbsent(func.getName(), func)==null;
		if(value) {
			func.addNoopable(this);
		}
		return value;
	}
	
	@Override
	public boolean cant_do(ByteArray funcName) {
		final Function value = funcMap.remove(funcName);
		value.removeNoopable(this);
		
		return value!=null;
	}
	
	@Override
	public synchronized void close() {
		if(this.isClosed) return;
		this.isClosed = true;
		
		try { this.conn.close(); }
		catch (IOException e) {
			LOGGER.warn("failed to close connection", e);
		}
		
		synchronized(this.disconnectListeners) {
			for(ClientDisconnectListener l: this.disconnectListeners) {
				l.onDisconnect(this);
			}
			this.disconnectListeners.clear();
		}
		for(Function func : this.funcMap.values()) {
			func.removeNoopable(this);
		}
		
		this.funcMap.clear();
	}
	
	@Override
	public String getClientId() {
		return this.clientID;
	}
	
	@Override
	public Iterable<Function> getFunctions() {
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
		
		for(Function f : this.funcMap.values()) {
			sb.append(f.getName().toString(GearmanUtils.getCharset()));
			sb.append(' ');
		}
		
		sb.append('\n');
		
		return GearmanPacket.createTEXT(sb.toString());
	}
	
	@Override
	public void grabJob() {
		for(Function func : this.funcMap.values()) {
			if(func.grabJob(this))
				return;
		}
		this.sendPacket(GearmanPacket.NO_JOB, null);
	}
	
	@Override
	public void grabJobUniq() {
		for(Function func : this.funcMap.values()) {
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
	public boolean removeDisconnectListener(ClientDisconnectListener listener) {
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
		LOGGER.info(GearmanUtils.toString(conn) + " : OUT : " + packet.getPacketType().toString());
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
		
		for(Function func : this.funcMap.values()) {
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
					LOGGER.warn(GearmanUtils.toString(conn) + " : FAILED TO SEND PACKET : " + data.getPacketType().toString());
				}
			}
		}
	}
}
