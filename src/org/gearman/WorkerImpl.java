package org.gearman;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanLostConnectionPolicy.Action;
import org.gearman.GearmanLostConnectionPolicy.Grounds;

class WorkerImpl extends JobServerPoolAbstract<WorkerConnectionController<?,?>> implements GearmanWorker {
	
	private static final long HEARTBEAT_PERIOD = 20000000000L;
	
	/**
	 * Periodically checks the state of the connections while jobs can be pulled 
	 * @author isaiah
	 */
	private final class Heartbeat implements Runnable {
		
		@Override
		public void run() {
			final long time = System.currentTimeMillis();
			for(WorkerConnectionController<?,?> cc : WorkerImpl.super.getConnections().values()) {
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
	
	private class LocalConnectionController extends WorkerConnectionController<GearmanServer, org.gearman.GearmanServer.ConnectCallbackResult> {
		
		LocalConnectionController(GearmanServer key) {
			super(WorkerImpl.this, key);
		}

		@Override
		protected WorkerDispatcher getDispatcher() {
			return WorkerImpl.this.dispatcher;
		}

		@Override
		protected WorkerImpl getWorker() {
			return WorkerImpl.this;
		}

		@Override
		protected void onConnect(ControllerState oldState) {
			super.getKey().createGearmanConnection(this, this);
		}

		@Override
		protected void onLostConnection(GearmanLostConnectionPolicy policy, Grounds grounds) {
			policy.lostLocalServer(super.getKey(), WorkerImpl.this, grounds);
		}

		@Override
		protected void onDrop(ControllerState oldState) {
			// No cleanup required
		}

		@Override
		public void onOpen(ControllerState oldState) {
			if(WorkerImpl.this.funcMap.isEmpty()) {
				super.closeServer();
			} else {
				super.onOpen(oldState);
			}
		}
		
		@Override
		protected void onNew() {
			if(!WorkerImpl.this.funcMap.isEmpty()) {
				super.openServer(false);
			}
		}

		@Override
		protected void onClose(ControllerState oldState) { }

		@Override
		protected void onWait(ControllerState oldState) { }
	}
	
	private class RemoteConnectionController extends WorkerConnectionController<InetSocketAddress, org.gearman.core.GearmanConnectionManager.ConnectCallbackResult> {
		
		private final class Reconnector implements Runnable {
			@Override
			public void run() {
				if(!WorkerImpl.this.funcMap.isEmpty()) {
					RemoteConnectionController.super.openServer(false);
				}
			}
		}
		
		private Reconnector r;
		
		RemoteConnectionController(InetSocketAddress key) {
			super(WorkerImpl.this, key);
		}

		@Override
		public void onOpen(ControllerState oldState) {
			if(WorkerImpl.this.funcMap.isEmpty()) {
				super.closeServer();
			} else {
				super.onOpen(oldState);
			}
		}
		
		@Override
		protected WorkerDispatcher getDispatcher() {
			return WorkerImpl.this.dispatcher;
		}

		@Override
		protected WorkerImpl getWorker() {
			return WorkerImpl.this;
		}

		@Override
		protected void onConnect(ControllerState oldState) {
			gearman.getGearmanConnectionManager().createGearmanConnection(super.getKey(), this, this);
		}

		@Override
		protected void onLostConnection(GearmanLostConnectionPolicy policy, Grounds grounds) {
			Action action = null;
			try {
				action = policy.lostRemoteServer(super.getKey(), WorkerImpl.this, grounds);
			} catch (Throwable t) {
				action = null;
			}
			
			if(action==null) {
				action = WorkerImpl.super.getDefaultPolicy().lostRemoteServer(super.getKey(), WorkerImpl.this, grounds);
				assert action!=null;
			} else if (action.equals(Action.DROP)) {
				super.dropServer();
			} else if(action.equals(Action.RECONNECT)) {
				super.waitServer(r==null? (r=new Reconnector()): r);
			}else {
				super.waitServer(r==null? (r=new Reconnector()): r, action.getNanoTime(), TimeUnit.NANOSECONDS);
			}
		}

		@Override
		protected void onDrop(ControllerState oldState) {
			// No cleanup required
		}

		@Override
		protected void onNew() {
			if(!WorkerImpl.this.funcMap.isEmpty()) {
				super.openServer(false);
			}
		}

		@Override
		protected void onClose(ControllerState oldState) { }

		@Override
		protected void onWait(ControllerState oldState) { }
	}
	
	private final class FunctionInfo {
		public final GearmanFunction function;
		public final long timeout;
		
		public FunctionInfo(GearmanFunction function, long timeout) {
			this.function = function;
			this.timeout = timeout;
		}
	}
	
	private final Gearman gearman;
	private final WorkerDispatcher dispatcher = new WorkerDispatcher();
	private final ConcurrentHashMap<String, FunctionInfo> funcMap = new ConcurrentHashMap<String, FunctionInfo>();
	
	private final Heartbeat heartbeat = new Heartbeat();
	private ScheduledFuture<?> future;
	
	private boolean isConnected = false;
	
	WorkerImpl(final Gearman gearman) {
		super(new WorkerLostConnectionPolicy(), 60, TimeUnit.SECONDS);
		
		assert gearman!=null;
		this.gearman = gearman;
	}

	@Override
	protected WorkerConnectionController<?,?> createController(GearmanServer key) {
		return new LocalConnectionController(key);
	}

	@Override
	protected WorkerConnectionController<?,?> createController(InetSocketAddress key) {
		return new RemoteConnectionController(key);
	}

	@Override
	public Gearman getGearman() {
		return this.gearman;
	}

	@Override
	public GearmanFunction addFunction(String name, GearmanFunction function) {
		return this.addFunction(name, function, 0, TimeUnit.MILLISECONDS);
	}

	@Override
	public final GearmanFunction addFunction(String name, GearmanFunction function, long timeout, TimeUnit unit) {
		if(name==null || function==null) throw new IllegalArgumentException("null paramiter");
		
		final FunctionInfo newFunc = new FunctionInfo(function, unit.toMillis(timeout));
		
		synchronized(this.funcMap) {
			final FunctionInfo oldFunc = this.funcMap.put(name, newFunc);
			
			if(oldFunc!=null) return oldFunc.function;
			if(this.isConnected) return null;
			
			this.isConnected = true;
			
			this.future = this.gearman.getPool().scheduleAtFixedRate(this.heartbeat, HEARTBEAT_PERIOD, HEARTBEAT_PERIOD, TimeUnit.NANOSECONDS);
			for(WorkerConnectionController<?,?> w : super.getConnections().values()) {
				w.openServer(false);
			}
			
			return null;
		}
	}

	@Override
	public GearmanFunction getFunction(String name) {
		final FunctionInfo info = this.funcMap.get(name);
		return info==null? null: info.function;
	}

	@Override
	public long getFunctionTimeout(String name) {
		final FunctionInfo info = this.funcMap.get(name);
		return info==null? -1: info.timeout;
	}

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
				this.isConnected = false;
				if(this.future!=null) future.cancel(false);
				for(WorkerConnectionController<?,?> conn : super.getConnections().values()) {
					conn.closeServer();
				}
			} else {
				for(WorkerConnectionController<?,?> conn : super.getConnections().values()) {
					conn.cantDo(functionName);
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
	public void unregisterAll() {
		
		/*
		 * TODO As of right now, when a worker has no function, connections are immediately closed.
		 * I'd like to see them timeoutt. This way if the user adds 
		 */
		
		synchronized(this.funcMap) {
			for(WorkerConnectionController<?,?> conn : super.getConnections().values()) {
				conn.closeServer();
			}
			this.funcMap.clear();
			if(this.future!=null) future.cancel(false);
		}
	}
	
	@Override
	public void shutdown() {
		super.shutdown();
		gearman.onServiceShutdown(this);
	}
}
