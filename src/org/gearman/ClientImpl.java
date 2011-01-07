package org.gearman;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanJob.Priority;
import org.gearman.GearmanLostConnectionPolicy.Action;
import org.gearman.GearmanLostConnectionPolicy.Grounds;
import org.gearman.core.GearmanCompletionHandler;
import org.gearman.core.GearmanPacket;

/**
 * @author isaiah
 */
class ClientImpl extends JobServerPoolAbstract<ClientImpl.InnerConnectionController<?>> implements GearmanClient {
	
	abstract class InnerConnectionController<K> extends ClientConnectionController<K> {
	
		InnerConnectionController(K key) {
			super(ClientImpl.this ,key);
		}
		
		public final void grab() {
			
			final GearmanJob job;
			synchronized(this) {
				if(super.getPendingJob()!=null) return;
				final ClientJobSubmission<?> jobSub = ClientImpl.this.jobQueue.poll();
				super.setPendingJob(jobSub);
				
				if(jobSub==null) return;
				job = jobSub.job;
			}
			
			final Priority p = job.getJobPriority();
			final String funcName = job.getFunctionName();
			final byte[] uID = job.getUniqueID();
			final byte[] data = job.getJobData();
			
			switch(p) {
			case LOW_PRIORITY:
				// TODO
			case HIGH_PRIORITY:
				// TODO
			case NORMAL_PRIORITY:
				this.getConnection().sendPacket(GearmanPacket.createSUBMIT_JOB(funcName, uID, data), null, null);
			}
		}
		
		@Override
		protected final ClientJobSubmission<?> grabJob() {
			return ClientImpl.this.jobQueue.poll();
		}
		
		@Override
		protected void onClose(ControllerState oldState) {
			switch(oldState) {
			case OPEN:
				assert ClientImpl.this.openServer == this;
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
				ClientImpl.this.openServer = null;
				break;
			case DROPPED:
			case WAITING:
			}
		}
	
		@Override
		protected void onNew() {
			assert this.getState().equals(ControllerState.CLOSED);
			ClientImpl.this.availableServers.add(this);
		}
		
		@Override
		protected void onOpen(ControllerState oldState) {
			switch(oldState) {
			case CONNECTING:
			case CLOSED:
				assert ClientImpl.this.openServer==null;
				ClientImpl.this.availableServers.removeFirst();
				ClientImpl.this.availableServers.clearFailKeys();
				ClientImpl.this.openServer = this;
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
				break;
			case DROPPED:
			case WAITING:
			}
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
	private final ClientConnectionList<InnerConnectionController<?>, ClientJobSubmission<?>> availableServers = new ClientConnectionList<InnerConnectionController<?>, ClientJobSubmission<?>>();
	private final LinkedBlockingDeque<ClientJobSubmission<?>> jobQueue = new LinkedBlockingDeque<ClientJobSubmission<?>>();
	
	/** Specifies if connections should forward exception packets */
	private boolean isExceptionChannelOpen = false;
	
	ClientImpl(final Gearman gearman) {
		super(new ClientLostConnectionPolicy(), 0, TimeUnit.NANOSECONDS);
		this.gearman = gearman;
	}

	@Override
	protected InnerConnectionController<?> createController(GearmanServer key) {
	 	return new LocalConnectionController(key);
	}

	@Override
	protected InnerConnectionController<?> createController(InetSocketAddress key) {
		return new RemoteConnectionController(key);
	}

	@Override
	public void setExceptionChannelOpen(boolean isOpen) {
		this.isExceptionChannelOpen = isOpen;
	}

	@Override
	public void submitJob(GearmanJob job) {
		final ClientJobSubmission<?> jobSub = new ClientJobSubmission<Object>(job,null,null);
		
		synchronized(this.availableServers) {
			// Check the connection state
			if(this.openServer!=null) {
				// If a connection is open, notify the connection
				
				// Add job to job queue
				this.jobQueue.addLast(jobSub);
				
				// notify server
				this.openServer.grab();
			} else {
				// If a connection is not open, make a connection
				
				final InnerConnectionController<?> icc;
				if ((icc = this.availableServers.tryFirst(jobSub))!=null){
					
					// Add job to job queue
					this.jobQueue.addLast(jobSub);
					
					// Make a connection
					icc.openServer(false);
				} else {
					// No available servers to connect to, fail job
					
					job.onComplete(GearmanJobResult.CLIENT_FAIL);
				}
			}
		}
	}

	@Override
	public Gearman getGearman() {
		return this.gearman;
	}

	@Override
	public boolean isExceptionChannelOpen() {
		return this.isExceptionChannelOpen;
	}
	
	@Override
	public <A> void submitJob(GearmanBackgroundJob job, A att, GearmanCompletionHandler<A> callback) {
		// TODO Auto-generated method stub
	}

	@Override
	public <A> void submitJob(GearmanJob job, A att, GearmanCompletionHandler<A> callback) {
		// TODO Auto-generated method stub
	}
	
	private final void failTo(final ClientJobSubmission<?> jobSub) {
		if(jobSub==null) return;
		
		assert this.jobQueue.contains(jobSub);
		
		ClientJobSubmission<?> failMe;
		do {
			failMe=this.jobQueue.poll();
			assert failMe!=null;
			
			failMe.fail(new IOException("failed to connect to any available job server"));
		} while(failMe!=jobSub);
	}
}
