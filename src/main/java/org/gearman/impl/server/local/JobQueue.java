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
final class JobQueue <X extends Job> {
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
		case LOW_PRIORITY:
			return low.add(job);
		case NORMAL_PRIORITY:
			return mid.add(job);
		case HIGH_PRIORITY:
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
		case LOW_PRIORITY:
			low.addFirst(job);
			return;
		case NORMAL_PRIORITY:
			mid.addFirst(job);
			return;
		case HIGH_PRIORITY:
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
		case LOW_PRIORITY:
			return low.remove(job);
		case NORMAL_PRIORITY:
			return mid.remove(job);
		case HIGH_PRIORITY:
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
		case LOW_PRIORITY:
			return low.contains(job);
		case NORMAL_PRIORITY:
			return mid.contains(job);
		case HIGH_PRIORITY:
			return high.contains(job);
		}
		
		assert false;
		return false;
	}
	
	public final boolean isEmpty() {
		return high.isEmpty() && mid.isEmpty() && low.isEmpty();
	}
}
