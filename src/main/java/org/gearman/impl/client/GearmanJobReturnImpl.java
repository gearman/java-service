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

package org.gearman.impl.client;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobReturn;

public class GearmanJobReturnImpl implements GearmanJobReturn, BackendJobReturn {

	private boolean isEOF = false;
	private final Deque<GearmanJobEvent> eventList = new LinkedList<>();
	
	@Override
	public synchronized GearmanJobEvent poll() throws InterruptedException {
		while(eventList.isEmpty() && !this.isEOF) {
			this.wait();
		}
		
		if(this.isEOF())
			return GearmanJobEventImmutable.GEARMAN_EOF;
		
		return eventList.pollFirst();
	}

	@Override
	public synchronized GearmanJobEvent poll(long timeout, TimeUnit unit) throws InterruptedException {
		if(timeout==0) return poll();
			
		timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
		while(eventList.isEmpty() && !this.isEOF && timeout>0) {
			long startTime = System.currentTimeMillis();
			
			this.wait(timeout);
			timeout = timeout - (System.currentTimeMillis()-startTime);
		}
		
		if(this.isEOF())
			return GearmanJobEventImmutable.GEARMAN_EOF;
		
		return eventList.pollFirst();
	}

	@Override
	public synchronized GearmanJobEvent pollNow() {
		if(this.isEOF())
			return GearmanJobEventImmutable.GEARMAN_EOF;
		
		return this.eventList.pollFirst();
	}
	
	@Override
	public synchronized void put(GearmanJobEvent event) {
		if(this.isEOF)
			throw new IllegalStateException();
		
		this.eventList.addLast(event);
		this.notifyAll();
	}
	
	@Override
	public synchronized void eof(GearmanJobEvent lastevent) {
		if(this.isEOF)
			throw new IllegalStateException();
		
		this.isEOF = true;
		this.eventList.addLast(lastevent);
		this.notifyAll();
	}

	@Override
	public boolean isEOF() {
		return this.isEOF && eventList.isEmpty();
	}

}
