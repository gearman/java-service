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
import org.gearman.GearmanPersistence;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.util.ByteArray;
import org.gearman.impl.util.EqualsLock;
import org.gearman.impl.util.GearmanUtils;

class Function {
	
	AtomicLong emptyCount = new AtomicLong(0);

	/** The function's name */
	private final ByteArray name;
	/** The lock preventing jobs with the same ID to be created or altered at the same time */
	private final EqualsLock lock = new EqualsLock();
	/** The set of jobs created by this function. ByteArray is equal to the uID */
	private final Map<ByteArray,InnerJob> jobSet = new ConcurrentHashMap<ByteArray,InnerJob>();
	/** The queued jobs waiting to be processed */
	private final JobQueue<InnerJob> queue = new JobQueue<InnerJob>();
	/** The list of workers waiting for jobs to be placed in the queue */
	private final Set<Client> workers = new CopyOnWriteArraySet<Client>();
	/** The maximum number of jobs this function can have at any one time */
	private int maxQueueSize = 0;
	
	public Function(final ByteArray name) {
		this.name = name;
	}
	public final void addNoopable(final Client noopable) {
		workers.add(noopable);
	}
	public final void removeNoopable(final Client noopable) {
		workers.remove(noopable);
	}
	public final void setMaxQueue(final int size) {
		synchronized(this.jobSet) { this.maxQueueSize = size; }
	}
	
	public final ByteArray getName() {
		return this.name;
	}
	
	public final boolean queueIsEmpty() {
		return this.queue.isEmpty();
	}
	
	public final GearmanPacket getStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.name.toString(GearmanUtils.getCharset())); sb.append('\t');
		sb.append(this.jobSet.size()); sb.append('\t');
		sb.append(this.jobSet.size()-this.queue.size());sb.append('\t');
		sb.append(this.workers.size());sb.append('\n');
		
		return GearmanPacket.createTEXT(sb.toString());
	}
	
	public final void put(ByteArray uniqueID, byte[] data, byte[] jobHandle, GearmanJobPriority priority) {
		final InnerJob job = new InnerJob(uniqueID, data, jobHandle, priority, true, null);
		this.jobSet.put(uniqueID, job);
	}
	
	public final void createJob(ByteArray uniqueID, final byte[] data, final GearmanJobPriority priority, final Client creator, boolean isBackground, GearmanPersistence persistence) {
		
		if(uniqueID.isEmpty()) {
			uniqueID = new ByteArray(("emptyID_"+emptyCount.incrementAndGet()).getBytes(GearmanUtils.getCharset()));
			while(jobSet.containsKey(uniqueID)) {
				uniqueID = new ByteArray(("emptyID_"+emptyCount.incrementAndGet()).getBytes(GearmanUtils.getCharset()));
			}
		}
		
		final Integer key = uniqueID.hashCode(); 
		this.lock.lock(key);
		try {
			
			// Make sure only one thread attempts to add a job with this uID at once
			
			if(this.jobSet.containsKey(uniqueID)) {
				
				final InnerJob job = this.jobSet.get(uniqueID);
				if(job!=null) {
					synchronized(job) {
						// If the job is not background, add creator to listener set and send JOB_CREATED packet
						if(!isBackground) job.addClient(creator);
						creator.sendPacket(job.createJobCreatedPacket(), null /*TODO*/);
					
						return;
					}
				}
			}
			
			final InnerJob job;
			
			/* 
			 * Note: with this maxQueueSize variable not being synchronized, it is
			 * possible for a few threads to slip in and add jobs after the
			 * maxQueueSize variable is set, but I've decided that is it not
			 * worth the cost to guarantee this minute feature, especially since
			 * it's possible to have more then maxQueueSize jobs if the jobs were
			 * added prior to the variable being set.
			 */
			if(this.maxQueueSize>0) {
				synchronized (this.jobSet) {
					if(maxQueueSize>0 && maxQueueSize<=jobSet.size()) {
						creator.sendPacket(StaticPackets.ERROR_QUEUE_FULL,null);
						return;
					}
					
					job = new InnerJob(uniqueID, data, priority, isBackground ,creator);
					this.jobSet.put(uniqueID, job);
				}
			} else {
				job = new InnerJob(uniqueID, data, priority, isBackground, creator);	
				this.jobSet.put(uniqueID, job);		// add job to local job set
			}
			
			try {
				if(isBackground && persistence!=null) {
					persistence.write(new ServerPersistable(job)); 
				}
			} catch(Exception e) {
				// TODO log exception
			}
			
			/* 
			 * The JOB_CREATED packet must sent before the job is added to the queue.
			 * Queuing the job before sending the packet may result in another thread
			 * grabbing, completing and sending a WORK_COMPLETE packet before the
			 * JOB_CREATED is sent 
			 */
			creator.sendPacket(job.createJobCreatedPacket(), null /*TODO*/);
			
			/*
			 * The job must be queued before sending the NOOP packets. Sending the noops
			 * first may result in a worker failing to grab the job 
			 */
			this.queue.add(job);
			
			for(Client noop : workers) {
				noop.noop();
			}
			
		} finally {
			// Always unlock lock
			this.lock.unlock(key);
		}
	}
	
	public final boolean grabJob(final Client worker) {
		
		final InnerJob job = this.queue.poll();
		
		if(job==null) return false;
		
		job.work(worker);
		return true;
	}
	
	public final boolean grabJobUniqueID(final Client worker) {
		final InnerJob job = this.queue.poll();
		if(job==null) return false;
		
		job.workUniqueID(worker);
		return true;
	}
	
	private final class InnerJob extends JobAbstract {

		InnerJob(ByteArray uniqueID, byte[] data, GearmanJobPriority priority, boolean isBackground, Client creator) {
			super(uniqueID, data, priority, isBackground,creator);
		}
		
		InnerJob(ByteArray uniqueID, byte[] data, byte[] jobHandle ,GearmanJobPriority priority, boolean isBackground, Client creator) {
			super(uniqueID, data, jobHandle,priority,isBackground,creator);
		}

		@Override
		protected final synchronized void onComplete(final JobState prevState) {
			assert prevState!=null;
			switch(prevState) {
			case QUEUED:
				// Remove from queue
				final boolean value = Function.this.queue.remove(this);
				assert value;
			case WORKING:
				final Job job = Function.this.jobSet.remove(this.getUniqueID());
				assert job.equals(this);
				// Remove from jobSet
			case COMPLETE:
				// Do nothing
			}
		}

		@Override
		protected final synchronized void onQueue(final JobState prevState) {
			assert prevState!=null;
			switch(prevState) {
			case QUEUED:
				// Do nothing
				break;
			case WORKING:
				// Requeue
				assert !Function.this.queue.contains(this);
				final boolean value = Function.this.queue.add(this);
				assert value;
				break;
			case COMPLETE:
				assert false;
				// should never go from COMPLETE to QUEUED
				break;
			}
		}

		@Override
		public Function getFunction() {
			return Function.this;
		}
	}
}
