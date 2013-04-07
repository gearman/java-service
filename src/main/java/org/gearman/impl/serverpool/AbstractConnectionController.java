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

package org.gearman.impl.serverpool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.gearman.GearmanJobStatus;
import org.gearman.GearmanLostConnectionGrounds;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnection;
import org.gearman.impl.core.GearmanConnection.SendCallbackResult;
import org.gearman.impl.core.GearmanConnectionHandler;
import org.gearman.impl.core.GearmanConnectionManager.ConnectCallbackResult;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.server.GearmanServerInterface;
import org.gearman.impl.util.ByteArray;
import org.gearman.impl.util.GearmanUtils;
import org.gearman.impl.util.TaskJoin;

import static org.gearman.context.GearmanContext.LOGGER;

public abstract class AbstractConnectionController implements ConnectionController, GearmanConnectionHandler<Object>, GearmanCallbackHandler<GearmanServerInterface , ConnectCallbackResult> {
	
	private final AbstractJobServerPool<?> sc;
	
	/** The key mapping to this object in the connMap */
	private final GearmanServerInterface key;
	private ControllerState state = ControllerState.CLOSED;
	private GearmanConnection<?> conn;
	private ScheduledFuture<?> future;
	private Closer closer;
	
	private AtomicInteger connId = new AtomicInteger(0);
	
	private final SendCallback defaultCallback;
	
	private HashMap<ByteArray, TaskJoin<GearmanJobStatus>> pendingJobStatus; 
	
	private final Object lock = new Object();
	
	public int getConnectionId() {
		return connId.get();
	}
	
	protected AbstractConnectionController(AbstractJobServerPool<?> sc, GearmanServerInterface key) {
		this.key = key;
		this.sc = sc;
		
		this.defaultCallback = new SendCallback(null);
	}
	
	public void onStatusReceived(GearmanPacket packet) {
		final byte[] b_jobHandle	= packet.getArgumentData(0);
		final byte[] b_isKnown		= packet.getArgumentData(1);
		final byte[] b_isRunning	= packet.getArgumentData(2);
		final byte[] b_numerator	= packet.getArgumentData(3);
		final byte[] b_denominator	= packet.getArgumentData(4);
		
		final ByteArray jobHandle	= new ByteArray(b_jobHandle);
		final boolean isKnown		= b_isKnown.length>0 ? (b_isKnown[0]=='1'?true:false):false;
		final boolean isRunning		= b_isRunning.length>0 ? (b_isRunning[0]=='1'? true: false) : false;
			
		long numerator;
		try { numerator = Long.parseLong(new String(b_numerator, GearmanUtils.getCharset())); }
		catch (NumberFormatException e) { numerator = 0;}
		
		long denominator;
		try { denominator = Long.parseLong(new String(b_denominator, GearmanUtils.getCharset()));}
		catch (NumberFormatException e) { denominator = 0;}
		
		this.completeJobStatus(jobHandle, isKnown, isRunning, numerator, denominator);
	}
	
	public final void ping() {
		conn.sendPacket(GearmanPacket.createECHO_REQ("ping".getBytes(GearmanUtils.getCharset())), null /*TODO*/);
	}
	
	@Override
	public ControllerState getControllerState() {
		return this.state;
	}
	
	public final boolean isConnecting(){
		return this.state.equals(ControllerState.CONNECTING);
	}
	
	public final boolean isOpen(){
		return this.state.equals(ControllerState.OPEN);
	}
	
	public final boolean isClosePending() {
		return this.state.equals(ControllerState.CLOSE_PENDING);
	}
	
	public final boolean isClosed() {
		return this.state.equals(ControllerState.CLOSED);
	}
	
	public final boolean isDropped() {
		return this.state.equals(ControllerState.DROPPED);
	}
	
	public final boolean isWaiting() {
		return this.state.equals(ControllerState.WAITING);
	}
	
	@Override
	public final void onAccept(final GearmanConnection<Object> conn) {
		LOGGER.info(GearmanUtils.toString(conn) + " : Connected");
		
		synchronized(this.lock) {
			
			assert this.isConnecting() || this.isClosed() || this.isDropped();
			assert !this.isOpen() && !this.isClosePending() && !this.isWaiting();
			
			final ControllerState oldState = this.state;
			if(this.state.equals(ControllerState.CONNECTING)) {
				
				// Normal execution
				assert this.conn==null;
				
				this.state = ControllerState.OPEN;
				this.conn = conn;
				
				this.connId.incrementAndGet();
				this.onOpen(oldState);
				
				if(!this.sc.getClientID().equals(AbstractJobServerPool.DEFAULT_CLIENT_ID)) {
					this.sendPacket(GearmanPacket.createSET_CLIENT_ID(this.sc.getClientID()), null);
				}
				
				if(this.pendingJobStatus!=null && !this.pendingJobStatus.isEmpty()) {
					
					for(Entry<ByteArray, TaskJoin<GearmanJobStatus>> status : this.pendingJobStatus.entrySet()) {
						final ByteArray jobHandle = status.getKey();
						this.sendPacket(GearmanPacket.createGET_STATUS(jobHandle.getBytes()), new GearmanCallbackHandler<GearmanPacket, SendCallbackResult>() {
							@Override
							public void onComplete(GearmanPacket data, SendCallbackResult result) {
								if(!result.isSuccessful()) {
									AbstractConnectionController.this.completeJobStatus(GearmanJobStatusType.SEND_FAILED, jobHandle, false, false, 0L, 0L);
								}
							}
						});
					}
				}
				
			} else {
				
				// If not in the CONNECTING state when a connection is accepted, then the
				// user has changed the state while in the connection proccess.
				
				// Since all other state assume a closed connection, close the connection
				try {
					conn.close();
				} catch (IOException e) {
					LOGGER.warn("failed To close connection", e);
				}
			}
		}
	}
	
	@Override
	public final void onDisconnect(final GearmanConnection<Object> conn) {
		LOGGER.info(GearmanUtils.toString(conn) + " : Disconnected");
		
		synchronized(this.lock) {
			if(!this.isOpen() && !this.isClosePending()) return;
			
			if(this.pendingJobStatus!=null) {
				for(Entry<ByteArray, TaskJoin<GearmanJobStatus>> entry : this.pendingJobStatus.entrySet()) {
					// Server Disconnected
					entry.getValue().setValue(GearmanJobStatusImpl.NOT_KNOWN);
				}
				
				this.pendingJobStatus.clear();
				this.pendingJobStatus = null;
			}
			// If we disconnect from the OPEN state, then we have unexpectedly disconnected
			this.closeServer();
		}
		this.onLostConnection(sc.getPolicy(), GearmanLostConnectionGrounds.UNEXPECTED_DISCONNECT);
	}
	
	@Override
	public void onComplete(GearmanServerInterface data, ConnectCallbackResult result) {
		// Connection failed callback handler 
		
		if(result.isSuccessful()) return;
		
		synchronized(this.lock) {
			assert this.conn==null;
			
			if(this.pendingJobStatus!=null) {
				for(Entry<ByteArray, TaskJoin<GearmanJobStatus>> entry : this.pendingJobStatus.entrySet()) {
					// Connection Failed
					entry.getValue().setValue(GearmanJobStatusImpl.NOT_KNOWN);
				}
				
				this.pendingJobStatus.clear();
				this.pendingJobStatus = null;
			}
			
			if(this.sc.isShutdown())
				this.dropServer();
			else {
				this.closeServer();
				this.onLostConnection(sc.getPolicy(), GearmanLostConnectionGrounds.FAILED_CONNECTION);
			}
		}
	}
	
	/**
	 * Tell this ConnectionController that it has had a response timeout.
	 * The following will occur:
	 *  1) The connection will be closed
	 *  2) The "onLostConnection" method will be called with a RESPONCE_TIMEOUT
	 */
	protected final void timeout() {
		synchronized(this.lock) {
			if(this.state.equals(ControllerState.OPEN)) {
				LOGGER.warn(GearmanUtils.toString(conn) + " : Server failed to respond");
				this.closeServer();
				this.onLostConnection(sc.getPolicy(), GearmanLostConnectionGrounds.UNEXPECTED_DISCONNECT);
			}
		}
	}
	
	public final GearmanServerInterface getKey() {
		return this.key;
	}
	
	public ControllerState getState() {
		return this.state;
	}
	
	public boolean isConnected() {
		return this.conn!=null;
	}
	
	public boolean sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
		if(this.conn==null || conn.isClosed()) return false;
		
		LOGGER.info(GearmanUtils.toString(conn) + " : OUT : " + packet.getPacketType().toString());
		this.conn.sendPacket(packet, callback==null? this.defaultCallback: new SendCallback(callback));
		return true;
	}
	
	public boolean sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback, int connectionId) {
		if(connId.get()==connectionId) {
			return sendPacket(packet, callback);
		} else {
			return false;
		}
	}
	
	/**
	 * Attempts to connect to a job server iff the current state allows it.<br>
	 * <br>
	 * If in the {@link ControllerState#OPEN} or {@link ControllerState#DROPPED} state, no attempt will be made to connect. If the <code>force</code>
	 * parameter is set to <code>true</code>, then it an attempt if in the {@link ControllerState#WAITING} or {@link ControllerState#CLOSED} state.
	 * Otherwise, an attempt will only be made if in the {@link ControllerState#CLOSED} state.<br>
	 * 
	 * @param force
	 * 		Will attempt to connect even if in the WAITING state
	 * @return
	 * 		<code>true</code> if an attempt is being made. If <code>true</code> the state of
	 * 		this ConnectionController will {@link ControllerState#OPEN} when this method
	 * 		exits. If <code>false</code> the state will be the same as it was before the
	 * 		invocation
	 */
	public final boolean openServer(final boolean force) {
		synchronized(this.lock) {
			
			final ControllerState oldState = this.state; 
			switch(this.state) {
			case CONNECTING:
			case OPEN:
			case DROPPED:
				return false;
			case WAITING:
				if(!force) return false;
			case CLOSED:
				this.state = ControllerState.CONNECTING;
				this.onConnect(oldState);
				return true;
			case CLOSE_PENDING:
				this.state = ControllerState.OPEN;
			default:
				assert false;
				return false;
			}
		}
	}


	/**
	 * Attempts to put the ConnectionController in the {@link ControllerState#WAITING} state.<br>
	 * <br>
	 * The {@link ControllerState#WAITING} is simply a timed state that waits a persiod of time
	 * before moving to the {@link ControllerState#CLOSED}. Moving from the {@link ControllerState#WAITING}
	 * state to the {@link ControllerState#OPEN} is legal but not suggested.<br>
	 * <br>
	 * Connection controllers are typically put into the {@link ControllerState#WAITING} after a
	 * connectivity failure. We wait hoping that after the given period has elapsed, the job
	 * server will be running properly.
	 * 
	 * @param callback
	 * 		A runnable that executes if the waiting period expires before the state changes.
	 * 		If the state changes before the waiting period expires, the callback will not execute.
	 * 		This includes moving from the WAITING state to the WAITING state.
	 * @param timeout
	 * 		The waiting period
	 * @param unit
	 * 		The time unit for the waiting period
	 */
	public final void waitServer(final Runnable callback, long waittime, final TimeUnit unit) {
		final ControllerState oldState;
		synchronized(this.lock) {
			if(this.closer==null) this.closer = new Closer();
			this.closer.setCallback(callback);
			
			oldState = this.state;
			switch(this.state) {
			case DROPPED:
				return;
			case WAITING:
				assert this.future!=null;
				this.future.cancel(true);
				break;
			case CONNECTING:
			case OPEN:
				this.closeServer();
				break;
			case CLOSED:
			case CLOSE_PENDING:
				break;
			default:
				throw new IllegalStateException("unknown controller state:" + this.state);
			}
			
			this.state = ControllerState.WAITING;
			sc.getGearman().getScheduler().schedule(this.closer, waittime, unit);
		}
		this.onWait(oldState);
	}


	/**
	 * Attempts to put the ConnectionController in the {@link ControllerState#WAITING} state.<br>
	 * <br>
	 * The {@link ControllerState#WAITING} is simply a timed state that waits a persiod of time
	 * before moving to the {@link ControllerState#CLOSED}. Moving from the {@link ControllerState#WAITING}
	 * state to the {@link ControllerState#OPEN} is legal but not suggested.<br>
	 * <br>
	 * Connection controllers are typically put into the {@link ControllerState#WAITING} after a
	 * connectivity failure. We wait hoping that after the given period has elapsed, the job
	 * server will be running properly.<br>
	 * <br>
	 * The waiting period is specified the reconnect time, {@link GearmanJobServerPool#getReconnectPeriod(TimeUnit)}. 
	 * 
	 * @param callback
	 * 		A runnable that executes if the waiting period expires without before the state changes.
	 * 		If the state changes before the waiting period expires, the callback will not execute.
	 * 		This includes moving from the WAITING state to the WAITING state.
	 */
	public final void waitServer(final Runnable callback) {
		this.waitServer(callback, sc.getReconnectPeriod(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
	}


	public void closeServer() {
		ControllerState oldState = this.state;
		synchronized(this.lock) {
			oldState = this.state;
			this.state = ControllerState.CLOSED;
			
			switch(oldState) {
			case CLOSED:
			case DROPPED:
				return;
			case CLOSE_PENDING:
			case OPEN:
				assert this.conn!=null;
				if(this.pendingJobStatus!=null && !this.pendingJobStatus.isEmpty()) {
					// Set the state to pending and return. Do not call onClose
					this.state = ControllerState.CLOSE_PENDING;
					return;
				} else {
					try {
						this.conn.close();
					} catch (IOException e) {
						LOGGER.warn("failed to close connection",e);
					}
					this.conn = null;
				}
				break;
			case WAITING:
				if(this.future!=null) {
					this.future.cancel(true);
					this.future = null;
				}
				break;
			case CONNECTING:
				break;
			default:
				assert false;
				return;
			}
		}
		this.onClose(oldState);
	}
	
	public final void dropServer() {
		dropServer(false);
	}
	
	final void dropServer(boolean isOnShutdown) {
		final ControllerState oldState;
		synchronized(this.lock) {
			oldState = this.state;
			if(this.state.equals(ControllerState.DROPPED)) return;
			
			this.state = ControllerState.DROPPED;
			sc.removeServer(this.key, true);
			
			if(this.conn!=null) {
				try {
					conn.close();
				} catch (IOException e) {
					LOGGER.warn("failed to close connection", e);
				}
			}
			
			if(this.pendingJobStatus!=null) {
				for(Entry<ByteArray, TaskJoin<GearmanJobStatus>> entry : this.pendingJobStatus.entrySet()) {
					// Server Dropped
					entry.getValue().setValue(GearmanJobStatusImpl.NOT_KNOWN);
				}
				
				this.pendingJobStatus.clear();
				this.pendingJobStatus = null;
			}
		}
		this.onDrop(oldState);
	}
	
	public final TaskJoin<GearmanJobStatus> getStatus(final ByteArray jobHandle) {
		
		synchronized(this.lock) {
			if(this.isDropped()) {
				return new TaskJoin<>(GearmanJobStatusImpl.NOT_KNOWN);
			}
			
			if(this.pendingJobStatus==null)
				this.pendingJobStatus = new HashMap<ByteArray, TaskJoin<GearmanJobStatus>>();
			
			TaskJoin<GearmanJobStatus> value = this.pendingJobStatus.get(jobHandle);
			
			if(value!=null)
				return value;
			else
				value = new TaskJoin<>();
			
			this.pendingJobStatus.put(jobHandle, value);
			
			if(!(this.isOpen() || this.isClosePending())) {
				this.openServer(true);
			} else {
				assert this.conn!=null && !this.conn.isClosed();
				this.sendPacket(GearmanPacket.createGET_STATUS(jobHandle.getBytes()), new GearmanCallbackHandler<GearmanPacket, SendCallbackResult>() {
					@Override
					public void onComplete(GearmanPacket data, SendCallbackResult result) {
						if(!result.isSuccessful()) {
							AbstractConnectionController.this.completeJobStatus(GearmanJobStatusType.SEND_FAILED, jobHandle, false, false, 0L, 0L);
						}
					}
				});
			}
			
			return value;
		}
	}
	
	
	private final void completeJobStatus(ByteArray jobHandle, boolean isKnown, boolean isRunning, long numerator, long denominator) {
		synchronized(this.lock) {
			final TaskJoin<GearmanJobStatus> taskJoin = this.pendingJobStatus.remove(jobHandle);
			
			if(this.pendingJobStatus.isEmpty() && this.isClosePending()) {
				this.pendingJobStatus = null;
				this.closeServer();
			}
			
			taskJoin.setValue(new GearmanJobStatusImpl(isKnown, isRunning, numerator, denominator));
		}
	}
	
	private final void completeJobStatus(GearmanJobStatusType type, ByteArray jobHandle, boolean isKnown, boolean isRunning, long numerator, long denominator) {
		synchronized(this.lock) {
			final TaskJoin<GearmanJobStatus> taskJoin = this.pendingJobStatus.remove(jobHandle);
			
			if(this.pendingJobStatus.isEmpty() && this.isClosePending()) {
				this.pendingJobStatus = null;
				this.closeServer();
			}
			
			if(!type.equals(GearmanJobStatusType.SUCCESS)) {
				taskJoin.setValue(GearmanJobStatusImpl.NOT_KNOWN);
			} else {
				taskJoin.setValue(new GearmanJobStatusImpl(isKnown, isRunning, numerator, denominator));
			}
		}
	}
	
	public boolean isShutdown() {
		return sc.isShutdown();
	}
	
	private final class Closer implements Runnable {
		private Runnable callback;
		
		@Override
		public void run() {
			synchronized(AbstractConnectionController.this.lock) {
				if(AbstractConnectionController.this.state.equals(ControllerState.WAITING) && !Thread.currentThread().isInterrupted()) {
					AbstractConnectionController.this.state = ControllerState.CLOSED;
					if(this.callback!=null) this.callback.run();
					
					AbstractConnectionController.this.future=null;
				}
			}
		}
		
		public void setCallback(Runnable callback) {
			this.callback = callback;
		}
	}
}
