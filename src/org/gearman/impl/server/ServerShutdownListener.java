package org.gearman.impl.server;

public interface ServerShutdownListener {
	public void onShutdown(GearmanServerInterface server);
}
