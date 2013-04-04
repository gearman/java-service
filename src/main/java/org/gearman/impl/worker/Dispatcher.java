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

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.gearman.impl.util.GearmanUtils;

/**
 * Manages the number of concurrently executing jobs for a given worker.
 * 
 * All {@link WorkerConnectionController} in a worker will use a dispatcher to manage the
 * number of running jobs. The purpose of the dispatcher is to notify {@link WorkerConnectionController}
 * objects when they can send GRAB_JOB packets. However, the {@link WorkerConnectionController}
 * has the responsibility of notifying its dispatcher when it's done grabbing and working on
 * a job.
 * 
 * @author isaiah
 */
class Dispatcher {
	/**
	 * The number of GRAB_JOB packets that have been dispatched but has yet
	 * finish
	 */
	private int count = 0;
	
	/**
	 * The maximum number of GRAB_JOB packets that may be dispatched at any
	 * one time
	 */
	private int maxCount = GearmanUtils.getWorkerThreads();
	
	/**
	 * The dispatch queue holds all {@link WorkerConnectionController} objects awaiting
	 * the okay to grab a job
	 */
	private final Queue<WorkerConnectionController> dispatch = new LinkedBlockingQueue<WorkerConnectionController>();

	/**
	 * Returns the maximum number of jobs that are allowed to be executed
	 * in parallel
	 * @return
	 * 		The maximum number of jobs that are allowed to be executed
	 * 		in parallel
	 */
	public final int getMaxCount() {
		return maxCount;
	}
	
	/**
	 * Sets the maximum number of jobs that are allowed to be executed
	 * in parallel. The default is 1.
	 * 
	 * @param maxCount
	 * 		The maximum number of jobs that are allowed to be executed
	 * 		in parallel
	 * @throws IllegalArgumentException
	 * 		if <code>maxCount</code> is less then zero
	 */
	public final void setMaxCount(final int maxCount) {
		if(maxCount<0) throw new IllegalArgumentException("maxCount must be 1 or greater");
		
		if(this.maxCount<maxCount) {
			this.maxCount = maxCount;
			this.grabNext();
		} else {
			this.maxCount = maxCount;
		}
	}
	
	/**
	 * Tells the Dispatcher that a ConnectionController has finished with
	 * the GRAB_JOB dispatch.<br>
	 * <br>
	 * A life-span of a GRAB_JOB dispatch is from the point of dispatch
	 * until one of the following events happen:<br>
	 * 1) A NO_JOB packet is packet is received<br>
	 * 2) The job created from a resulting JOB_CREATED packet has completed execution.
	 */
	public synchronized final void done() {
		this.count--;
		this.grabNext();
	}
	
	/**
	 * Removes the given {@link WorkerConnectionController} from the queue
	 * @param cc
	 *            The {@link WorkerConnectionController}
	 */
	public final void drop(final WorkerConnectionController cc) {
		this.dispatch.remove(cc);
	}
	
	/**
	 * Tells the Dispatcher that the given {@link WorkerConnectionController} would like
	 * to grab a job.
	 * 
	 * @param cc
	 *            The given {@link WorkerConnectionController}
	 */
	public final void grab(final WorkerConnectionController cc) {

		// If two or more ConnectionControllers end up in the queue,
		// there is an error in logic
		assert !this.dispatch.contains(cc);

		// Add ConnectionController to queue
		this.dispatch.add(cc);

		// Attempt to dispatch GRAB_JOB packets
		this.grabNext();
	}
	
	/**
	 * Attempts to dispatch GRAB_JOB packets.
	 */
	private synchronized final void grabNext() {

		// The number of available threads should decide the number of GRAB_JOB
		// packets can be dispatched at any one time. This loop enforces that.

		WorkerConnectionController cc;
		for (; count < this.maxCount && !dispatch.isEmpty(); count++) {
			cc = dispatch.remove();
			if (cc != null) 
				cc.grabJob();
		}
	}
}
