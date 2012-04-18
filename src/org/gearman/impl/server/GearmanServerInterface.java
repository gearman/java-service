package org.gearman.impl.server;

import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnectionHandler;
import org.gearman.impl.core.GearmanConnectionManager.ConnectCallbackResult;

public interface GearmanServerInterface extends org.gearman.GearmanServer {
	public <A> void createGearmanConnection(GearmanConnectionHandler<A> handler, GearmanCallbackHandler<GearmanServerInterface, ConnectCallbackResult> failCallback);
	public void addShutdownListener(ServerShutdownListener listener);
	public void removeShutdownListener(ServerShutdownListener listener);
}