package org.gearman.impl.serverpool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanLostConnectionPolicy;
import org.gearman.GearmanServer;
import org.gearman.impl.GearmanImpl;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.server.GearmanServerInterface;
import org.gearman.impl.server.ServerShutdownListener;

/**
 * A nasty class used to manage multa
 * 
 * @author isaiah
 */
public abstract class AbstractJobServerPool <X extends AbstractConnectionController> implements GearmanServerPool, ServerShutdownListener {
	static final String DEFAULT_CLIENT_ID = "-";
	
	private final GearmanImpl gearman;
	
	private final ConcurrentHashMap<GearmanServerInterface, X> connMap = new ConcurrentHashMap<GearmanServerInterface, X>();
	private final GearmanLostConnectionPolicy defaultPolicy;
	private GearmanLostConnectionPolicy policy;;
	private long waitPeriod;
	private boolean isShutdown = false;
	private String id = AbstractJobServerPool.DEFAULT_CLIENT_ID;
	
	protected AbstractJobServerPool(GearmanImpl gearman, GearmanLostConnectionPolicy defaultPolicy, long waitPeriod, TimeUnit unit) {
		this.defaultPolicy = defaultPolicy;
		this.policy = defaultPolicy;
		this.waitPeriod = unit.toNanos(waitPeriod);
		this.gearman = gearman;
	}
	
	@Override
	public boolean addServer(GearmanServer srvr) {
		if(!(srvr instanceof GearmanServerInterface))
			throw new IllegalArgumentException("Unsupported GearmanServer Implementation: " + srvr.getClass().getCanonicalName());
		
		GearmanServerInterface key = (GearmanServerInterface)srvr;
		
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		
		X x = this.createController(key);
		
		if(this.connMap.putIfAbsent(key, x)==null) {
			x.onNew();
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public GearmanImpl getGearman() {
		return this.gearman;
	}

	@Override
	public String getClientID() {
		return this.id;
	}

	@Override
	public long getReconnectPeriod(TimeUnit unit) {
		return unit.convert(this.waitPeriod,TimeUnit.NANOSECONDS);
	}

	@Override
	public int getServerCount() {
		return this.connMap.size();
	}

	@Override
	public boolean hasServer(GearmanServer srvr) {
		return this.connMap.containsKey(srvr);
	}

	@Override
	public void removeAllServers() {
		List<GearmanServer> srvrs = new ArrayList<GearmanServer>(this.connMap.keySet());
		for(GearmanServer srvr : srvrs) {
			this.removeServer(srvr);
		}
	}

	@Override
	public boolean removeServer(GearmanServer srvr) {
		return this.connMap.remove(srvr)!=null;
	}

	@Override
	public void setClientID(String id) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		if(this.id.equals(id)) return;
		
		for(X x : this.connMap.values()) {
			x.sendPacket(GearmanPacket.createSET_CLIENT_ID(id), null /** TODO */);
		}
	}

	@Override
	public void setLostConnectionPolicy(GearmanLostConnectionPolicy policy) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		
		if(this.policy==null)
			this.policy = this.defaultPolicy;
		else
			this.policy = policy;
	}

	@Override
	public void setReconnectPeriod(long time, TimeUnit unit) {
		if(this.isShutdown) throw new IllegalStateException("In Shutdown State");
		this.waitPeriod = unit.toNanos(time);
	}

	@Override
	public boolean isShutdown() {
		return this.isShutdown;
	}

	@Override
	public synchronized void shutdown() {
		if(this.isShutdown) return;
		this.isShutdown = true;
		
		this.removeAllServers();
	}
	
	protected Map<GearmanServerInterface, X> getConnections() {
		return Collections.unmodifiableMap(this.connMap);
	}
	
	protected GearmanLostConnectionPolicy getDefaultPolicy() {
		return this.defaultPolicy;
	}
	
	protected GearmanLostConnectionPolicy getPolicy() {
		return this.policy;
	}

	@Override
	public Collection<GearmanServer> getServers() {
		Collection<GearmanServer> value = new ArrayList<GearmanServer>(this.connMap.keySet());
		return value;
	}
	
	@Override
	public void onShutdown(GearmanServerInterface server) {
		this.removeServer(server);
		this.policy.shutdownServer(server);
	}
	
	/**
	 * Creates a new ConnectionControler to add to the JobServerPool<br>
	 * Note: The returned value is not guaranteed to be added to the set
	 * of connections.  
	 * @param key
	 * 		The ConnectionControler's key
	 * @return
	 * 		
	 */
	protected abstract X createController(GearmanServerInterface key);
}
