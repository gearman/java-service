package org.gearman;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.gearman.GearmanJobStatus.StatusCallbackResult;
import org.gearman.GearmanLostConnectionPolicy.Grounds;
import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanCallbackResult;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanConnectionHandler;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanVariables;
import org.gearman.core.GearmanConnection.SendCallbackResult;
import org.gearman.util.ByteArray;

/**
 * A nasty class used to manage multa
 * 
 * @author isaiah
 */
abstract class JobServerPoolAbstract <X extends JobServerPoolAbstract.ConnectionController<?,?>> implements GearmanJobServerPool {
	private static final String DEFAULT_CLIENT_ID = "-";
	
	public enum ControllerState {
		
		/**
		 * The state specifying the ConnectionController is in the processes of connecting to a server<br>
		 * <br>
		 * You can enter this state from the CLOSED and WAITING state
		 */
		CONNECTING,
		
		/**
		 * The state specifying the ConnectionController is connected to a job server<br>
		 * <br>
		 * You can enter this state from the CONNECTING state.
		 */
		OPEN,
		
		/**
		 * The state specifying the ConnectionController is waiting to close. The controller
		 * will enter this state if the user has specified it wants to close the connection
		 * but there are pending JOB_STATUS 
		 */
		CLOSE_PENDING,
		
		
		/**
		 * The state specifying the ConnectionController is not connected to a job server<br>
		 * <br>
		 * You can enter this state from the CONNECTING, OPEN, CLOSED, and WAITING states. This is the initial state.
		 */
		CLOSED,
		
		/**
		 * The state specifying the ConnectionController is no longer in regular use or controlled by
		 * the ServiceClient
		 * <br>
		 *  You can enter this state from the CONNECTING, OPEN, CLOSED, CLOSE_PENDING ,DROPPED, and WAITING states. This is the final state 
		 */
		DROPPED,
		
		/**
		 * The state specifying the ConnectionController is in a suggested timeout period. It is
		 * suggested that we wait out a period time before attempting to move to the OPEN state.
		 * However, this is only a suggestion. It is legal to move into the OPEN state.   
		 * <br>
		 * You can enter this state from the CONNECTING, OPEN, CLOSED, and WAITING states.
		 */
		WAITING
	};
	
	private static class SendCallback implements GearmanCallbackHandler<GearmanPacket, SendCallbackResult> {
		private final GearmanLogger logger;
		private final GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback;
		
		private SendCallback(GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback, GearmanLogger logger) {
			this.logger = logger;
			this.callback = callback;
		}
		
		@Override
		public void onComplete(GearmanPacket data, SendCallbackResult result) {
			if(!result.isSuccessful()) {
				logger.log(Level.WARNING, "");
			}
			
			if(callback!=null)
				callback.onComplete(data, result);
		}
	}
				
	/**
	 * Controls connections to job servers.
	 * 
	 * @author isaiah
	 *
	 * @param <K>
	 */
	abstract static class ConnectionController<K, C extends GearmanCallbackResult> implements GearmanConnectionHandler<Object>, GearmanCallbackHandler<K,C> {
		private final JobServerPoolAbstract<?> sc;
		
		/** The key mapping to this object in the connMap */
		private final K key;
		private ControllerState state = ControllerState.CLOSED;
		private GearmanConnection<?> conn;
		private ScheduledFuture<?> future;
		private Closer closer;
		
		private final SendCallback defaultCallback;
		
		private HashMap<ByteArray, JobStatus> pendingJobStatus; 
		
		private final Object lock = new Object();
		
		ConnectionController(JobServerPoolAbstract<?> sc, K key, GearmanLogger logger) {
			this.key = key;
			this.sc = sc;
			
			this.defaultCallback = new SendCallback(null, logger);
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
			try { numerator = Long.parseLong(new String(b_numerator, GearmanVariables.UTF_8)); }
			catch (NumberFormatException e) { numerator = 0;}
			
			long denominator;
			try { denominator = Long.parseLong(new String(b_denominator, GearmanVariables.UTF_8));}
			catch (NumberFormatException e) { denominator = 0;}
			
			this.completeJobStatus(StatusCallbackResult.SUCCESS, jobHandle, isKnown, isRunning, numerator, denominator);
		}
		
		public final GearmanLogger getGearmanLogger() {
			return this.defaultCallback.logger;
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
			this.defaultCallback.logger.log(GearmanLogger.toString(conn) + " : Connected");
			
			synchronized(this.lock) {
				
				assert this.isConnecting() || this.isClosed() || this.isDropped();
				assert !this.isOpen() && !this.isClosePending() && !this.isWaiting();
				
				final ControllerState oldState = this.state;
				if(this.state.equals(ControllerState.CONNECTING)) {
					
					// Normal execution
					assert this.conn==null;
					
					this.state = ControllerState.OPEN;
					this.conn = conn;
					this.onOpen(oldState);
					
					if(!this.sc.id.equals(JobServerPoolAbstract.DEFAULT_CLIENT_ID)) {
						this.sendPacket(GearmanPacket.createSET_CLIENT_ID(this.sc.id), null);
					}
					
					if(this.pendingJobStatus!=null && !this.pendingJobStatus.isEmpty()) {
						
						for(Entry<ByteArray, JobStatus> status : this.pendingJobStatus.entrySet()) {
							final ByteArray jobHandle = status.getKey();
							this.sendPacket(GearmanPacket.createGET_STATUS(jobHandle.getBytes()), new GearmanCallbackHandler<GearmanPacket, SendCallbackResult>() {
								@Override
								public void onComplete(GearmanPacket data, SendCallbackResult result) {
									if(!result.isSuccessful()) {
										ConnectionController.this.completeJobStatus(StatusCallbackResult.SEND_FAILED, jobHandle, false, false, 0L, 0L);
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
						this.defaultCallback.logger.log(e);
					}
				}
			}
		}
		
		@Override
		public final void onDisconnect(final GearmanConnection<Object> conn) {
			this.defaultCallback.logger.log(GearmanLogger.toString(conn) + " : Disconnected");
			
			synchronized(this.lock) {
				if(!this.isOpen() && !this.isClosePending()) return;
				
				if(this.pendingJobStatus!=null) {
					for(JobStatus jobStatus : this.pendingJobStatus.values()) {
						jobStatus.complete(StatusCallbackResult.SERVER_DISCONNECTED, false, false, 0, 0);
					}
					
					this.pendingJobStatus.clear();
					this.pendingJobStatus = null;
				}
				
				// If we disconnect from the OPEN state, then we have unexpectedly disconnected
				this.closeServer();
				this.onLostConnection(sc.policy, Grounds.UNEXPECTED_DISCONNECT);
			}
		}
		
		@Override
		public void onComplete(K data, C result) {
			// Connection failed callback handler 
			
			if(result.isSuccessful()) return;
			
			synchronized(this.lock) {
				assert this.conn==null;
				
				if(this.pendingJobStatus!=null) {
					for(JobStatus jobStatus : this.pendingJobStatus.values()) {
						jobStatus.complete(StatusCallbackResult.CONNECTION_FAILED,false,false,0,0);
					}
					
					this.pendingJobStatus.clear();
					this.pendingJobStatus = null;
				}
				
				if(this.sc.isShutdown)
					this.dropServer();
				else {
					this.closeServer();
					this.onLostConnection(sc.policy, Grounds.FAILED_CONNECTION);
				}
			}
		}
		
		/**
		 * Tell this ConnectionController that it has had a response timeout.
		 * The following will occure:
		 *  1) The connection will be closed
		 *  2) The "onLostConnection" method will be called with a RESPONCE_TIMEOUT
		 */
		protected final void timeout() {
			synchronized(this.lock) {
				if(this.state.equals(ControllerState.OPEN)) {
					this.closeServer();
					this.onLostConnection(sc.policy, Grounds.UNEXPECTED_DISCONNECT);
				}
			}
		}
		
		
		
		public final K getKey() {
			return this.key;
		}
		
		public ControllerState getState() {
			return this.state;
		}
		
		public boolean isConnected() {
			return this.conn!=null;
		}
		
		public boolean sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
			if(this.conn==null) return false;
			
			this.defaultCallback.logger.log(GearmanLogger.toString(conn) + " : OUT : " + packet.getPacketType().toString());
			this.conn.sendPacket(packet, callback==null? this.defaultCallback: new SendCallback(callback, this.defaultCallback.logger));
			return true;
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
			synchronized(this.lock) {
				if(this.closer==null) this.closer = new Closer();
				this.closer.setCallback(callback);
				
				final ControllerState oldState = this.state;
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
					break;
				}
				
				this.state = ControllerState.WAITING;
				sc.getGearman().getPool().schedule(this.closer, waittime, unit);
				this.onWait(oldState);
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
			this.waitServer(callback, sc.waitPeriod, TimeUnit.NANOSECONDS);
		}


		public final void closeServer() {
			synchronized(this.lock) {
				ControllerState oldState = this.state;
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
							this.defaultCallback.logger.log(e);
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
				
				this.onClose(oldState);
			}
		}


		public final void dropServer() {
			synchronized(this.lock) {
				final ControllerState oldState = this.state;
				if(this.state.equals(ControllerState.DROPPED)) return;
				
				this.state = ControllerState.DROPPED;
				sc.connMap.remove(key);
				
				if(this.conn!=null) {
					try {
						conn.close();
					} catch (IOException e) {
						this.defaultCallback.logger.log(e);
					}
				}
				
				if(this.pendingJobStatus!=null) {
					for(JobStatus jobStatus : this.pendingJobStatus.values()) {
						jobStatus.complete(StatusCallbackResult.SERVER_DROPPED, false, false, 0, 0);
					}
					
					this.pendingJobStatus.clear();
					this.pendingJobStatus = null;
				}
				
				this.onDrop(oldState);
			}
		}
		
		protected abstract void onConnect(ControllerState oldState);
		protected abstract void onOpen(ControllerState oldState);
		protected abstract void onClose(ControllerState oldState);
		protected abstract void onDrop(ControllerState oldState);
		protected abstract void onWait(ControllerState oldState);
		protected abstract void onNew();
		
		protected abstract void onLostConnection(GearmanLostConnectionPolicy policy, Grounds grounds);
		
		public final void getStatus(final ByteArray jobHandle, final JobStatus jobStatus) {
			
			synchronized(this.lock) {
				if(this.isDropped()) return;
				
				if(this.pendingJobStatus==null)
					this.pendingJobStatus = new HashMap<ByteArray,JobStatus>();
				
				this.pendingJobStatus.put(jobHandle, jobStatus);
				
				if(!(this.isOpen() || this.isClosePending())) {
					this.openServer(true);
				} else {
					assert this.conn!=null && !this.conn.isClosed();
					this.sendPacket(GearmanPacket.createGET_STATUS(jobHandle.getBytes()), new GearmanCallbackHandler<GearmanPacket, SendCallbackResult>() {
						@Override
						public void onComplete(GearmanPacket data, SendCallbackResult result) {
							if(!result.isSuccessful()) {
								ConnectionController.this.completeJobStatus(StatusCallbackResult.SEND_FAILED, jobHandle, false, false, 0L, 0L);
							}
						}
					});
				}
			}
		}
		
		
		private final void completeJobStatus(StatusCallbackResult result ,ByteArray jobHandle, boolean isKnown, boolean isRunning, long numerator, long denominator) {
			synchronized(this.lock) {
				final JobStatus status = this.pendingJobStatus.remove(jobHandle);
				
				if(this.pendingJobStatus.isEmpty() && this.isClosePending()) {
					this.pendingJobStatus = null;
					this.closeServer();
				}
				
				status.complete(result, isKnown, isRunning, numerator, denominator);
			}
		}
		
		private final class Closer implements Runnable {
			private Runnable callback;
			
			@Override
			public void run() {
				synchronized(ConnectionController.this.lock) {
					if(ConnectionController.this.state.equals(ControllerState.WAITING) && !Thread.currentThread().isInterrupted()) {
						ConnectionController.this.state = ControllerState.CLOSED;
						if(this.callback!=null) this.callback.run();
						
						ConnectionController.this.future=null;
					}
				}
			}
			
			public void setCallback(Runnable callback) {
				this.callback = callback;
			}
		}
		
	}
	
	private final ConcurrentHashMap<Object, X> connMap = new ConcurrentHashMap<Object,X>();
	private final GearmanLostConnectionPolicy defaultPolicy;
	private GearmanLostConnectionPolicy policy;;
	private long waitPeriod;
	private boolean isShutdown = false;
	private String id = JobServerPoolAbstract.DEFAULT_CLIENT_ID;
	
	JobServerPoolAbstract(GearmanLostConnectionPolicy defaultPolicy, long waitPeriod, TimeUnit unit) {
		this.defaultPolicy = defaultPolicy;
		this.policy = defaultPolicy;
		this.waitPeriod = unit.toNanos(waitPeriod);
	}
	
	@Override
	public boolean addServer(GearmanServer srvr) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		
		final X x = this.createController(srvr);
		if(this.connMap.putIfAbsent(srvr, x)==null) {
			x.onNew();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean addServer(InetSocketAddress adrs) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		
		final X x = this.createController(adrs);
		if(this.connMap.putIfAbsent(adrs, x)==null) {
			x.onNew();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getClientID() {
		return this.id;
	}

	@Override
	public long getReconnectPeriod(TimeUnit unit) {
		return unit.convert(this.waitPeriod,TimeUnit.NANOSECONDS);
	}

	@Override
	public int getServerCount() {
		return this.connMap.size();
	}

	@Override
	public boolean hasServer(InetSocketAddress address) {
		return this.connMap.containsKey(address);
	}

	@Override
	public boolean hasServer(GearmanServer srvr) {
		return this.connMap.containsKey(srvr);
	}

	@Override
	public void removeAllServers() {
		Iterator<X> it = this.connMap.values().iterator();
		X value;
		
		while(it.hasNext()) {
			value = it.next();
			it.remove();
			
			if(value!=null) {
				value.dropServer();
			}
		}
	}

	@Override
	public boolean removeServer(GearmanServer srvr) {
		final X x = this.connMap.get(srvr);
		
		if(x!=null) {
			x.dropServer();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean removeServer(InetSocketAddress adrs) {
		final X x = this.connMap.get(adrs);
		
		if(x!=null) {
			x.dropServer();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void setClientID(String id) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		if(this.id.equals(id)) return;
		
		for(X x : this.connMap.values()) {
			x.sendPacket(GearmanPacket.createSET_CLIENT_ID(id), null);
		}
	}

	@Override
	public void setLostConnectionPolicy(GearmanLostConnectionPolicy policy) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		
		if(this.policy==null)
			this.policy = this.defaultPolicy;
		else
			this.policy = policy;
	}

	@Override
	public void setReconnectPeriod(long time, TimeUnit unit) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		this.waitPeriod = unit.toNanos(time);
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public synchronized void shutdown() {
		if(this.isShutdown) return;
		this.isShutdown = true;
		
		this.removeAllServers();
	}
	
	protected Map<Object,X> getConnections() {
		return Collections.unmodifiableMap(this.connMap);
	}
	protected GearmanLostConnectionPolicy getDefaultPolicy() {
		return this.defaultPolicy;
	}
	
	/**
	 * Creates a new ConnectionControler to add to the JobServerPool<br>
	 * Note: The returned value is not guaranteed to be added to the set
	 * of connections.  
	 * @param key
	 * 		The ConnectionControler's key
	 * @return
	 * 		
	 */
	protected abstract X createController(GearmanServer key);
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	protected abstract X createController(InetSocketAddress key);
}
