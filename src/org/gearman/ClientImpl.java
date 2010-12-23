package org.gearman;

import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArrayList;

import org.gearman.core.GearmanCompletionHandler;

/**
 * In this implementation a client will prioritize servers with the order that there're added.
 * Jobs are sent to the server with the highest priority. If that server goes down, jobs will
 * go to the server with the next highest priority.
 * 
 * When a server is dropped or removed, servers with a lower priority will be moved up.
 * 
 * If a job fails to be sent to any of the available servers, the job fails and the completion
 * handler (if given) is notified of the failure.
 * 
 * @author isaiah
 */
public class ClientImpl extends JobServerPoolAbstract<ClientConnectionController<?>> implements GearmanClient {
	
	final CopyOnWriteArrayList<ClientConnectionController<?>> servers = new CopyOnWriteArrayList<ClientConnectionController<?>>();
	
	ClientImpl(final Gearman gearman) {
		super(new ClientLostConnectionPolicy());
		// TODO Auto-generated constructor stub
	}

	@Override
	protected ClientConnectionController<?> createController(GearmanServer key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ClientConnectionController<?> createController(
			InetSocketAddress key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setExceptionChannel(boolean isOpen) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <A> void submitJob(GearmanBackgroundJob job, A att,
			GearmanCompletionHandler<A> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <A> void submitJob(GearmanJob job, A att,
			GearmanCompletionHandler<A> callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <A> void submitJob(GearmanJob job) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Gearman getGearman() {
		// TODO Auto-generated method stub
		return null;
	}
}
