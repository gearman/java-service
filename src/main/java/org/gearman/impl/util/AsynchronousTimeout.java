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

package org.gearman.impl.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit; 

/**
 * A mechanism for handling asynchronous timeouts.
 *  
 * @author isaiah
 */
public class AsynchronousTimeout {
	private final ScheduledExecutorService ses;
	private final Runnable callback;
	private final long timeout;
	
	private final TimeoutTask task = new TimeoutTask();
	
	private boolean isStarted = false;
	private boolean isTaskDeployed = false;
	
	private long timeoutTime;
	
	public AsynchronousTimeout(ScheduledExecutorService ses, Runnable callback, long timeout, TimeUnit unit) {
		if(ses==null)
			throw new NullPointerException("ScheduledExecutorService ses is null");
		if(ses.isShutdown())
			throw new IllegalArgumentException("ScheduledExecutorService ses is shutdown");
		if(callback==null)
			throw new NullPointerException("Runnable callback is null");
		if(timeout<=0)
			throw new IllegalArgumentException("timeout must be positive number greater than zero");
		if(unit==null)
			throw new NullPointerException("TimeUnit unit is null");
		
		this.ses = ses;
		this.callback = callback;
		this.timeout = TimeUnit.NANOSECONDS.convert(timeout, unit);
	}
	
	
	/**
	 * Starts or restarts the timeout period.
	 */
	public synchronized void start() {
		this.isStarted = true;
		
		this.timeoutTime = timeout + System.nanoTime();
		
		if(!this.isTaskDeployed) {
			isTaskDeployed = true;
			ses.schedule(task, this.timeout, TimeUnit.NANOSECONDS);
		}
	}
	
	/**
	 * Stops the timeout period.
	 */
	public synchronized void stop() {
		this.isStarted = false;
	}
	
	/**
	 * Tests if the timeout period has started
	 * @return
	 * 		<code>true</code> if the timeout period is started.
	 */
	public boolean isStarted() {
		return isStarted;
	}
	
	private class TimeoutTask implements Runnable {
		@Override
		public void run() {
			synchronized(AsynchronousTimeout.this) {
				if(!isStarted) {
					isTaskDeployed = false;
					return;
				}
			
				final long timeout = timeoutTime - System.nanoTime();
				if(timeout>0) {
					ses.schedule(this, timeout, TimeUnit.NANOSECONDS);
					return;
				}
					
				isStarted = false;
				isTaskDeployed = false;
			}
			
			callback.run();
		}
	}
}
