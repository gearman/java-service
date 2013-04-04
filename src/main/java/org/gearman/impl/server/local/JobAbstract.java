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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.gearman.GearmanJobPriority;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanConnection.SendCallbackResult;
import org.gearman.impl.core.GearmanPacket.Magic;
import org.gearman.impl.core.GearmanPacket.Type;
import org.gearman.impl.util.ByteArray;
import org.gearman.impl.util.GearmanUtils;

abstract class JobAbstract implements Job, ClientDisconnectListener {
	
	private static final byte[] STATUS_TRUE = new byte[]{'1'};
	private static final byte[] STATUS_FALSE = new byte[]{'0'};
	
	
	/** 
	 * Global Job Map
	 * key = job handle
	 * value = job
	 */
	private static final Map<ByteArray, Job > globalJobs = new ConcurrentHashMap<ByteArray, Job>();
	
	public static final Job getJob(final ByteArray jobHandle) {
		return JobAbstract.globalJobs.get(jobHandle);
	}
	
	/** Defines this job's current state */
	private JobState state = JobState.QUEUED;
	/** Defines this job's priority. Also used as the state change lock */
	private final GearmanJobPriority priority;
	/** Specifies if this is a background or not */
	private final boolean isBackground;
	
	// --- Job Data --- //
	
	/** The function local ID specified by the user */
	private final ByteArray uniqueID;
	/** The server wide ID specified by the server */
	private final ByteArray jobHandle;
	/** The opaque data that is given as an argument in the SUBMIT_JOB packet*/
	private final byte[] data;
	/** The status numerator */
	private byte[] numerator;
	/** The status denominator */
	private byte[] denominator;
	
	//--- Listening Clients and Worker --- //
	
	/** The set of all listing clients */
	private final Set<Client> clients = new CopyOnWriteArraySet<Client>();
	/** The worker assigned to work on this job */
	private Client worker;
	
	JobAbstract(final ByteArray uniqueID, final byte[] data, final GearmanJobPriority priority, boolean isBackground, final Client creator) {
		this(uniqueID, data, getNextJobHandle(), priority, isBackground, creator);
	}
	
	JobAbstract(final ByteArray uniqueID, final byte[] data, final byte[] jobHandle, final GearmanJobPriority priority, boolean isBackground, final Client creator) {
		this.uniqueID = uniqueID;
		this.data = data;
		this.priority = priority;
		
		if(!(this.isBackground = isBackground)) {
			this.clients.add(creator);
			creator.addDisconnectListener(this);
		}
		
		this.jobHandle = new ByteArray(jobHandle);
		
		JobAbstract.globalJobs.put(this.jobHandle, this);
	}
	
	protected final boolean addClient(final Client client) {
		if(this.clients.add(client)) {
			client.addDisconnectListener(this);
			return true;
		}
		
		return false;
	}

	@Override
	public final GearmanPacket createJobAssignPacket() {
		return GearmanPacket.createJOB_ASSIGN(jobHandle.getBytes(), this.getFunction().getName().toString(GearmanUtils.getCharset()), data);
	}

	@Override
	public final GearmanPacket createJobAssignUniqPacket() {
		return new GearmanPacket(Magic.RES, Type.JOB_ASSIGN_UNIQ, this.jobHandle.getBytes(), this.getFunction().getName().getBytes(), this.uniqueID.getBytes(), data);
	}

	@Override
	public final GearmanPacket createJobCreatedPacket() {
		return new GearmanPacket(Magic.RES, Type.JOB_CREATED, this.jobHandle.getBytes());
	}

	@Override
	public final GearmanPacket createWorkStatusPacket() {
		return new GearmanPacket(Magic.RES, Type.WORK_STATUS,this.jobHandle.getBytes(), numerator, denominator);
	}
	
	@Override
	public final GearmanPacket createStatusResPacket() {
		final byte[] isRunning = this.state.equals(JobState.WORKING)? STATUS_TRUE: STATUS_FALSE;
		return new GearmanPacket(Magic.RES, Type.STATUS_RES,this.jobHandle.getBytes(),STATUS_TRUE,isRunning,numerator==null?STATUS_FALSE:numerator, denominator==null?STATUS_FALSE:denominator);
	}


	@Override
	public byte[] getData() {
		return this.data;
	}

	@Override
	public ByteArray getJobHandle() {
		return this.jobHandle;
	}

	@Override
	public GearmanJobPriority getPriority() {
		return this.priority;
	}

	@Override
	public JobState getState() {
		return this.state;
	}

	@Override
	public ByteArray getUniqueID() {
		return this.uniqueID;
	}

	@Override
	public boolean isBackground() {
		return this.isBackground;
	}

	@Override
	public void sendExceptionPacket(GearmanPacket packet) {
		assert packet!=null;
		
		for(Client client : this.clients) {
			client.sendExceptionPacket(packet,null/*TODO*/);
		}
	}

	@Override
	public void sendPacket(GearmanPacket packet) {
		assert packet!=null;
		
		for(Client client : this.clients) {
			client.sendPacket(packet, null/*TODO*/);
		}
	}

	@Override
	public void setStatus(byte[] numerator, byte[] denominator) {
		this.numerator = numerator;
		this.denominator = denominator;
	}

	@Override
	public void workComplete(GearmanPacket packet) {
		assert packet.getPacketType().equals(GearmanPacket.Type.WORK_COMPLETE) || packet.getPacketType().equals(GearmanPacket.Type.WORK_FAIL);
		packet.setMagic(Magic.RES);
		
		final JobState prevState = this.state;
		this.state = JobState.COMPLETE;
		
		this.onComplete(prevState);
		JobAbstract.globalJobs.remove(this.jobHandle);
		
		for(Client client : this.clients) {
			boolean t = client.removeDisconnectListener(this);
			assert t;
			client.sendPacket(packet,null /*TODO*/);
		}
		this.clients.clear();
		
		if(this.worker!=null) {
			boolean t = this.worker.removeDisconnectListener(this);
			assert t;
			this.worker = null;
		}
	}
	
	@Override
	public final void onDisconnect(final Client client) {
		
		switch (this.state) {
		case QUEUED:
			assert this.worker==null;
			this.clients.remove(client);
			// If the job was in the QUEUED state, all attached clients have disconnected, and it is not a background job, drop the job
			if(this.clients.isEmpty() && !this.isBackground)
				complete();
			break;
		case WORKING:
			this.clients.remove(client);
			if(this.worker==client) {
				this.worker = null;
				
				if(this.clients.isEmpty() && !this.isBackground) {
					complete();
				} else {
					// (!this.clients.isEmpty() || this.isBackground)==true
					queue();
				}
			}

			/* 
			 * If the disconnecting client is not the worker, there is no need to change the state.
			 * Since there is no way to notify the worker that the job is no longer valid, we just
			 * let the worker finish execution 
			 */
			
			break;
		case COMPLETE:
			// Do nothing
		}
		if(client==this.worker) {
			this.worker = null;
		}
		this.clients.remove(client);
	}
	
	protected final void work(final Client worker){
		assert this.state==JobState.QUEUED;
		this.worker = worker;
		this.state = JobState.WORKING;
		
		worker.addDisconnectListener(this);
		
		worker.sendPacket(this.createJobAssignPacket(), new GearmanCallbackHandler<GearmanPacket, org.gearman.impl.core.GearmanConnection.SendCallbackResult>(){
			@Override
			public void onComplete(GearmanPacket data, SendCallbackResult result) {
				if(!result.isSuccessful()) 
					JobAbstract.this.queue();
			}			 
		});
	}
	
	protected final void workUniqueID(final Client worker) {
		assert this.state==JobState.QUEUED;
		this.worker = worker;
		this.state = JobState.WORKING;
		
		worker.addDisconnectListener(this);
		
		worker.sendPacket(this.createJobAssignUniqPacket(), new GearmanCallbackHandler<GearmanPacket, org.gearman.impl.core.GearmanConnection.SendCallbackResult>(){
			@Override
			public void onComplete(GearmanPacket data, SendCallbackResult result) {
				if(!result.isSuccessful()) 
					JobAbstract.this.queue();
			}			 
		});
	}
	
	protected abstract void onComplete(JobState prevState);
	
	private final void complete() {
		final JobState prevState = this.state;
		this.state = JobState.COMPLETE;
		
		this.onComplete(prevState);
	}
	private final void queue() {
		final JobState prevState = this.state;
		this.state = JobState.QUEUED;
		
		this.onQueue(prevState);
	}
	
	protected abstract void onQueue(JobState prevState);
	
	/** The prefix for the job handle */
	private static final byte[] jobHandlePrefix = initJobHandle();
	/** The current job handle number */
	private static AtomicLong jobHandleNumber = new AtomicLong(0);
	
	/**
	 * Initializes the jobHandlePrefix variable
	 * @return
	 * 		The prefix for the job handle
	 */
	private static final byte[] initJobHandle() {
		String user;
		try {
			user = java.net.InetAddress.getLocalHost().getHostName();
		} catch (Throwable e) {
			String prefixStr = GearmanUtils.getJobHandlePrefix();
			return (prefixStr + ":gearman:").getBytes(GearmanUtils.getCharset());
		}
		
		String prefixStr = GearmanUtils.getJobHandlePrefix();
		return (prefixStr + ':' + user + ':').getBytes(GearmanUtils.getCharset());
	}
	
	
	/**
	 * Returns the next available job handle
	 * @return
	 * 		the next available job handle
	 */
	private static final byte[] getNextJobHandle() {
		final byte[] jobNumber = Long.toString(jobHandleNumber.incrementAndGet()).getBytes(GearmanUtils.getCharset());
		
		final byte[] jobHandle = new byte[jobHandlePrefix.length+jobNumber.length];
		System.arraycopy(jobHandlePrefix, 0, jobHandle, 0, jobHandlePrefix.length);
		System.arraycopy(jobNumber, 0, jobHandle, jobHandlePrefix.length, jobNumber.length);
		
		return jobHandle;
	}
}
