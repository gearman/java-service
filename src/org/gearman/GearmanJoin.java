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
