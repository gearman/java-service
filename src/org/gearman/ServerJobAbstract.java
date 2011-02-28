package org.gearman;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanConstants;
import org.gearman.core.GearmanConnection.SendCallbackResult;
import org.gearman.core.GearmanPacket.Magic;
import org.gearman.core.GearmanPacket.Type;
import org.gearman.util.ByteArray;

abstract class ServerJobAbstract implements ServerJob, ServerClientDisconnectListener {
	
	private static final byte[] STATUS_TRUE = new byte[]{'1'};
	private static final byte[] STATUS_FALSE = new byte[]{'0'};
	
	
	/** 
	 * Global Job Map
	 * key = job handle
	 * value = job
	 */
	private static final Map<ByteArray, ServerJob > globalJobs = new ConcurrentHashMap<ByteArray, ServerJob>();
	
	public static final ServerJob getJob(final ByteArray jobHandle) {
		return ServerJobAbstract.globalJobs.get(jobHandle);
	}
	
	/** Defines this job's current state */
	private JobState state = JobState.QUEUED;
	/** Defines this job's priority. Also used as the state change lock */
	private final JobPriority priority;
	/** Specifies if this is a background or not */
	private final boolean isBackground;
	
	// --- Job Data --- //
	
	/** The function local ID specified by the user */
	private final ByteArray uniqueID;
	/** The server wide ID specified by the server */
	private final ByteArray jobHandle;
	/** The opaque data that is given as an argument in the SUBMIT_JOB packet*/
	private final byte[] data;
	/** The status numerator */
	private byte[] numerator;
	/** The status denominator */
	private byte[] denominator;
	
	//--- Listening Clients and Worker --- //
	
	/** The set of all listing clients */
	private final Set<ServerClient> clients = new CopyOnWriteArraySet<ServerClient>();
	/** The worker assigned to work on this job */
	private ServerClient worker;
	
	ServerJobAbstract(final ByteArray uniqueID, final byte[] data, final JobPriority priority, final ServerClient creator) {
		this.uniqueID = uniqueID;
		this.data = data;
		this.priority = priority;
		
		if(!(this.isBackground = creator==null)) {
			this.clients.add(creator);
			creator.addDisconnectListener(this);
		}
		
		this.jobHandle = new ByteArray(getNextJobHandle());
		
		ServerJobAbstract.globalJobs.put(this.jobHandle, this);
	}
	
	protected final boolean addClient(final ServerClient client) {
		if(this.clients.add(client)) {
			client.addDisconnectListener(this);
			return true;
		}
		
		return false;
	}

	@Override
	public final GearmanPacket createJobAssignPacket() {
		return GearmanPacket.createJOB_ASSIGN(jobHandle.getBytes(), this.getFunction().getName().toString(GearmanConstants.UTF_8), data);
	}

	@Override
	public final GearmanPacket createJobAssignUniqPacket() {
		return new GearmanPacket(Magic.RES, Type.JOB_ASSIGN_UNIQ, this.jobHandle.getBytes(), this.getFunction().getName().getBytes(), this.uniqueID.getBytes(), data);
	}

	@Override
	public final GearmanPacket createJobCreatedPacket() {
		return new GearmanPacket(Magic.RES, Type.JOB_CREATED, this.jobHandle.getBytes());
	}

	@Override
	public final GearmanPacket createWorkStatusPacket() {
		return new GearmanPacket(Magic.RES, Type.WORK_STATUS,this.jobHandle.getBytes(), numerator, denominator);
	}
	
	@Override
	public final GearmanPacket createStatusResPacket() {
		final byte[] isRunning = this.state.equals(JobState.WORKING)? STATUS_TRUE: STATUS_FALSE;
		return new GearmanPacket(Magic.RES, Type.STATUS_RES,this.jobHandle.getBytes(),STATUS_TRUE,isRunning,numerator==null?STATUS_FALSE:numerator, denominator==null?STATUS_FALSE:denominator);
	}


	@Override
	public byte[] getData() {
		return this.data;
	}

	@Override
	public ByteArray getJobHandle() {
		return this.jobHandle;
	}

	@Override
	public JobPriority getPriority() {
		return this.priority;
	}

	@Override
	public JobState getState() {
		return this.state;
	}

	@Override
	public ByteArray getUniqueID() {
		return this.uniqueID;
	}

	@Override
	public boolean isBackground() {
		return this.isBackground;
	}

	@Override
	public void sendExceptionPacket(GearmanPacket packet) {
		assert packet!=null;
		
		for(ServerClient client : this.clients) {
			client.sendExceptionPacket(packet,null/*TODO*/);
		}
	}

	@Override
	public void sendPacket(GearmanPacket packet) {
		assert packet!=null;
		
		for(ServerClient client : this.clients) {
			client.sendPacket(packet, null/*TODO*/);
		}
	}

	@Override
	public void setStatus(byte[] numerator, byte[] denominator) {
		this.numerator = numerator;
		this.denominator = denominator;
	}

	@Override
	public void workComplete(GearmanPacket packet) {
		assert packet.getPacketType().equals(GearmanPacket.Type.WORK_COMPLETE) || packet.getPacketType().equals(GearmanPacket.Type.WORK_FAIL);
		packet.setMagic(Magic.RES);
		
		final JobState prevState = this.state;
		this.state = JobState.COMPLETE;
		
		this.onComplete(prevState);
		ServerJobAbstract.globalJobs.remove(this.jobHandle);
		
		for(ServerClient client : this.clients) {
			boolean t = client.removeDisconnectListener(this);
			assert t;
			client.sendPacket(packet,null /*TODO*/);
		}
		this.clients.clear();
		
		if(this.worker!=null) {
			boolean t = this.worker.removeDisconnectListener(this);
			assert t;
			this.worker = null;
		}
	}
	
	@Override
	public final void onDisconnect(final ServerClient client) {
		
		switch (this.state) {
		case QUEUED:
			assert this.worker==null;
			this.clients.remove(client);
			// If the job was in the QUEUED state, all attached clients have disconnected, and it is not a background job, drop the job
			if(this.clients.isEmpty() && !this.isBackground)
				complete();
			break;
		case WORKING:
			this.clients.remove(client);
			if(this.worker==client) {
				this.worker = null;
				
				if(this.clients.isEmpty() && !this.isBackground) {
					complete();
				} else {
					// (!this.clients.isEmpty() || this.isBackground)==true
					queue();
				}
			}

			/* 
			 * If the disconnecting client is not the worker, there is no need to change the state.
			 * Since there is no way to notify the worker that the job is no longer valid, we just
			 * let the worker finish execution 
			 */
			
			break;
		case COMPLETE:
			// Do nothing
		}
		if(client==this.worker) {
			this.worker = null;
		}
		this.clients.remove(client);
	}
	
	protected final void work(final ServerClient worker){
		assert this.state==JobState.QUEUED;
		this.worker = worker;
		this.state = JobState.WORKING;
		
		worker.addDisconnectListener(this);
		
		worker.sendPacket(this.createJobAssignPacket(), new GearmanCallbackHandler<GearmanPacket, org.gearman.core.GearmanConnection.SendCallbackResult>(){
			@Override
			public void onComplete(GearmanPacket data, SendCallbackResult result) {
				if(!result.isSuccessful()) 
					ServerJobAbstract.this.queue();
			}			 
		});
	}
	
	protected final void workUniqueID(final ServerClient worker) {
		assert this.state==JobState.QUEUED;
		this.worker = worker;
		this.state = JobState.WORKING;
		
		worker.addDisconnectListener(this);
		
		//TODO does not send UniqueID
		worker.sendPacket(this.createJobAssignPacket(), new GearmanCallbackHandler<GearmanPacket, org.gearman.core.GearmanConnection.SendCallbackResult>(){
			@Override
			public void onComplete(GearmanPacket data, SendCallbackResult result) {
				if(!result.isSuccessful()) 
					ServerJobAbstract.this.queue();
			}			 
		});
	}
	
	protected abstract void onComplete(JobState prevState);
	
	private final void complete() {
		final JobState prevState = this.state;
		this.state = JobState.COMPLETE;
		
		this.onComplete(prevState);
	}
	private final void queue() {
		final JobState prevState = this.state;
		this.state = JobState.QUEUED;
		
		this.onQueue(prevState);
	}
	
	protected abstract void onQueue(JobState prevState);
	
	/** The prefix for the job handle */
	private static final byte[] jobHandlePrefix = initJobHandle();
	/** The current job handle number */
	private static long jobHandleNumber = 1;
	
	/**
	 * Initializes the jobHandlePrefix variable
	 * @return
	 * 		The prefix for the job handle
	 */
	private static final byte[] initJobHandle() {
		byte[] user;
		try {
			user = java.net.InetAddress.getLocalHost().getHostName().getBytes(GearmanConstants.UTF_8);
			//user = System.getProperty("user.name").getBytes("UTF-8");
		} catch (UnknownHostException e) {
			assert false;
			return null;
		}
		
		final byte[] prefix = new byte[user.length + 3];
		prefix[0]='H';
		prefix[1]=':';
		System.arraycopy(user, 0, prefix, 2, user.length);
		prefix[user.length+2]=':';
		
		return prefix;
	}
	
	
	/**
	 * Returns the next available job handle
	 * @return
	 * 		the next available job handle
	 */
	private synchronized static final byte[] getNextJobHandle() {
		final byte[] jobNumber = Long.toString(jobHandleNumber).getBytes(GearmanConstants.UTF_8);
		
		final byte[] jobHandle = new byte[jobHandlePrefix.length+jobNumber.length];
		System.arraycopy(jobHandlePrefix, 0, jobHandle, 0, jobHandlePrefix.length);
		System.arraycopy(jobNumber, 0, jobHandle, jobHandlePrefix.length, jobNumber.length);
		
		jobHandleNumber++;
		
		return jobHandle;
	}
}
