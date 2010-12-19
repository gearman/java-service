package org.gearman;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.gearman.ServerJob.JobPriority;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanVariables;
import org.gearman.util.ByteArray;
import org.gearman.util.EqualsLock;

class ServerFunction {

	/** The function's name */
	private final ByteArray name;
	/** The lock preventing jobs with the same ID to be created or altered at the same time */
	private final EqualsLock lock = new EqualsLock();
	/** The set of jobs created by this function. ByteArray is equal to the uID */
	private final Map<ByteArray,Job> jobSet = new ConcurrentHashMap<ByteArray,Job>();
	/** The queued jobs waiting to be processed */
	private final ServerJobQueue<Job> queue = new ServerJobQueue<Job>();
	/** The list of workers waiting for jobs to be placed in the queue */
	private final Set<ServerClient> workers = new CopyOnWriteArraySet<ServerClient>();
	/** The maximum number of jobs this function can have at any one time */
	private int maxQueueSize = 0;
	
	public ServerFunction(final ByteArray name) {
		this.name = name;
	}
	public final void addNoopable(final ServerClient noopable) {
		workers.add(noopable);
	}
	public final void removeNoopable(final ServerClient noopable) {
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
		sb.append(this.name.toString(GearmanVariables.UTF_8)); sb.append('\t');
		sb.append(this.jobSet.size()); sb.append('\t');
		sb.append(this.jobSet.size()-this.queue.size());sb.append('\t');
		sb.append(this.workers.size());sb.append('\n');
		
		return GearmanPacket.createTEXT(sb.toString());
	}
	
	public final void createJob(final ByteArray uniqueID, final byte[] data, final JobPriority priority, final ServerClient creator) {
		
		final Integer key = uniqueID.hashCode(); 
		this.lock.lock(key);
		try {
			// Make sure only one thread attempts to add a job with this uID at once
			
			if(this.jobSet.containsKey(uniqueID)) {
				final Job job = this.jobSet.get(uniqueID);
				assert job!=null;
				
				// If creator is specified, add creator to listener set and send JOB_CREATED packet
				if(creator!=null) {
					job.addClient(creator);
					creator.sendPacket(job.createJobCreatedPacket(), null ,null /*TODO*/);
				}
				
				return;
			}
			
			final Job job;
			
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
						creator.sendPacket(ServerStaticPackets.ERROR_QUEUE_FULL,null,null);
						return;
					}
					
					job = new Job(uniqueID, data, priority, creator);
					this.jobSet.put(uniqueID, job);
				}
			} else {
				job = new Job(uniqueID, data, priority, creator);	
				this.jobSet.put(uniqueID, job);		// add job to local job set
			}
			this.queue.add(job);
			
			if(creator!=null) {
				creator.sendPacket(job.createJobCreatedPacket(), null ,null /*TODO*/);
			}
			
			for(ServerClient noop : workers) {
				noop.noop();
			}
		} finally {
			// Always unlock lock
			this.lock.unlock(key);
		}
	}
	
	public final boolean grabJob(final ServerClient worker) {
		final Job job = this.queue.poll();
		if(job==null) return false;
		
		job.work(worker);
		return true;
	}
	
	public final boolean grabJobUniqueID(final ServerClient worker) {
		final Job job = this.queue.poll();
		if(job==null) return false;
		
		job.workUniqueID(worker);
		return true;
	}
	
	private final class Job extends ServerJobAbstract {

		Job(ByteArray uniqueID, byte[] data, JobPriority priority, ServerClient creator) {
			super(uniqueID, data, priority, creator);
		}

		@Override
		protected final synchronized void onComplete(final JobState prevState) {
			assert prevState!=null;
			switch(prevState) {
			case QUEUED:
				// Remove from queue
				final boolean value = ServerFunction.this.queue.remove(this);
				assert value;
			case WORKING:
				final ServerJob job = ServerFunction.this.jobSet.remove(this.getUniqueID());
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
				assert !ServerFunction.this.queue.contains(this);
				final boolean value = ServerFunction.this.queue.add(this);
				assert value;
				break;
			case COMPLETE:
				assert false;
				// should never go from COMPLETE to QUEUED
				break;
			}
		}

		@Override
		public ServerFunction getFunction() {
			return ServerFunction.this;
		}
	}
}
