package org.gearman;

import java.util.concurrent.TimeUnit;

/**
 * The gearman job return is used to receive incoming job data from the job server.
 * @author isaiah
 */
public interface GearmanJobReturn {
	
	/**
	 * Polls the next job event, blocking if necessary
	 * @return
	 * 		The next job event
	 * @throws InterruptedException
	 * 		if the thread is interrupted while blocked
	 */
	public GearmanJobEvent poll() throws InterruptedException;
	
	/**
	 * Polls the next job event, blocking up to the given amount of time if necessary
	 * @param timeout
	 * 		The timeout
	 * @param unit
	 * 		The time unit
	 * @return
	 * 		The next job event or <code>null</code> if the given timeout elapses.
	 * @throws InterruptedException
	 * 		if the thread is interrupted while blocked
	 */
	public GearmanJobEvent poll(long timeout, TimeUnit unit) throws InterruptedException;
	
	/**
	 * Polls the next job event. <code>null</code> is returned if no event is available
	 * @return
	 * 		The next job event or <code>null</code> if no event is available.
	 */
	public GearmanJobEvent pollNow();
	
	/**
	 * Tests if the job return has reached the end-of-file. If the end-of-file has been
	 * reached, no more data will be received from this job return
	 * @return
	 * 		<code>true</code> if and only if the end-of-file has been reached
	 */
	public boolean isEOF();
}
