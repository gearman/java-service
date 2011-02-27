package org.gearman;

public final class Joinable {
	private boolean isDone =false;
	
	public synchronized final void done() {
		this.isDone = true;
		this.notifyAll();
	}
	
	public synchronized final void join(long timeout) throws InterruptedException {
		if(!this.isDone) {
			this.wait(timeout);
		}
	}
	
	public final boolean isDone() {
		return this.isDone;
	}
}
