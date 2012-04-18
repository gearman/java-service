/*
 * Copyright (C) 2010 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
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
