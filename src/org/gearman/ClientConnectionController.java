package org.gearman;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.gearman.GearmanLostConnectionPolicy.Grounds;
import org.gearman.JobServerPoolAbstract.ConnectionController;
import org.gearman.JobServerPoolAbstract.ControllerState;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanPacket;
import org.gearman.util.ByteArray;

public class ClientConnectionController<K> extends ConnectionController<K> implements Comparable<ClientConnectionController<?>> {

	private final int seq;
	
	private final ConcurrentHashMap<ByteArray, GearmanJob> jobMap = new ConcurrentHashMap<ByteArray, GearmanJob>();
	private final Queue<GearmanJob> queuedJobs = new LinkedBlockingQueue<GearmanJob>();
	
	private long responceTime = Long.MAX_VALUE;
	private long idleTime = Long.MAX_VALUE;
	
	ClientConnectionController(JobServerPoolAbstract<?> sc, K key, int seq) {
		super(sc, key);
		this.seq = seq;
	}

	@Override
	protected void onNew() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onDrop(ControllerState oldState) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onLostConnection(GearmanLostConnectionPolicy policy,
			Grounds grounds) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onConnect(ControllerState oldState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
		switch (packet.getPacketType()) {
		case JOB_CREATED:
		case WORK_STATUS:
		case WORK_COMPLETE:
		case WORK_FAIL:
		case ECHO_RES: //Not used
		case ERROR:
		case STATUS_RES:
		case WORK_EXCEPTION:
		case OPTION_RES:
		case WORK_DATA:
		case WORK_WARNING:
		default: // Not a client response
			assert false;
		}
	}

	@Override
	protected void onClose(ControllerState oldState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onOpen(ControllerState oldState) {
		// 
	}

	@Override
	protected void onWait(ControllerState oldState) {
		// Move to waiting queue
	}

	@Override
	public int compareTo(ClientConnectionController<?> o) {
		return o.seq - this.seq;
	}
}
