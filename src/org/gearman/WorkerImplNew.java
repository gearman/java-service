package org.gearman;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.gearman.core.GearmanCompletionHandler;
import org.gearman.core.GearmanPacket;


public class WorkerImplNew implements GearmanWorker {
	
	private static final String DEFAULT_CLIENT_ID = "-";
	private static final int TIME_INTERVAL = 20000;
	private static final int RECONNECT_MOD = 6; 
	
	private abstract class ConnectionController<K,A> extends WorkerConnectionController<K,A> {
		private boolean isRemoved = false;
		
		public ConnectionController(final K key) {
			super(key);
			
			WorkerImplNew.this.connMap.put(key, this);
		}

		@Override
		protected final WorkerDispatcher getDispatcher() {
			return WorkerImplNew.this.dispatcher;
		}

		@Override
		protected final GearmanWorker getWorker() {
			return WorkerImplNew.this;
		}

		@Override
		protected final void remove() {
			synchronized(this) {
				if(this.isRemoved) return;
				this.isRemoved = true;
			}
			
			WorkerImplNew.this.connMap.remove(this.getKey());
		}
	}
	
	private final class LocalConnectionController<A> extends ConnectionController<GearmanServer, A> {
		public LocalConnectionController(GearmanServer key) {
			super(key);
		}

		@Override
		protected void connect() {
			super.getKey().createGearmanConnection(this);			
		}

		@Override
		protected void onDisconnect() {
			if(super.isRemoved) return;
			this.remove();
		}
	}
	
	private final class RemoteConnectionController<A> extends ConnectionController<InetSocketAddress, A> {
		public RemoteConnectionController(InetSocketAddress key) {
			super(key);
		}

		@Override
		protected void connect() {
			WorkerImplNew.this.getGearman().getGearmanConnectionManager().createGearmanConnection(super.getKey(), this, null, this);
		}

		@Override
		protected void onDisconnect() {
			if(super.isRemoved) return;
			//TODO reconnect
		}
	}
	
	private static final class FunctionInfo {
		public final GearmanFunction func;
		public final long timeout;
		
		public FunctionInfo(final GearmanFunction func, final long timeout) {
			this.func = func;
			this.timeout = timeout;
		}
	}
	
	private final Gearman gearman;
	private final ConcurrentHashMap<String, FunctionInfo> funcMap = new ConcurrentHashMap<String, FunctionInfo>();
	private final ConcurrentHashMap<Object, WorkerConnectionController<?,?>> connMap = new ConcurrentHashMap<Object, WorkerConnectionController<?,?>>();
	private final LinkedBlockingQueue<WorkerConnectionController<?,?>> lostConnections = new LinkedBlockingQueue<WorkerConnectionController<?,?>>();
	private final WorkerDispatcher dispatcher = new WorkerDispatcher();
	private String id = WorkerImplNew.DEFAULT_CLIENT_ID;
	
	public WorkerImplNew(final Gearman gearman) {
		this.gearman = gearman;
	}
	
	@Override
	public final GearmanFunction addFunction(final String name, final GearmanFunction function) {
		return addFunction(name,function,0);
	}

	@Override
	public final GearmanFunction addFunction(final String name, final GearmanFunction function, final long timeout) {
		if(timeout<0) throw new IllegalArgumentException("timeout<0");
		
		final FunctionInfo newFunc = new FunctionInfo(function,timeout);
		final FunctionInfo oldFunc = this.funcMap.put(name, newFunc);
		
		for(WorkerConnectionController<?,?> cc : this.connMap.values()) {
			cc.sendPacket(GearmanPacket.createCAN_DO(name), null,null/*TODO*/);
		}
		
		return oldFunc==null? null: oldFunc.func;
	}

	@Override
	public final <A> void addServer(final InetSocketAddress adrs, final A att, final GearmanCompletionHandler<A> callback) {
		if(adrs==null) throw new IllegalArgumentException("adrs == null");
		
		final ConnectionController<?,A> cc = new RemoteConnectionController<A>(adrs);
		cc.connect(att, callback);
	}

	@Override
	public final <A> void addServer(String host, int port, A att, GearmanCompletionHandler<A> callback)  {
		this.addServer(new InetSocketAddress(host,port), att, callback);
	}

	@Override
	public final <A> void addServer(GearmanServer srvr, A att, GearmanCompletionHandler<A> callback) {
		if(srvr==null) throw new IllegalArgumentException("srvr == null");
		
		final ConnectionController<?,A> cc = new LocalConnectionController<A>(srvr);
		cc.connect(att, callback);
	}

	@Override
	public Set<String> getRegisteredFunctions() {
		return Collections.unmodifiableSet(this.funcMap.keySet());
	}

	@Override
	public int getServerCount() {
		return this.funcMap.size();
	}

	@Override
	public String getWorkerID() {
		return this.id;
	}

	@Override
	public boolean hasServer(InetSocketAddress address) {
		return this.connMap.contains(address);
	}

	@Override
	public boolean hasServer(String host, int port) {
		return this.connMap.contains(new InetSocketAddress(host,port));
	}

	@Override
	public boolean hasServer(GearmanServer srvr) {
		return this.connMap.contains(srvr);
	}

	@Override
	public void removeAllServers() {
		for(WorkerConnectionController<?,?> cc : this.connMap.values()) {
			cc.remove();
		}
		this.connMap.clear();
	}

	@Override
	public boolean removeFunction(String functionName) {
		final FunctionInfo info = this.funcMap.remove(functionName);
		if(info==null) return false;
		
		for(WorkerConnectionController<?,?> cc : this.connMap.values()) {
			cc.sendPacket(GearmanPacket.createCANT_DO(functionName), null,/*TODO*/null);
		}
		
		return true;
	}

	@Override
	public boolean removeServer(InetSocketAddress adrs) {
		return removeServerConnection(adrs);
	}

	@Override
	public boolean removeServer(String host, int port) {
		return removeServerConnection(new InetSocketAddress(host,port));
	}

	@Override
	public boolean removeServer(GearmanServer server) {
		return removeServerConnection(server);
	}
	
	private final boolean removeServerConnection(final Object key) {
		final WorkerConnectionController<?,?> cc = this.connMap.get(key);
		if(cc==null) return false;
		
		cc.remove();
		return true;
	}

	@Override
	public final void setWorkerID(final String id) {
		this.id = id;
		
		for(WorkerConnectionController<?,?> cc : this.connMap.values()) {
			cc.sendPacket(GearmanPacket.createSET_CLIENT_ID(id), null,null /*TODO*/);
		}
	}

	@Override
	public void setMaximumConcurrency(int count) {
		this.dispatcher.setMaxCount(count);
	}

	@Override
	public int getMaximumConcurrency() {
		return this.dispatcher.getMaxCount();
	}

	@Override
	public void unregisterAll() {
		for(WorkerConnectionController<?,?> cc : this.connMap.values()) {
			cc.sendPacket(GearmanPacket.createRESET_ABILITIES(), null,/*TODO*/null);
		}
		
		this.funcMap.clear();
	}

	@Override
	public Gearman getGearman() {
		return this.gearman;
	}

	@Override
	public boolean isShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
	}

	@Override
	public GearmanFunction getFunction(String name) {
		final FunctionInfo info = this.funcMap.get(name);
		return info==null? null: info.func;
	}

	@Override
	public long getFunctionTimeout(String name) {
		final FunctionInfo info = this.funcMap.get(name);
		return info==null? null: info.timeout;
	}
}
