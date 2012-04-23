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
	
	public synchronized void setValue(T value) {
		if(value==null)
			throw new IllegalArgumentException();
		this.value = value;
		this.notifyAll();
	}
}
