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

package org.gearman;

import java.util.concurrent.TimeUnit;

/**
 * The gearman job return is used to receive incoming job data from the job server.
 * @author Isaiah van der Elst
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
