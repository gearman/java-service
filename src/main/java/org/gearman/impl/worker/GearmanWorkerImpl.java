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

package org.gearman.impl.worker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.gearman.GearmanFunction;
import org.gearman.GearmanLostConnectionAction;
import org.gearman.GearmanLostConnectionGrounds;
import org.gearman.GearmanLostConnectionPolicy;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;
import org.gearman.impl.GearmanImpl;
import org.gearman.impl.server.GearmanServerInterface;
import org.gearman.impl.serverpool.ControllerState;
import org.gearman.impl.serverpool.AbstractJobServerPool;

public class GearmanWorkerImpl extends AbstractJobServerPool<WorkerConnectionController> implements GearmanWorker {
	
	// TODO Do something else. I don't like the heart-beat mechanism 
	private static final long HEARTBEAT_PERIOD = 20000000000L; // TODO decouple property
	
	/**
	 * Periodically checks the state of the connections while jobs can be pulled 
	 * @author isaiah
	 */
	private final class Heartbeat implements Runnable {
		
		@Override
		public void run() {
			final long time = System.currentTimeMillis();
			
			for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
				switch(cc.getState()) {
				case CONNECTING:
					// If connecting, nothing to do until a connection is established
				case DROPPED:
					// If dropped, the controller id
				case WAITING:
					break;
				case OPEN:
					cc.timeoutCheck(time);
					break;
				case CLOSED:
					cc.openServer(false);
					break;
				default:
					assert false;
				}
			}
		}
	}
	
	private class InnerConnectionController extends WorkerConnectionController {
		
		private final class Reconnector implements Runnable {
			@Override
			public void run() {
				if(!GearmanWorkerImpl.this.funcMap.isEmpty()) {
					InnerConnectionController.super.openServer(false);
				}
			}
		}
		
		private Reconnector r;
		
		InnerConnectionController(GearmanServerInterface key) {
			super(GearmanWorkerImpl.this, key);
		}

		@Override
		public void onOpen(ControllerState oldState) {
			if(GearmanWorkerImpl.this.funcMap.isEmpty()) {
				super.closeServer();
			} else {
				super.onOpen(oldState);
			}
		}
		
		@Override
		protected Dispatcher getDispatcher() {
			return GearmanWorkerImpl.this.dispatcher;
		}

		@Override
		protected GearmanWorkerImpl getWorker() {
			return GearmanWorkerImpl.this;
		}

		@Override
		public void onConnect(ControllerState oldState) {
			connections.incrementAndGet();
			super.getKey().createGearmanConnection(this, this);
		}

		@Override
		public void onLostConnection(GearmanLostConnectionPolicy policy, GearmanLostConnectionGrounds grounds) {
			GearmanServer server = this.getKey();
			if(server==null) {
				// TODO log error
			}
			
			if(server.isShutdown()) {
				return;
			} else {
				GearmanLostConnectionAction action; 
				try {
					action = policy.lostConnection(server, grounds);
				} catch (Throwable t) {
					action = null;
				}
				
				if(action==null) {
					action = GearmanWorkerImpl.super.getDefaultPolicy().lostConnection(super.getKey(), grounds);
				} 
				
				switch(action) {
				case DROP:
					super.dropServer();
					break;
				case RECONNECT:
					super.waitServer(r==null? (r=new Reconnector()): r);
					break;
				default:
					throw new IllegalStateException("Unknown Action: " + action);
				}
			}
		}

		@Override
		public void onDrop(ControllerState oldState) {
			// No cleanup required
		}

		@Override
		public void onNew() {
			if(!GearmanWorkerImpl.this.funcMap.isEmpty()) {
				super.openServer(false);
			}
		}

		@Override
		public void onClose(ControllerState oldState) {
			connections.decrementAndGet();
			super.onClose(oldState);
		}

		@Override
		public void onWait(ControllerState oldState) { }
	}
	
	private final class FunctionInfo {
		private final GearmanFunction function;
		// private final long timeout;
		
		public FunctionInfo(GearmanFunction function, long timeout) {
			this.function = function;
			//this.timeout = timeout;
		}
	}
	
	private final Dispatcher dispatcher = new Dispatcher();
	private final ConcurrentHashMap<String, FunctionInfo> funcMap = new ConcurrentHashMap<String, FunctionInfo>();
	
	private final Heartbeat heartbeat = new Heartbeat();
	private ScheduledFuture<?> future;
	
	private AtomicInteger connections = new AtomicInteger(0);
	
	private boolean isConnected() {
		return connections.get()>0;
	}
	
	
	
	public GearmanWorkerImpl(final GearmanImpl gearman) {
		super(gearman, new GearmanLostConnectionPolicyImpl(), 60, TimeUnit.SECONDS);
		
	}

	@Override
	protected WorkerConnectionController createController(GearmanServerInterface key) {
		return new InnerConnectionController(key);
	}

	@Override
	public GearmanFunction addFunction(String name, GearmanFunction function) {
		return this.addFunction(name, function, 0, TimeUnit.MILLISECONDS);
	}

	
	public final GearmanFunction addFunction(String name, GearmanFunction function, long timeout, TimeUnit unit) {
		if(name==null || function==null) throw new IllegalArgumentException("null paramiter");
		
		final FunctionInfo newFunc = new FunctionInfo(function, unit.toMillis(timeout));
		
		synchronized(this.funcMap) {
			final FunctionInfo oldFunc = this.funcMap.put(name, newFunc);
			
			if(oldFunc!=null) return oldFunc.function;
			if(this.isConnected()) {
				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values())
					cc.canDo(name);
				
				if(this.future==null)this.future = super.getGearman().getScheduler().scheduleAtFixedRate(this.heartbeat, HEARTBEAT_PERIOD, HEARTBEAT_PERIOD, TimeUnit.NANOSECONDS);
				return null;
			}
			
			if(this.future==null)this.future = super.getGearman().getScheduler().scheduleAtFixedRate(this.heartbeat, HEARTBEAT_PERIOD, HEARTBEAT_PERIOD, TimeUnit.NANOSECONDS);
			
			for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
				cc.openServer(false);
			}
			
			return null;
		}
	}

	@Override
	public GearmanFunction getFunction(String name) {
		final FunctionInfo info = this.funcMap.get(name);
		return info==null? null: info.function;
	}

	/*
	@Override
	public long getFunctionTimeout(String name) {
		final FunctionInfo info = this.funcMap.get(name);
		return info==null? -1: info.timeout;
	}
	*/

	@Override
	public int getMaximumConcurrency() {
		return this.dispatcher.getMaxCount();
	}

	@Override
	public Set<String> getRegisteredFunctions() {
		return Collections.unmodifiableSet(this.funcMap.keySet());
	}

	@Override
	public boolean removeFunction(String functionName) {
		synchronized(this.funcMap) {
			final FunctionInfo info = this.funcMap.remove(functionName);
			if(info==null) return false;
			
			if(this.funcMap.isEmpty()) {
				if(this.future!=null) {
					future.cancel(false);
					future=null;
				}
				
				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
					cc.cantDo(functionName);
					cc.closeIfNotWorking();
				}
			} else {
				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
					cc.cantDo(functionName);
				}
			}
			return true;
		}
	}

	@Override
	public void setMaximumConcurrency(int maxConcurrentJobs) {
		this.dispatcher.setMaxCount(maxConcurrentJobs);
	}

	@Override
	public void removeAllServers() {
		
		synchronized(this.funcMap) {
			if(this.future!=null) {
				future.cancel(false);
				future = null;
			}
		}
		
		super.removeAllServers();
	}
	
	@Override
	public void shutdown() {
		super.shutdown();
		this.getGearman().onServiceShutdown(this);
	}

	@Override
	public void removeAllFunctions() {
		synchronized(this.funcMap) {
			this.funcMap.clear();
			if(this.future!=null) {
				future.cancel(false);
				future = null;
			}
			
			if(this.funcMap.isEmpty()) {
				if(this.future!=null) {
					future.cancel(false);
					future=null;
				}
				
				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
					cc.resetAbilities();
					cc.closeIfNotWorking();
				}
			} else {
				for(WorkerConnectionController cc : GearmanWorkerImpl.super.getConnections().values()) {
					cc.resetAbilities();
				}
			}
		}
	}
	
	@Override
	public void finalize() throws Throwable {
		super.finalize();
		this.shutdown();
	}
}
