/*
 * Copyright (C) 2010 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;


/**
 * A JobQueue queues the different jobs in three different priority levels, low,
 * medium, and high.  Jobs pulled from this queue are pulled from the highest
 * priority first, then medium priority, and low priority last. 
 * 
 * @author isaiah
 *
 */
final class ServerJobQueue <X extends ServerJob> {
	/** Low priority queue */
	private final BlockingDeque<X> low		= new LinkedBlockingDeque<X>();
	/** Medium priority queue */
	private final BlockingDeque<X> mid		= new LinkedBlockingDeque<X>();
	/** High priority queue */
	private final BlockingDeque<X> high		= new LinkedBlockingDeque<X>();
	
	/** 
	 * Adds a job to the back of queue with the corresponding priority
	 * @param job
	 * 		The job to add
	 * @return
	 * 		True if the job was added successful, false otherwise
	 */
	public final boolean add(X job) {
		if(job == null) 
			throw new IllegalArgumentException("Null Value");
		
		switch (job.getPriority()) {
		case LOW:
			return low.add(job);
		case MID:
			return mid.add(job);
		case HIGH:
			return high.add(job);
		}
		
		assert false;
		return false;
	}
	
	/**
	 * Adds a job to the front of queue with the corresponding priority
	 * @param job
	 * 		The job to insert
	 */
	public final void addFirst(X job) {
		if(job == null) 
			throw new IllegalArgumentException("Null Value");
		
		switch (job.getPriority()) {
		case LOW:
			low.addFirst(job);
			return;
		case MID:
			mid.addFirst(job);
			return;
		case HIGH:
			high.addFirst(job);
			return;
		}
		
		assert false;
	}
	
	/**
	 * Polls the next available job
	 * @return
	 * 		The next job if one is available. null is returned if no job is available 
	 */
	public final X poll() {
		X job = high.poll();
		if(job!=null)
			return job;
		
		job = mid.poll();
		if(job!=null)
			return job;
		
		return low.poll();
	}
	
	/**
	 * Returns the total number of queued jobs
	 * @return
	 * 		The total number of queued jobs
	 */
	public final int size() {
		return low.size() + mid.size() + high.size();
	}
	
	/**
	 * Removes a job from the queue
	 * @param job
	 * 		The job to remove
	 * @return
	 * 		true if the job was in the queue and successfully removed,
	 * 		false otherwise 		
	 */
	public final boolean remove(X job) {
		if(job == null) 
			throw new IllegalArgumentException("Null Value");
		
		switch (job.getPriority()) {
		case LOW:
			return low.remove(job);
		case MID:
			return mid.remove(job);
		case HIGH:
			return high.remove(job);
		}
		
		assert false;
		return false;
	}
	
	/**
	 * Test is this queue has the specified job 
	 * @param job
	 * 		The job that we are looking for in the queue
	 * @return
	 * 		true if the job is in the queue, false if not.
	 */
	public final boolean contains(X job) {
		if(job == null) 
			throw new IllegalArgumentException("Null Value");
		
		switch (job.getPriority()) {
		case LOW:
			return low.contains(job);
		case MID:
			return mid.contains(job);
		case HIGH:
			return high.contains(job);
		}
		
		assert false;
		return false;
	}
	
	public final boolean isEmpty() {
		return high.isEmpty() && mid.isEmpty() && low.isEmpty();
	}
}
