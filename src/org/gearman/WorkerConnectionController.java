package org.gearman;

import java.util.Set;

import org.gearman.JobServerPoolAbstract.ConnectionController;
import org.gearman.JobServerPoolAbstract.ControllerState;
import org.gearman.core.GearmanCompletionHandler;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanConstants;
import org.gearman.core.GearmanPacket.Magic;

abstract class WorkerConnectionController<K> extends ConnectionController<K> {

	private static final int NOOP_TIMEOUT = 59000;
	private static final int GRAB_TIMEOUT = 19000;
	
	/** Specifies if this ConnectionController is in the Dispatcher's queue */
	private boolean isQueued = false;
	
	/**
	 * The time that the last PRE_SLEEP packet was sent. If not sleeping,
	 * this value should be Long.MAX_VALUE
	 */
	private long noopTimeout = Long.MAX_VALUE;
	
	/**
	 * The time that the last GRAB_JOB packet was sent. If this connection
	 * is not waiting for a response to a GRAB_JOB packet, this value should
	 * be Long.MAX_VALUE
	 */
	private long grabTimeout = Long.MAX_VALUE;
	
	WorkerConnectionController(JobServerPoolAbstract<WorkerConnectionController<?>> sc, K key) {
		super(sc, key);
	}
	
	public final void canDo(final Set<String> funcNames) {
		final GearmanConnection<?> conn = super.getConnection();
		if(conn==null) return;
		
		if(!funcNames.isEmpty()) {
			for(String funcName : funcNames) {
				conn.sendPacket(GearmanPacket.createCAN_DO(funcName), null, null);
			}
			this.toDispatcher();
		}
	}
	public final void canDo(final String funcName) {
		final GearmanConnection<?> conn = super.getConnection();
		if(conn==null) return;
		
		conn.sendPacket(GearmanPacket.createCAN_DO(funcName), null, null);
		this.toDispatcher();
	}
	
	public final void cantDo(final String funcName) {
		final GearmanConnection<?> conn = super.getConnection();
		if(conn==null) return;
		
		conn.sendPacket(GearmanPacket.createCANT_DO(funcName), null, null);
	}
	
	private final void error(final GearmanPacket packet) {
		//log error
	}
	
	protected abstract WorkerDispatcher getDispatcher();
	
	protected abstract WorkerImpl getWorker();
	
	/**
	 * Sends a GRAB_JOB packet to the server to request any available jobs
	 * on the queue. The server will respond with either NO_JOB or
	 * JOB_ASSIGN, depending on whether a job is available.
	 * 
	 * This method should only be called by the Dispatcher
	 */
	public final void grabJob() {
		final GearmanConnection<?> conn = super.getConnection();
		if(conn==null) return;

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
	
	private final void jobAssign(final GearmanPacket packet, final GearmanConnection<?> conn) {
		
		this.toDispatcher();
		
		try {
			
			final byte[] jobHandle = packet.getArgumentData(0);
			final String name = new String(packet.getArgumentData(1),GearmanConstants.UTF_8);
			final byte[] jobData = packet.getArgumentData(2);
			
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
			final GearmanJob job = new WorkerJob(name, jobData,this,jobHandle);
			
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
		}
	}
	
	private final void noJob(final GearmanConnection<?> conn) {
		// Since the connection is currently in the sleeping state, it will
		// not yet return to the dispatcher's queue
				
		this.grabTimeout = Long.MAX_VALUE;
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
		this.noopTimeout=Long.MAX_VALUE;
		this.toDispatcher();
	}

	
	@Override
	public void onOpen(ControllerState oldState) {
		final Set<String> funcSet = this.getWorker().getRegisteredFunctions();
		this.canDo(funcSet);
	}
	
	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
		
		switch (packet.getPacketType()) {
		case NOOP:
			noop();
			return;
		case JOB_ASSIGN:
			jobAssign(packet, conn);
			return;
		case JOB_ASSIGN_UNIQ:
			return;
		case NO_JOB:
			noJob(conn);
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
	
	public final void resetAbilities() {
		final GearmanConnection<?> conn = super.getConnection();
		if(conn==null) return;
		
		conn.sendPacket(GearmanPacket.createRESET_ABILITIES(), null, null);
	}
	
	public final void timeoutCheck(long time) {
		
		if(time-this.grabTimeout>GRAB_TIMEOUT) {
			// If the server fails to send back a response to the GRAB_JOB packet,
			// we log the error and close the connection without re-queuing
			
			super.timeout();
		} else if(time-this.noopTimeout>NOOP_TIMEOUT){
			this.noop();
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
}
