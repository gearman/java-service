package org.gearman;

import java.util.concurrent.TimeUnit;

public interface GearmanJoin<A> {
	
	/**
	 * Returns the object attached to the job
	 * @return
	 * 		The attached object
	 */
	public A getAttachment();
	
	/**
	 * Blocks the current thread until the job's eof has been reached and all events
	 * have been processed
	 * @throws InterruptedException
	 * 		if the thread is interrupted while blocked
	 */
	public void join() throws InterruptedException;
	
	/**
	 * Blocks the current thread until the job's eof has been reached and all events
	 * have been processed or until the given timeout elapses
	 * @param timeout
	 * 		The timeout duration
	 * @param unit
	 * 		The timeout unit
	 * @throws InterruptedException
	 * 		if the thread is interrupted while blocked
	 */
	public void join(long timeout, TimeUnit unit) throws InterruptedException;
	
	/**
	 * Returns <code>true</code> if the eof has been reached and all events have been
	 * processed
	 * @return
	 * 		Returns <code>true</code> if the eof has been reached and all events have been
	 * 		processed
	 */
	public boolean isEOF();
}
