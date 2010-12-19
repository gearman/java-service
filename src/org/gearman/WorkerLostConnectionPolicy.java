package org.gearman;

import java.net.InetSocketAddress;

public class WorkerLostConnectionPolicy implements GearmanLostConnectionPolicy {

	@Override
	public void lostLocalServer(GearmanServer server, GearmanJobServerPool service, Grounds grounds) {
	}

	@Override
	public Action lostRemoteServer(InetSocketAddress adrs, GearmanJobServerPool service, Grounds grounds) {
		if(grounds.equals(Grounds.RESPONCE_TIMEOUT)) {
			return Action.dropServer();
		} else {
			return Action.reconnectServer();
		}
	}
}
