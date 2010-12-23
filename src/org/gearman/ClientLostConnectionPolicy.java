package org.gearman;

import java.net.InetSocketAddress;

public class ClientLostConnectionPolicy implements GearmanLostConnectionPolicy {
	@Override
	public void lostLocalServer(GearmanServer server, GearmanJobServerPool service, Grounds grounds) {
		// TODO Auto-generated method stub
	}

	@Override
	public Action lostRemoteServer(InetSocketAddress adrs, GearmanJobServerPool service, Grounds grounds) {
		return null;
	}	
}
