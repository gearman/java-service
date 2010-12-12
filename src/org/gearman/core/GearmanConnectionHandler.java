package org.gearman.core;

public interface GearmanConnectionHandler<X> {
	public void onAccept(GearmanConnection<X> conn);
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<X> conn);
	public void onDisconnect(GearmanConnection<X> conn);
}
