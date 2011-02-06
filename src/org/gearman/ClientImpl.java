package org.gearman;

import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanLostConnectionPolicy.Action;
import org.gearman.GearmanLostConnectionPolicy.Grounds;

/**
 * @author isaiah
 */
class ClientImpl extends JobServerPoolAbstract<ClientImpl.InnerConnectionController<?>> implements GearmanClient {
	
	private static final long HEARTBEAT_PERIOD = 10000000000L;
	
	private final class Heartbeat implements Runnable {

		/*
		 * It's my plan to have the client support multiple live connections in the future.
		 * When that happens, the Heartbeat will manage open connections.
		 */
		
		private ScheduledFuture<?> future;
		
		@Override
		public void run() {
			
			final InnerConnectionController<?> openCC = ClientImpl.this.openServer;
			if(openCC==null) return;
			
			final long time = System.currentTimeMillis();
			openCC.timeoutCheck(time);			
		}
		
		public final synchronized void start() {
			if(future!=null) return;
			this.future = ClientImpl.this.gearman.getPool().scheduleAtFixedRate(this, HEARTBEAT_PERIOD, HEARTBEAT_PERIOD, TimeUnit.NANOSECONDS);
		}
		
		public final synchronized void stop() {
			this.future.cancel(false);
			this.future = null;
		}
	}
	
	abstract class InnerConnectionController<K> extends ClientConnectionController<K> {
	
		InnerConnectionController(K key) {
			super(ClientImpl.this ,key);
		}
		
		@Override
		protected void onClose(ControllerState oldState) {
			
			switch(oldState) {
			case OPEN:
				assert ClientImpl.this.openServer == this;
				
				/*
				 * TODO
				 * when the client supports more then one connection, check
				 * that this is the last open connection before stopping the
				 * heartbeat
				 */
				ClientImpl.this.heartbeat.stop();
				
				ClientImpl.this.openServer = null;
				ClientImpl.this.availableServers.add(this);
				break;
			case WAITING:
				ClientImpl.this.availableServers.add(this);
				break;
			case DROPPED:
			case CONNECTING:
			case CLOSED:
			}
			
			super.onClose(oldState);
		}

		@Override
		protected void onDrop(ControllerState oldState) {
			
			switch(oldState) {
			case CONNECTING:
			case CLOSED:
				ClientImpl.this.availableServers.remove(this);
				break;
			case OPEN:
				assert ClientImpl.this.openServer==this;
				
				/*
				 * TODO
				 * when the client supports more then one connection, check
				 * that this is the last open connection before stopping the
				 * heartbeat
				 */
				ClientImpl.this.heartbeat.stop();
				
				ClientImpl.this.openServer = null;
				break;
			case DROPPED:
			case WAITING:
			}
			
			super.onDrop(oldState);
		}
	
		@Override
		protected void onNew() {
			// In closed state
			
			assert this.getState().equals(ControllerState.CLOSED);
			ClientImpl.this.availableServers.add(this);
		}
		
		@Override
		protected void onOpen(ControllerState oldState) {
			switch(oldState) {
			case CONNECTING:
			case CLOSED:
				// asign the open server
				assert ClientImpl.this.openServer==null;
				ClientImpl.this.openServer = this;
				
				// start heartbeat.
				ClientImpl.this.heartbeat.start();				
				
				// remove this server from
				ClientImpl.this.availableServers.removeFirst();
				ClientImpl.this.availableServers.clearFailKeys();
				
				// 
				this.grab();
				
				break;
			case OPEN:
			case DROPPED:
			case WAITING:
			}
		}
		
		@Override
		protected void onWait(ControllerState oldState) {
			switch(oldState) {
			case CONNECTING:
			case CLOSED:
				ClientImpl.this.failTo(ClientImpl.this.availableServers.removeFirst());
				break;
			case OPEN:
				assert ClientImpl.this.openServer == this;
				ClientImpl.this.openServer = null;
				
				/*
				 * TODO
				 * when the client supports more then one connection, check
				 * that this is the last open connection before stopping the
				 * heartbeat
				 */
				ClientImpl.this.heartbeat.stop();
				break;
				
			case DROPPED:
			case WAITING:
			}
			
			super.onWait(oldState);
		}
		
		@Override
		protected ClientJobSubmission pollNextJob() {
			return ClientImpl.this.jobQueue.poll();
		}

		@Override
		protected void requeueJob(ClientJobSubmission jobSub) {
			ClientImpl.this.jobQueue.addFirst(jobSub);
		}
		
		@Override
		protected Gearman getGearman() {
			return ClientImpl.this.gearman;
		}
	}
	
	private final class LocalConnectionController extends InnerConnectionController<GearmanServer> {
	
		LocalConnectionController(GearmanServer key) {
			super(key);
		}
	
		@Override
		protected void onLostConnection(GearmanLostConnectionPolicy policy, Grounds grounds) {
			policy.lostLocalServer(this.getKey(), ClientImpl.this, grounds);
			super.dropServer();
		}
	
		@Override
		protected void onConnect(ControllerState oldState) {
			super.getKey().createGearmanConnection(this, null, this);
		}
	}
	
	private final class RemoteConnectionController extends InnerConnectionController<InetSocketAddress> {

		RemoteConnectionController(InetSocketAddress key) {
			super(key);
		}
		
		@Override
		protected void onLostConnection(GearmanLostConnectionPolicy policy, Grounds grounds) {
			Action action = null;
			
			try {
				action = policy.lostRemoteServer(this.getKey(), ClientImpl.this, grounds);
			} catch (Throwable t) {
				action = null;
			}
			
			if(action==null) {
				action = ClientImpl.this.getDefaultPolicy().lostRemoteServer(this.getKey(), ClientImpl.this, grounds);
				assert action!=null;
			}
			
			if(action.equals(Action.DROP)) {
				// Drop
				super.dropServer();
			} else if(action.equals(Action.RECONNECT)) {
				// Default reconnect
				assert ClientImpl.this.availableServers.peek()==this;
				ClientImpl.this.failTo(ClientImpl.this.availableServers.removeFirst());
				ClientImpl.this.availableServers.add(this);
			} else {
				// User defined reconnect
				if(action.getNanoTime()==0) {
					assert ClientImpl.this.availableServers.peek()==this;
					ClientImpl.this.failTo(ClientImpl.this.availableServers.removeFirst());
					ClientImpl.this.availableServers.add(this);
				}
				super.waitServer(null, action.getNanoTime(), TimeUnit.NANOSECONDS);
			}
		}

		@Override
		protected void onConnect(ControllerState oldState) {
			ClientImpl.this.gearman.getGearmanConnectionManager().createGearmanConnection(super.getKey(), this, null, this);
		}
	}
		
	/** The gearman parent */
	private final Gearman gearman;
	
	/** The set of servers currently open */
	private InnerConnectionController<?> openServer;
	private final Heartbeat heartbeat = new Heartbeat();
	private final ClientConnectionList<InnerConnectionController<?>, ClientJobSubmission> availableServers = new ClientConnectionList<InnerConnectionController<?>, ClientJobSubmission>();
	private final LinkedBlockingDeque<ClientJobSubmission> jobQueue = new LinkedBlockingDeque<ClientJobSubmission>();
	
	/**
	 * Create a new ClientImpl
	 * @param gearman
	 * 		the gearman provider
	 */
	ClientImpl(final Gearman gearman) {
		// Initialize the GearmanJobServerPool
		super(new ClientLostConnectionPolicy(), 0, TimeUnit.NANOSECONDS);
		
		// Set the gearman provider
		this.gearman = gearman;
	}

	@Override
	protected InnerConnectionController<?> createController(GearmanServer key) {
		/*
		 * This method is used by the GearmanJobServerPool to create ConnectionControllers
		 * with connections in the local address space.
		 * 
		 * Creation does not imply it'll be used in the system.
		 */
		
	 	return new LocalConnectionController(key);
	}

	@Override
	protected InnerConnectionController<?> createController(InetSocketAddress key) {
		/*
		 * This method is used by the GearmanJobServerPool to create ConnectionControllers
		 * with remote connections.
		 * 
		 * Creation does not imply it'll be used in the system.
		 */
		
		return new RemoteConnectionController(key);
	}
	
	@Override
	public Gearman getGearman() {
		return this.gearman;
	}

	@Override
	public void submitJob(GearmanJob job, SubmitHandler callback) {
		if(this.isShutdown()) {
			if(callback!=null) {
				callback.onSubmissionComplete(job,SubmitResult.FAILED_TO_SHUTDOWN);
			}
			return;
		}
		
		if(!job.submit()) {
			if(callback!=null) {
				callback.onSubmissionComplete(job,SubmitResult.FAILED_TO_INVALID_JOB_STATE);
			}
			return;
		}
		final ClientJobSubmission jobSub = new ClientJobSubmission(job,callback,job instanceof GearmanBackgroundJob);
		
		synchronized(this.availableServers) {
			// Check the connection state
			if(this.openServer!=null) {
				// connection is open. notify the connection
				
				// Add job to job queue
				this.jobQueue.addLast(jobSub);
				
				// notify server
				this.openServer.grab();
				
			} else {
				// connection is not open. make a connection
				
				final InnerConnectionController<?> icc;
				if ((icc = this.availableServers.tryFirst(jobSub))!=null){
					
					// Add job to job queue
					this.jobQueue.addLast(jobSub);
					
					// Make a connection
					icc.openServer(false);
					
				} else {
					// No available servers to connect to, fail job
					jobSub.onSubmissionComplete(GearmanClient.SubmitResult.FAILED_TO_NO_SERVER);
				}
			}
		}
	}
	
	private final void failTo(final ClientJobSubmission jobSub) {
		if(jobSub==null) return;
		
		assert this.jobQueue.contains(jobSub);
		
		ClientJobSubmission failMe;
		do {
			failMe=this.jobQueue.poll();
			assert failMe!=null;
			
			failMe.onSubmissionComplete(GearmanClient.SubmitResult.FAILED_TO_CONNECT);
		} while(failMe!=jobSub);
	}
}
