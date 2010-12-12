package org.gearman;

import java.io.IOException;
import java.util.Set;

import org.gearman.core.GearmanCompletionHandler;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanConnectionHandler;
import org.gearman.core.GearmanFailureHandler;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanVariables;
import org.gearman.core.GearmanPacket.Magic;

abstract class WorkerConnectionController<K,A> implements GearmanConnectionHandler<Object>, GearmanFailureHandler<A> {
	
	private static final int NOOP_TIMEOUT = 60000;
	private static final int GRAB_TIMEOUT = 20000;
	
	/** The key for the connection map. Allows this object to easily remove itself */
	private final K key;
	
	/**
	 * The callback handler from the user. When the connection is accepted or fails, notify them with
	 * this. Make sure this value is set to null after notification. we don't want a memory leak
	 */
	private GearmanCompletionHandler<A> callback;
	
	/**
	 * The attachment for the callback handler. Set to null after the callback handler is call to avoid
	 * memory leak
	 */
	private A att;
	
	/**
	 * The GearmanConnection used to communicate with the server
	 */
	private GearmanConnection<Object> conn;
	
	/** Specifies if this ConnectionController is in the Dispatcher's queue */
	private boolean isQueued = false;
	
	/**
	 * The time that the last PRE_SLEEP packet was sent. If not sleeping,
	 * this value should be zero
	 */
	private long noopTimeout = 0;
	
	/**
	 * The time that the last GRAB_JOB packet was sent. If this connection
	 * is not waiting for a response to a GRAB_JOB packet, this value should
	 * be zero
	 */
	private long grabTimeout = 0;
	
	/**
	 * Creates a new ConnectionController.
	 * @param key
	 * @param att
	 * @param callback
	 */
	public WorkerConnectionController(K key) {
		this.key = key;
	}
	
	public void connect(A att, GearmanCompletionHandler<A> callback) {
		synchronized(this) {
			if(this.att!=null || this.callback!=null) throw new IllegalStateException("Connection in progress");
			this.att = att;
			this.callback = callback;
		}
		
		this.connect();
	}
	
	public final boolean isConnected() {
		return this.conn==null? false: !conn.isClosed();
	}
	
	protected abstract void connect();
	protected abstract void remove();
	
	public K getKey() {
		return this.key;
	}
	
	protected abstract GearmanWorker getWorker();
	protected abstract WorkerDispatcher getDispatcher();
	
	@Override
	public void onAccept(GearmanConnection<Object> conn) {
		this.conn = conn;	
		
		final Set<String> funcSet = this.getWorker().getRegisteredFunctions();
		
		for(String func : funcSet) {
			this.sendPacket(GearmanPacket.createCAN_DO(func), null, null /*TODO*/);
		}
		
		if(this.callback!=null) {
			final A att = this.att;
			this.att = null;
			final GearmanCompletionHandler<A> callback = this.callback;
			this.callback = null;
			callback.onComplete(att);
		}
		
		this.toDispatcher();
	}
	
	@Override
	public void onDisconnect(GearmanConnection<Object> conn) {
		this.onDisconnect();
	}
	protected abstract void onDisconnect();
	
	@Override
	public void onFail(Throwable exc, Object attachment) {
		this.remove();
		
		if(this.callback!=null) {
			final A att = this.att;
			this.att = null;
			final GearmanCompletionHandler<A> callback = this.callback;
			this.callback = null;
			callback.onFail(exc, att);
		}
	}

	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
		System.out.println(packet.getPacketType());
		switch (packet.getPacketType()) {
		case NOOP:
			noop();
			return;
		case JOB_ASSIGN:
			jobAssign(packet);
			return;
		case JOB_ASSIGN_UNIQ:
			return;
		case NO_JOB:
			noJob();
			return;
		case ECHO_RES:
			// not implemented
			return;
		case ERROR:
			error(packet);
			return;
		case OPTION_RES:
			// not implemented
			return;
		default:
			assert false;
			// If default, the packet received is not a worker response
			// packet.
		}
	}
	
	public final void timeoutCheck(long time) {
		if(time-this.grabTimeout>GRAB_TIMEOUT) {
			// If the server fails to send back a response to the GRAB_JOB packet,
			// we log the error and close the connection without re-queuing
			
			// TODO log timeout
			try {
				this.conn.close(); // triggers onDisconnect method
				// TODO remove from the set of servers
			} catch (IOException e) {
				// TODO log error
			}
			
			return;
		} else if(time-this.noopTimeout>NOOP_TIMEOUT){
			this.noop();
		}
	}
	
	/**
	 * Sends a GRAB_JOB packet to the server to request any available jobs
	 * on the queue. The server will respond with either NO_JOB or
	 * JOB_ASSIGN, depending on whether a job is available.
	 * 
	 * This method should only be called by the Dispatcher
	 */
	public final void grabJob() {
		if(this.conn.isClosed()) return;

		// When this method is called, this object is no longer in the
		// Dispatcher's queue
		this.isQueued = false;
		this.grabTimeout = System.currentTimeMillis();
		
		// If the connection is lost, but the sendPacket() method is
		// not throwing an IOException, the response timeout will
		// catch the failure and set things right with the Dispatcher
		this.getWorker().getGearman().getPool().execute(new Runnable() {
			@Override
			public void run() {
				conn.sendPacket(GearmanPacket.createGRAB_JOB(), null ,new GearmanCompletionHandler<Object>() {
					//TODO move this ExceptionHandler so that we don't need to create a new instance every timer
					
					@Override
					public void onComplete(Object attachment) {}
					@Override
					public void onFail(Throwable exc, Object attachment) {
						/*
						 * If we fail, cancel the grab task by calling the
						 * done() method
						 */
						WorkerConnectionController.this.getDispatcher().done();
					}
				});
			}
		});
	}
	
	private final void noJob() {
		// Since the connection is currently in the sleeping state, it will
		// not yet return to the dispatcher's queue
		
		this.grabTimeout = 0;
		this.noopTimeout = System.currentTimeMillis();
		
		this.getDispatcher().done();
		conn.sendPacket(GearmanPacket.createPRE_SLEEP(), null, new GearmanCompletionHandler<Object>(){
			// TODO move task such that we do not need to create a new instance every time
			@Override
			public void onComplete(Object attachment) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void onFail(Throwable exc, Object attachment) {
				exc.printStackTrace();
			};
		});
		
		// Since the connection is currently in the sleeping state, it will
		// not yet return to the dispatcher's queue
	}
	
	/**
	 * Called when a NOOP packet is received
	 */
	private final void noop() {
		// A noop packet has come in. This implies that the connection has
		// moved from the sleeping state to a state that is ready to work.
		this.noopTimeout=0;
		this.toDispatcher();
	}
	
	/**
	 * Called when the 
	 * @param packet
	 */
	private final void jobAssign(final GearmanPacket packet) {
		
		try {
			
			final byte[] jobHandle = packet.getArgumentData(0);
			final String name = new String(packet.getArgumentData(1),GearmanVariables.UTF_8);
			final byte[] jobData = packet.getArgumentData(2);
			
			this.toDispatcher();
			
			// Get function logic
			final GearmanFunction func = this.getWorker().getFunction(name);
			
			if(func==null) {
				// Lookup failed. This can happen if the function is
				// unregistered after the GRAB_JOB packet is sent
				// but before this lookup is performed.

				// send WORK_FAIL
				conn.sendPacket(GearmanPacket.createWORK_FAIL(Magic.REQ, jobHandle), null,null /*TODO*/);
				return;
			}
						
			// Create job for function
			final GearmanJob job = new WorkerJob(name, jobData,conn,jobHandle);
			
			// Run function
			try {
				final GearmanJobResult result = func.work(job);
				job.setResult(result==null? GearmanJobResult.workSuccessful(): result);
			} catch(Exception e) {
				conn.sendPacket(GearmanPacket.createWORK_FAIL(Magic.REQ,jobHandle), null,null /*TODO*/);
			}
			
		} finally {
			// Tell dispatcher of completeness
			this.getDispatcher().done();
			this.toDispatcher();
		}
	}
	
	private final void toDispatcher() {
		/*
		 * Only one copy of each ConnectionController is allowed in the
		 * Dispatcher's queue. This is to enforce that only one thread is
		 * trying to grab a job from a single connection at a time. Having
		 * two GRAB_JOB packets sent in succession before the server can
		 * respond is undefined by the gearman protocol, and thus the
		 * behavior is unpredictable.
		 * 
		 * If the job server decides to send more then one NOOP packet when
		 * a job becomes available, this little mechanism will prevent more
		 * then one ConnectionController from going into the Dispatcher's
		 * queue
		 */
		synchronized (this) {
			if (this.isQueued) return;
			this.isQueued = true;
		}

		// Place the ConnectionController in the Dispatcher's queue
		this.getDispatcher().grab(this);
	}
	
	private final void error(final GearmanPacket packet) {
		//log error
	}
	
	public final <ATT> void sendPacket(GearmanPacket packet, ATT att, GearmanCompletionHandler<ATT> callback) {
		if(this.conn!=null) {
			this.conn.sendPacket(packet, att, callback);
		} else {
			callback.onFail(new IOException("not connected"), att);
		}
	}
}
