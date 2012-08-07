package org.gearman.impl.util;

/**
 * A funky mechonizom that executes a task when there are zero threads in the critcal
 * code block. Where this is used, the critcal code block is not a single block of
 * code. The the thread that calls "lock" may not be the same thread that calls "unlock,"
 * so we need to be abosulty sure there are equal number of "lock" calls as "unlock"
 * calls.
 * 
 * 
 * @author isaiah van der elst
 */
public class ZeroLock {
	private int count = 0;
	private final Runnable task;
	
	public ZeroLock(Runnable task) {
		this.task = task;
	}
	
	public synchronized void lock() {
		count++;
	}
	
	public synchronized void unlock() {
		count--;
		assert count>=0;
		runIfNotLocked();
	}
	
	public synchronized void runIfNotLocked() {
		if(count==0) task.run();
	}
}
