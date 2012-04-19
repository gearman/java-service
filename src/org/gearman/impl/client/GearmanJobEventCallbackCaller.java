package org.gearman.impl.client;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventCallback;

public class GearmanJobEventCallbackCaller<A> implements BackendJobReturn, Runnable {

	private boolean isEOF = false;
	
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
}
