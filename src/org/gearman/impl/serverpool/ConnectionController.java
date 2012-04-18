package org.gearman.impl.serverpool;

import java.util.concurrent.TimeUnit;

import org.gearman.GearmanLostConnectionGrounds;
import org.gearman.GearmanLostConnectionPolicy;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanConnection.SendCallbackResult;
import org.gearman.impl.util.ByteArray;

public interface ConnectionController {
	public ControllerState getControllerState();
	public void getStatus(final ByteArray jobHandle, GearmanJobStatusCallback callback);
	public void dropServer();
	public void closeServer();
	public void waitServer(Runnable callback);
	public void waitServer(Runnable callback, long waittime, final TimeUnit unit);
	public boolean openServer(final boolean force);
	public boolean sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback);
	
	public void onConnect(ControllerState oldState);
	public void onOpen(ControllerState oldState);
	public void onClose(ControllerState oldState);
	public void onDrop(ControllerState oldState);
	public void onWait(ControllerState oldState);
	public void onNew();
	public void onLostConnection(GearmanLostConnectionPolicy policy, GearmanLostConnectionGrounds grounds);
}
