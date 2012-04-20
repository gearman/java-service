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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventCallback;
import org.gearman.GearmanJoin;

public class GearmanJobEventCallbackCaller<A> implements BackendJobReturn, GearmanJoin<A> ,Runnable {

	/** <code>true</code> if the eof method has been called */
	private boolean isEOF = false;
	
	/** <code>true</code> if the eof method has been called and all events have been processed */
	private boolean isDone = false;
	
	private final Queue<GearmanJobEvent> eventQueue = new LinkedList<GearmanJobEvent>();
	private final Executor exe;
	private final A att;
	private final GearmanJobEventCallback<A> callback;
	
	private boolean isRunning = false;
	
	GearmanJobEventCallbackCaller(A att, GearmanJobEventCallback<A> callback,Executor exe) {
		this.att = att;
		this.exe = exe;
		this.callback = callback;
	}
	
	@Override
	public void run() {
		while(true) {
			
			GearmanJobEvent event;
			
			synchronized(this) {
				event = eventQueue.poll();
				if(event!=null) {
					this.isRunning = true;
				} else {
					this.isRunning = false;
					if(this.isEOF) {
						this.isDone = true;
						this.notifyAll();
					}
					return;
				}
			}
			
			try {
				callback.onEvent(att, event);
			} catch(Throwable th) {
				// TODO log issue
			}
		}
	}

	@Override
	public synchronized void put(GearmanJobEvent event) {
		if(this.isEOF) throw new IllegalStateException();
		
		eventQueue.add(event);
		if(!this.isRunning) exe.execute(this);
	}

	@Override
	public synchronized void eof(GearmanJobEvent lastevent) {
		eventQueue.add(lastevent);
		eventQueue.add(GearmanJobEventImmutable.GEARMAN_EOF);
		this.isEOF = true;
		if(!this.isRunning) exe.execute(this);
	}

	@Override
	public A getAttachment() {
		return this.att;
	}

	@Override
	public synchronized void join() throws InterruptedException {
		while(!this.isDone) {
			this.wait();
		}
	}

	@Override
	public synchronized void join(long timeout, TimeUnit unit) throws InterruptedException {
		if(timeout==0) {
			join();
			return;
		}
		
		timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
		while(!this.isDone && timeout>0) {
			long startTime = System.currentTimeMillis();
			
			this.wait(timeout);
			timeout = timeout - (startTime - System.currentTimeMillis());
		}
	}

	@Override
	public boolean isEOF() {
		return isDone;
	}
}
