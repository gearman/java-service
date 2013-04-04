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

package org.gearman.impl.util;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple lock based on the value of an object instead of the object's instance.
 * 
 * The synchronizing problem in the server is that sometimes it's required to
 * synchronize on a key value for a hash table. However, the key being used will
 * never be the same instance from one thread to another, and synchronizing on the
 * hash table itself will be too slow.  Currently synchronization is done by this
 * lock which locks based on Object value, not the Object's instance.
 * 
 * Synchronization could have been done using primitive Objects, like Integers,
 * but I decided not to because this program is designed to be embedded. That
 * kind of synchronization may interfere with the wrapping program, possibly
 * causing a deadlock that is impossible to find. 
 * 
 * @author isaiah.v
 */
public class EqualsLock {
		
	/** The set of all keys and lock owners */
	private final Map<Object, Thread> keys = new HashMap<Object, Thread>();
	
	/**
	 * Accrues a lock for the given key. If this thread acquires a lock with
	 * key1, any subsequent threads trying to acquire the lock with key2 will
	 * block if key1.equals(key2).  If key1.equals(key2) is not true, the
	 * subsequent thread will acquire the lock for key2 and continue execution.
	 * 
	 * @param key
	 * 		The key 
	 */
	public final void lock(final Object key) {
		boolean isInterrupted = false;
		
		try {
			synchronized(keys){
				
				while(!acquireLock(key, Thread.currentThread())) {
					keys.wait();
				}
			}

		} catch (InterruptedException e) {
			// Ignore the interruption until we've finished
			isInterrupted = Thread.interrupted();
		}
		
		if(isInterrupted) {
			// re-interrupt thread if an interruption occured
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Acquires the lock only if it is free at the time of invocation.
	 * 
	 * Acquires the lock if it is available and returns immediately with the
	 * value true. If the lock is not available then this method will return
	 * immediately with the value false.
	 * 
	 * @param key
	 * 		The key to acquire the lock
	 * @return	
	 * 		true if the lock was acquired, false if the lock was not acquired 
	 */
	public final boolean tryLock(final Object key) {
		synchronized(keys) {
			return acquireLock(key, Thread.currentThread());
		}
	}
	
	/**
	 * Releases the lock of the given key.  The lock is only released if the
	 * calling thread owns the lock for the given key
	 * 
	 * @param key The key
	 */
	public final void unlock(final Object key) {
		synchronized(keys){
			if(keys.get(key)==Thread.currentThread()) {
				keys.remove(key);
				keys.notifyAll();
			}
		}
	}
	
	/**
	 * Adds the (Object,Thread) pair if the key is not already in the key set.
	 * 
	 * @param key	The key to add
	 * @param t		The Thread to be associated with the key
	 * @return
	 * 		true if the Thread t and the Object key is successfully added, or
	 * 		Thread t is already associated with Object key. false if the Object
	 * 		key has already been added but Thread t is not associated with it.
	 */
	private final boolean acquireLock (final Object key, final Thread t) {
		final Thread value = keys.get(key);
		
		if(value == t)
			return true;
		if(value != null)
			return false;
			
		keys.put(key, t);
		return true;
	}
}
