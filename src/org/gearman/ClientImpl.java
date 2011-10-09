package org.gearman;

import java.net.InetSocketAddress;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.gearman.GearmanLostConnectionPolicy.Action;
import org.gearman.GearmanLostConnectionPolicy.Grounds;
import org.gearman.core.GearmanCallbackResult;

class ClientImpl extends JobServerPoolAbstract<ClientImpl.InnerConnectionController<?,?>> implements GearmanClient {

	
	protected class InnerConnectionController<K, C extends GearmanCallbackResult> extends ClientConnectionController<K,C> {
		
		InnerConnectionController(K key) {
			super(ClientImpl.this, key, logger);
		}
		
		@Override
		protected Gearman getGearman() {
			return ClientImpl.this.gearman;
		}
		
		@Override
		protected ClientJobSubmission pollNextJob() {
			return ClientImpl.this.pollJob();
		}
		
		@Override
		protected void requeueJob(ClientJobSubmission jobSub) {
			/*
			 * Note: this method may me called by super.close()
			 */
			
			ClientImpl.this.requeueJob(jobSub);
		}
		
		@Override
		protected void onConnect(ControllerState oldState) {
		}
		
		@Override
		protected void onLostConnection(GearmanLostConnectionPolicy policy, Grounds grounds) {
			super.close();
		}

		@Override
		protected void onNew() {
			ClientImpl.this.addController(this);
		}

		@Override
		protected void onOpen(ControllerState oldState) {
			ClientImpl.this.onConnectionOpen(this);
		}
		
		@Override
		protected void onDrop(ControllerState oldState) {
			super.close();
			ClientImpl.this.dropController(this, oldState);
		}
		
		@Override
		protected void onWait(ControllerState oldState) {
			super.close();
		}
		
		@Override
		protected void onClose(ControllerState oldState) {
			super.close();
			if(oldState.equals(ControllerState.OPEN))
				ClientImpl.this.onClose(this);
		}
	}
	
	private final class LocalConnectionController extends InnerConnectionController<GearmanServer, org.gearman.GearmanServer.ConnectCallbackResult> {
		
		LocalConnectionController(GearmanServer key) {
			super(key);
		}
		
		@Override
		protected void onLostConnection(GearmanLostConnectionPolicy policy, Grounds grounds) {
			super.onLostConnection(policy, grounds);
			
			/*
			 * An unexpected disconnect from a local server can only be due to it being shutdown.
			 * Since a local server cannot be re-opened, it must be removed
			 */
			
			// notify the user of the connection being dropped
			policy.lostLocalServer(this.getKey(), ClientImpl.this, grounds);
			
			// remove "this" from the client
			super.dropServer();
		}
		
		@Override
		protected void onConnect(ControllerState oldState) {
			super.onConnect(oldState);
			
			/*
			 * Open a new local connection. This connection controller is both
			 * the GearmanConnectionHandler and GearmanFailureHandler
			 */
			super.getKey().createGearmanConnection(this, this);
		}
	}
	
	private final class RemoteConnectionController extends InnerConnectionController<InetSocketAddress, org.gearman.core.GearmanConnectionManager.ConnectCallbackResult> implements Runnable{
		RemoteConnectionController(InetSocketAddress key) {
			super(key);
		}
		
		@Override
		protected void onLostConnection(GearmanLostConnectionPolicy policy, Grounds grounds) {
			super.onLostConnection(policy, grounds);
			
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
			} else {
				// reconnect
				
				switch(grounds) {
				case UNEXPECTED_DISCONNECT:
				case RESPONCE_TIMEOUT:
					ClientImpl.this.removeFromOpen(this);
					break;
				case FAILED_CONNECTION:
					ClientImpl.this.onFailedConnection(this);
					break;
				default:
					assert false;
				}
				
				
				if(action.equals(Action.RECONNECT)) {
					// Default reconnect
					
					this.run();
				} else {
					// User defined
					
					if(action.getNanoTime()==0) {
						this.run();
					} else {
						super.waitServer(this, action.getNanoTime(), TimeUnit.NANOSECONDS);
					}
				}
			}
		}
		
		@Override
		protected void onConnect(ControllerState oldState) {
			super.onConnect(oldState);
			
			/*
			 * Open a new remote connection. This connection controller is both
			 * the GearmanConnectionHandler and GearmanFailureHandler
			 */
			ClientImpl.this.gearman.getGearmanConnectionManager().createGearmanConnection(super.getKey(), this, this);
		}

		@Override
		public void run() {
			ClientImpl.this.addController(this);
		}
	}
	
	/** The gearman provider */
	private final Gearman gearman;
	
	/** The set of open connections */ 
	private final Queue<InnerConnectionController<?,?>> open = new LinkedBlockingQueue<InnerConnectionController<?,?>>();
	
	/** The set of available connections*/
	private final ClientConnectionList<InnerConnectionController<?,?>, ClientJobSubmission> available = new ClientConnectionList<InnerConnectionController<?,?>, ClientJobSubmission>();
	
	/** The set of jobs waiting to be submitted */
	private final Deque<ClientJobSubmission> jobQueue = new LinkedBlockingDeque<ClientJobSubmission>();
	
	private final GearmanLogger logger;
	
	ClientImpl(final Gearman gearman) {
		
		// Initialize the GearmanJobServerPool
		super(new ClientLostConnectionPolicy(), 0, TimeUnit.NANOSECONDS);
		
		// Set the gearman provider
		this.gearman = gearman;
		
		// Set the default loggerID
		logger = GearmanLogger.createGearmanLogger(gearman, this);
	}

	@Override
	protected InnerConnectionController<?,?> createController(GearmanServer key) {
		return new LocalConnectionController(key);
	}

	@Override
	protected InnerConnectionController<?,?> createController(InetSocketAddress key) {
		return new RemoteConnectionController(key);
	}

	@Override
	public void submitJob(GearmanJob job, GearmanSubmitHandler callback) {
		if(this.isShutdown()) {
			if(callback!=null)
				callback.onComplete(job,SubmitCallbackResult.FAILED_TO_SHUTDOWN);
			return;
		} else if(!job.submit()) {
			if(callback!=null) 
				callback.onComplete(job,SubmitCallbackResult.FAILED_TO_INVALID_JOB_STATE);
			return;
		} else if (super.getServerCount()==0) {
			if(callback!=null)
				callback.onComplete(job, SubmitCallbackResult.FAILED_TO_NO_SERVER);
			return;
		}
		
		this.addJob(new ClientJobSubmission(job, callback, job instanceof GearmanBackgroundJob));
	}

	@Override
	public Gearman getGearman() {
		return this.gearman;
	}
	
	private final void addJob(ClientJobSubmission job) {
	
		InnerConnectionController<?,?> conn = null;
		
		synchronized(this.open) {
			
			if(!this.open.isEmpty()) {
				this.jobQueue.addLast(job);
				
				for(InnerConnectionController<?,?> icc : this.open) {
					if(icc.grab()) return;
				}
				
				final InnerConnectionController<?,?> icc;
				if ((icc = this.available.tryFirst(null))!=null){
					// Make a connection
					conn = icc;
				}
				
			} else {
				
				final InnerConnectionController<?,?> icc;
				if ((icc = this.available.tryFirst(job))!=null){
					// Add job to job queue
					this.jobQueue.addLast(job);
					
					// Make a connection
					conn = icc;
				} else {
					// No available servers to connect to, fail job
					job.onSubmissionComplete(GearmanClient.SubmitCallbackResult.FAILED_TO_NO_SERVER);
				}
			}
		}
		
		if(conn!=null) conn.openServer(false);
	}
	
	private final void onConnectionOpen(final InnerConnectionController<?,?> icc) {
		synchronized(this.open) {
			if(this.open.isEmpty())
				this.available.clearFailKeys();
			
			assert this.available.contains(icc);
			
			Object t1;
			t1 = this.available.remove(icc);
			assert t1==null && !this.available.contains(icc);
			
			this.open.add(icc);
			
			icc.grab();
		}
	}
	
	private final void addController(final InnerConnectionController<?,?> icc) {
		synchronized(this.open) {
			assert (icc.getState().equals(ControllerState.CLOSED)
				|| icc.getState().equals(ControllerState.CONNECTING));
			
			this.available.add(icc);
			assert this.available.contains(icc);
		}
	}
	
	private final void dropController(final InnerConnectionController<?,?> icc, final ControllerState oldState) {
		synchronized(this.open) {
			assert icc.getState().equals(ControllerState.DROPPED);
			
			switch(oldState) {
			case CONNECTING:
			case CLOSED:
				assert this.available.contains(icc);
				assert !this.open.contains(icc);
				
				final ClientJobSubmission job = this.available.remove(icc);
				if(job!=null) {
					// There should be no fail keys while there are open connections
					assert this.open.isEmpty();
					this.failTo(job, SubmitCallbackResult.FAILED_TO_CONNECT);
				}
				
				break;
			case OPEN:
				assert this.open.contains(icc);
				assert !this.available.contains(icc);
				
				boolean t = this.open.remove(icc);
				assert t;
				
				break;
			case WAITING:
				assert !this.open.contains(icc);
				assert !this.available.contains(icc);
				break;
			case DROPPED:
				assert false;
				break;
			}
		}
	}
	
	/**
	 * Call when there is an expected disconnect.
	 * @param icc
	 */
	private final void onClose(final InnerConnectionController<?,?> icc) {
		
		/*
		 * Move the connection controller from the open set to the available set
		 */
		
		InnerConnectionController<?,?> openNext = null;
		
		synchronized(this.open) {
			
			// The controller should be in the open set
			assert this.open.contains(icc);
			
			// remove the controller from the open set
			boolean test;
			test = this.open.remove(icc);
			assert test;
			
			/*
			 * If if the set of open connections is empty and there are still jobs in the
			 * queue, attempt to make a connection
			 */
			if(this.open.isEmpty()) {
				/*
				 * Note: if the disconnect causes jobs to be added to the job queue,
				 * it should be added before this method is called 
				 */
				
				// Grab the last job added to the job queue, if one exits 
				final ClientJobSubmission job = this.jobQueue.peekLast();
				if(job!=null) {
					// If there are jobs in the jobQueue, make a new connection
					
					// try to make a new connection and set the fail key
					final InnerConnectionController<?,?> conn = this.available.tryFirst(job);
					
					if(conn!=null) {
						assert conn.getState().equals(ControllerState.CLOSED)
							|| conn.getState().equals(ControllerState.CONNECTING);
						
						openNext = conn;
						
					} else {
						// If conn is null, then there are no other available connections
						this.failTo(job, SubmitCallbackResult.FAILED_TO_NO_SERVER);
					}
				}
			}
			
			/*
			 * since this is an expected disconnect, we know this is a good job server to
			 * connect to. Add it to the head of the list
			 */
			test= this.available.addFirst(icc);
			assert test;
		}
		
		if(openNext!=null) {
			boolean test = openNext.openServer(false);
			assert test;
		}
	}
	
	private final void onFailedConnection(final InnerConnectionController<?,?> icc) {
		synchronized(this.open) {
			assert this.available.contains(icc);
			final ClientJobSubmission cjs = this.available.remove(icc);
			assert !this.available.contains(icc);
			
			if(cjs!=null) {
				// There should be no fail keys while there are open connections
				assert this.open.isEmpty();
				this.failTo(cjs, SubmitCallbackResult.FAILED_TO_CONNECT);
			}
			
			this.available.add(icc);
			assert this.available.contains(icc);
		}
	}
	
	private final void failTo(final ClientJobSubmission job, SubmitCallbackResult result) {
		
		synchronized(this.open) {
			/*
			 * Note: this operation needs to be synchronized. This will prevent
			 * a connection form being established and polling the job that's
			 * being failed.
			 */
			
			assert this.open.isEmpty();
			assert this.jobQueue.contains(job);
			
			ClientJobSubmission current;
			do {
				current=this.jobQueue.pollFirst();
				current.onSubmissionComplete(result);
			} while(current!=job);
		}
	}
	
	private final ClientJobSubmission pollJob() {
		return this.jobQueue.poll();
	}
	
	private final void requeueJob(ClientJobSubmission job) {
		this.jobQueue.addFirst(job);
	}
	
	private final void removeFromOpen(final InnerConnectionController<?,?> icc) {
		synchronized(this.open) {
			assert icc.getState().equals(ControllerState.CLOSED);
			assert this.open.contains(icc);
			
			this.open.remove(icc);
		}
	}
	
	@Override
	public final void shutdown() {
		synchronized(this.open) {
			for(ClientJobSubmission jobSub : this.jobQueue){
				jobSub.onSubmissionComplete(SubmitCallbackResult.FAILED_TO_SHUTDOWN);
			}
			this.open.clear();
			this.available.clear();
			
			super.shutdown();
			gearman.onServiceShutdown(this);
		}
	}

	@Override
	public void setLoggerID(String loggerId) {
		this.logger.setLoggerID(loggerId);
	}
	
	@Override
	public String getLoggerID() {
		return this.logger.getLoggerID();
	}
}
