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

package org.gearman.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanPersistence;
import org.gearman.GearmanServer;
import org.gearman.GearmanService;
import org.gearman.GearmanWorker;
import org.gearman.impl.client.ClientImpl;
import org.gearman.impl.core.GearmanConnectionManager;
import org.gearman.impl.server.local.GearmanServerLocal;
import org.gearman.impl.server.remote.GearmanServerRemote;
import org.gearman.impl.util.Scheduler;
import org.gearman.impl.worker.GearmanWorkerImpl;

/**
 * The implementation of the {@link Gearman} class
 * @author isaiah
 */
public final class GearmanImpl extends Gearman {
	
	private final GearmanConnectionManager connectionManager;
	private final Scheduler scheduler;
	
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Set<GearmanService> serviceSet = Collections.synchronizedSet(new HashSet<GearmanService>());
	
	private boolean isShutdown = false;
	
	public GearmanImpl() throws IOException {
		this(1);
	}
	
	public GearmanImpl(int coreThreads) throws IOException {
		if(coreThreads<=0)
			throw new IllegalArgumentException("GearmanImpl needs 1 or more threads");
		
		final ThreadPoolExecutor pool = new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, GearmanConstants.THREAD_TIMEOUT, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
		pool.allowCoreThreadTimeOut(false);
		pool.prestartCoreThread();
		
		this.scheduler = new Scheduler(pool); 
		this.connectionManager = new GearmanConnectionManager(scheduler);
	}

	@Override
	public void shutdown() {
		try {
			lock.writeLock().lock();
			this.isShutdown = true;
			
			for(GearmanService service : this.serviceSet) {
				service.shutdown();
			}
			this.serviceSet.clear();
			
			// Shutting down the connection manager will shutdown the scheduler
			this.connectionManager.shutdown();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public Gearman getGearman() {
		return this;
	}

	@Override
	public String getVersion() {
		return GearmanConstants.VERSION;
	}

	@Override
	public GearmanServer createGearmanServer(String host, int port) {
		InetSocketAddress address = new InetSocketAddress(host, port);
		
		lock.readLock().lock();
		try {
			if(this.isShutdown()) {
				throw new IllegalStateException("Shutdown Service");
			}
			
			final GearmanServer server = new GearmanServerRemote(this, address);
			this.serviceSet.add(server);
			
			return server;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public GearmanWorker createGearmanWorker() {
		lock.readLock().lock();
		try {
			if(this.isShutdown()) {
				throw new IllegalStateException("Shutdown Service");
			}
			
			final GearmanWorker worker = new GearmanWorkerImpl(this);
			this.serviceSet.add(worker);
			
			return worker;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public GearmanClient createGearmanClient() {
		lock.readLock().lock();
		try {
			if(this.isShutdown()) {
				throw new IllegalStateException("Shutdown Service");
			}
			
			final GearmanClient client = new ClientImpl(this);
			this.serviceSet.add(client);
			
			return client;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public Scheduler getScheduler() {
		return this.scheduler;
	}
	
	public final GearmanConnectionManager getGearmanConnectionManager() {
		return this.connectionManager;
	}

	@Override
	public GearmanServer startGearmanServer(int port) throws IOException {
		return startGearmanServer(port, (GearmanPersistence)null);
	}

	@Override
	public GearmanServer startGearmanServer(int port, GearmanPersistence persistence) throws IOException {
		lock.readLock().lock();
		try {
			if(this.isShutdown()) {
				throw new IllegalStateException("Shutdown Service");
			}
			
			
			final GearmanServer server = new GearmanServerLocal(this, persistence, port);
			this.serviceSet.add(server);
			
			return server;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	public void onServiceShutdown(GearmanService service) {
		lock.readLock().lock();
		try {
			if(this.isShutdown()) return;
			this.serviceSet.remove(service);
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public GearmanServer startGearmanServer() throws IOException {
		return startGearmanServer(getDefaultPort());
	}

	@Override
	public int getDefaultPort() {
		return GearmanConstants.PORT;
	}
}
