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

package org.gearman.impl.worker;

import java.util.Set;

import org.gearman.GearmanFunction;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnection;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanConnection.SendCallbackResult;
import org.gearman.impl.core.GearmanPacket.Magic;
import org.gearman.impl.data.GearmanJob;
import org.gearman.impl.server.GearmanServerInterface;
import org.gearman.impl.serverpool.AbstractConnectionController;
import org.gearman.impl.serverpool.ControllerState;
import org.gearman.impl.serverpool.AbstractJobServerPool;
import org.gearman.impl.util.GearmanUtils;

import static org.gearman.context.GearmanContext.LOGGER;

abstract class WorkerConnectionController extends AbstractConnectionController {
	
	private static final int NOOP_TIMEOUT = 59000;	// TODO decouple property
	private static final int GRAB_TIMEOUT = 19000;	// TODO decouple property
	private static final int PING_TIMEOUT = 59000;	// TODO decouple property
	
	private final ZeroLock zeroLock = new ZeroLock(new Runnable(){
		@Override
		public void run() {
			if(WorkerConnectionController.this.getWorker().getRegisteredFunctions().isEmpty())
				WorkerConnectionController.this.closeServer();
		}
	});
	
	/** Specifies if this ConnectionController is in the Dispatcher's queue */
	private boolean isQueued = false;
	
	/**
	 * The time that the last PRE_SLEEP packet was sent. If not sleeping,
	 * this value should be Long.MAX_VALUE
	 */
	private long noopTimeout = Long.MAX_VALUE;
	
	/**
	 * While working on long executing jobs, the worker needs to ping the server
	 * to keep it alive.
	 */
	private long pingTimeout = Long.MAX_VALUE;
	
	/**
	 * The time that the last GRAB_JOB packet was sent. If this connection
	 * is not waiting for a response to a GRAB_JOB packet, this value should
	 * be Long.MAX_VALUE
	 */
	private long grabTimeout = Long.MAX_VALUE;
	
	public void closeIfNotWorking() {
		zeroLock.runIfNotLocked();
	}
	
	@Override
	public synchronized void onClose(ControllerState oldState) {
		this.isQueued = false;
		this.getDispatcher().drop(this);
	}
	
	WorkerConnectionController(AbstractJobServerPool<WorkerConnectionController> sc, GearmanServerInterface key) {
		super(sc, key);
	}
	
	public final void canDo(final Set<String> funcNames) {		
		if(!funcNames.isEmpty()) {
			for(String funcName : funcNames) {
				super.sendPacket(GearmanPacket.createCAN_DO(funcName), null);
			}
			this.toDispatcher();
		}
	}
	public final void canDo(final String funcName) {
		boolean b = super.sendPacket(GearmanPacket.createCAN_DO(funcName), null);
		
		if(b)
			this.toDispatcher();
	}
	
	public final void cantDo(final String funcName) {
		super.sendPacket(GearmanPacket.createCANT_DO(funcName), null);
	}
	
	private final void error(final GearmanPacket packet) {
		//log error
	}
	
	protected abstract Dispatcher getDispatcher();
	
	protected abstract GearmanWorkerImpl getWorker();
	
	/**
	 * Sends a GRAB_JOB packet to the server to request any available jobs
	 * on the queue. The server will respond with either NO_JOB or
	 * JOB_ASSIGN, depending on whether a job is available.
	 * 
	 * This method should only be called by the Dispatcher
	 */
	public final void grabJob() {
		zeroLock.lock();
		
		if(!super.isConnected()) return;

		// When this method is called, this object is no longer in the
		// Dispatcher's queue
		this.isQueued = false;
		this.grabTimeout = System.currentTimeMillis();
		
		// If the connection is lost, but the sendPacket() method is
		// not throwing an IOException, the response timeout will
		// catch the failure and set things right with the Dispatcher
		this.getWorker().getGearman().getScheduler().execute(new Runnable() {
			@Override
			public void run() {
				final boolean b = WorkerConnectionController.this.sendPacket(GearmanPacket.createGRAB_JOB(), new GearmanCallbackHandler<GearmanPacket, SendCallbackResult>() {
					@Override
					public void onComplete(GearmanPacket data, SendCallbackResult result) {
						if(!result.isSuccessful()) {
							zeroLock.unlock();
							WorkerConnectionController.this.getDispatcher().done();
						}
					}
					
				});
				
				if(!b) {
					zeroLock.unlock();
					WorkerConnectionController.this.getDispatcher().done();
				}
			}
		});
	}
	
	private final void jobAssign(final GearmanPacket packet, final GearmanConnection<?> conn) {
		this.grabTimeout = Long.MAX_VALUE;
		this.toDispatcher();
		
		
		this.getWorker().getGearman().getScheduler().execute(new Runnable() {

			@Override
			public void run() {
				try {
					
					final byte[] jobHandle = packet.getArgumentData(0);
					final String name = new String(packet.getArgumentData(1), GearmanUtils.getCharset());
					final byte[] jobData = packet.getArgumentData(2);
					
					// Get function logic
					final GearmanFunction func = getWorker().getFunction(name);
					
					if(func==null) {
						// Lookup failed. This can happen if the function is
						// unregistered after the GRAB_JOB packet is sent
						// but before this lookup is performed.

						// send WORK_FAIL
						sendPacket(GearmanPacket.createWORK_FAIL(Magic.REQ, jobHandle),null);
						return;
					}
								
					// Create job for function
					//final GearmanJob job = new WorkerJob(name, jobData,this,jobHandle_BA);
					final GearmanJob job = new GearmanJob(name, jobData);
					final GearmanFunctionCallbackImpl callback = new GearmanFunctionCallbackImpl(jobHandle, WorkerConnectionController.this);
					
					// Run function
					try {
						final byte[] result = func.work(job.getFunctionName(), job.getData(), callback);
						callback.success(result==null? new byte[]{} : result);
					} catch(Throwable e) {
						if(LOGGER.isInfoEnabled())
							LOGGER.info("Gearman Job Failed: " + new String(jobHandle) + " : " + e.getMessage());
						callback.fail();
					}
					
				} finally {					
					zeroLock.unlock();
					getDispatcher().done();
				}
			}
			
		});
	}
	
	private final void noJob(final GearmanConnection<?> conn) {
		// Received a response. Set the 
		this.grabTimeout = Long.MAX_VALUE;
		this.noopTimeout = System.currentTimeMillis();
		this.pingTimeout = Long.MAX_VALUE;
		
		this.getDispatcher().done();
		sendPacket(GearmanPacket.createPRE_SLEEP(), null);
		
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
		this.pingTimeout = System.currentTimeMillis();
		this.toDispatcher();
	}

	
	@Override
	public synchronized void onOpen(ControllerState oldState) {
		final Set<String> funcSet = this.getWorker().getRegisteredFunctions();
		this.canDo(funcSet);
	}
	
	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
		LOGGER.info(GearmanUtils.toString(conn)+ " : IN : " + packet.getPacketType());
		
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
		case STATUS_RES:
			super.onStatusReceived(packet);
			break;
		case ECHO_RES:
			// nothing to do
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
		super.sendPacket(GearmanPacket.createRESET_ABILITIES(), null);
	}
	
	public final void timeoutCheck(long time) {
		
		if(time-this.grabTimeout>GRAB_TIMEOUT) {
			// If the server fails to send back a response to the GRAB_JOB packet,
			// we log the error and close the connection without re-queuing
			
			// If a timeout occurs, we need to release the zero lock accrued when
			// the GRAB_JOB packet was sent
			assert zeroLock.isLocked();
			zeroLock.unlock();
			
			// complete job
			this.getDispatcher().done();
			
			// Disconnect
			super.timeout();
			
		} else if(time-this.noopTimeout>NOOP_TIMEOUT) {
			this.noop();
		} else if(time-this.pingTimeout>PING_TIMEOUT) {
			super.ping();
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
