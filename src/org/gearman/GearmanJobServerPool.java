package org.gearman;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public interface GearmanJobServerPool extends GearmanService {
	public boolean addServer(GearmanServer srvr);
	public boolean addServer(InetSocketAddress adrs);
	public String getClientID();
	public long getReconnectPeriod(TimeUnit unit);
	public int getServerCount();
	public void removeAllServers();
	public boolean removeServer(GearmanServer srvr);
	public boolean removeServer(InetSocketAddress adrs);
	public void setClientID(String id);
	public void setLostConnectionPolicy(GearmanLostConnectionPolicy policy);
	public void setReconnectPeriod(long time, TimeUnit unit);
	public boolean hasServer(InetSocketAddress address);
	public boolean hasServer(GearmanServer srvr);
}
