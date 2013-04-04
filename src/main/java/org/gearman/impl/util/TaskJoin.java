package org.gearman.impl.util;

public class TaskJoin<T> {
	
	private T value;
	
	public TaskJoin() {
		this(null);
	}
	
	public TaskJoin(T value) {
		this.value = value;
	}
	
	public synchronized T getValue() {
		boolean isInterrupted = false;
		while(this.value==null) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				isInterrupted = Thread.interrupted();
			}
		}
		
		if(isInterrupted)
			Thread.currentThread().interrupt();
		
		return value;
	}
	
	public synchronized T getValue(long timeout) {
		if(timeout<0) throw new IllegalArgumentException("negative timeout value");
		if(timeout==0) return getValue();
		
		final long unblockTime = System.currentTimeMillis()+timeout;
		
		boolean isInterrupted = false;
		while(this.value==null) {
			try {
				timeout = unblockTime - System.currentTimeMillis();
				if(timeout>0)
					this.wait(timeout);
				else
					return null;
			} catch (InterruptedException e) {
				isInterrupted = Thread.interrupted();
			}
		}
		
		if(isInterrupted)
			Thread.currentThread().interrupt();
		
		return value;
	}
	
	public synchronized void setValue(T value) {
		if(value==null)
			throw new IllegalArgumentException();
		this.value = value;
		this.notifyAll();
	}
}
