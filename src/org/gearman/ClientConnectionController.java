package org.gearman;

import java.util.concurrent.ConcurrentHashMap;

import org.gearman.JobServerPoolAbstract.ConnectionController;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanVariables;
import org.gearman.util.ByteArray;

abstract class ClientConnectionController <K> extends ConnectionController<K> {
		
	/**
	 * The set of executing jobs. The key is the job's handle and the value is the job itself
	 */
	private final ConcurrentHashMap<ByteArray, GearmanJob> jobs = new ConcurrentHashMap<ByteArray, GearmanJob>();
	private ClientJobSubmission<?> pendingJob = null;
	
	ClientConnectionController(final ClientImpl client, final K key) {
		super(client, key);
	}
	
	protected final ClientJobSubmission<?> getPendingJob() {
		return this.pendingJob;
	}
	
	protected final void setPendingJob(final ClientJobSubmission<?> pendingJob) {
		this.pendingJob = pendingJob;
	}
	
	protected abstract void grab();
	
	protected abstract ClientJobSubmission<?> grabJob();
		
	@Override
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
		switch (packet.getPacketType()) {
		case JOB_CREATED:
			jobCreated(packet);
			break;
		case WORK_STATUS:
			workStatus(packet);
			break;
		case WORK_COMPLETE:
			workComplete(packet);
			break;
		case WORK_FAIL:
			workFail(packet);
			break;
		case ECHO_RES:	// Not used
			assert false;
			break;
		case ERROR:
			error(packet);
			break;
		case STATUS_RES:
			//TODO
			break;
		case WORK_EXCEPTION:
			workException(packet);
			break;
		case OPTION_RES:
			//TODO
			break;
		case WORK_DATA:
			workData(packet);
			break;
		case WORK_WARNING:
			workWarning(packet);
			break;
		default: // Not a client response
			assert false;
		}
	}
	
	private final void workWarning(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final GearmanJob job = this.jobs.get(jobHandle);
		
		if(job==null) {
			// TODO log warning
			return;
		}
		
		final byte[] warning = packet.getArgumentData(1);
		try {
			job.callbackWarning(warning);
		} catch (Throwable t) {
			// If the user throws an exception, catch it, print it, and continue.
			t.printStackTrace();
		}
	}
	
	private final void workData(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final GearmanJob job = this.jobs.get(jobHandle);
		
		if(job==null) {
			// TODO log warning
			return;
		}
		
		final byte[] data= packet.getArgumentData(1);
		try {
			job.callbackData(data);
		} catch (Throwable t) {
			// If the user throws an exception, catch it, print it, and continue.
			t.printStackTrace();
		}
	}
	
	private final void workException(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final GearmanJob job = this.jobs.get(jobHandle);
		
		if(job==null) {
			// TODO log warning
			return;
		}
		
		final byte[] exception = packet.getArgumentData(1);
		try {
			job.callbackException(exception);
		} catch (Throwable t) {
			// If the user throws an exception, catch it, print it, and continue.
			t.printStackTrace();
		}
	}
	
	private final void workFail(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final GearmanJob job = this.jobs.get(jobHandle);
		
		if(job==null) {
			// TODO log warning
			return;
		}
		
		try {
			job.onComplete(GearmanJobResult.WORKER_FAIL);
		} catch (Throwable t) {
			// If the user throws an exception, catch it, print it, and continue.
			t.printStackTrace();
		}
	}
	
	private final void jobCreated(final GearmanPacket packet) {
		final ClientJobSubmission<?> jobSub;
		
		synchronized(this.jobs) {
			assert this.pendingJob!=null;
			
			jobSub = this.pendingJob;
			assert jobSub!=null;
			assert jobSub.job!=null;
			
			this.pendingJob = null;
		}
		
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		this.jobs.put(jobHandle, jobSub.job);
		
		this.grab();
	}
	
	private final void workStatus(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final GearmanJob job = this.jobs.get(jobHandle);
		
		if(job==null) {
			// TODO log warning
			return;
		}
		
		try {
			final long numerator = Long.parseLong(new String(packet.getArgumentData(1),GearmanVariables.UTF_8));
			final long denominator = Long.parseLong(new String(packet.getArgumentData(2),GearmanVariables.UTF_8));
			job.status(numerator, denominator);
		} catch (NumberFormatException nfe) {
			// TODO log error
		}
	}
	
	private final void workComplete(final GearmanPacket packet) {
		final ByteArray jobHandle = new ByteArray(packet.getArgumentData(0));
		final GearmanJob job = this.jobs.get(jobHandle);
		
		if(job==null) {
			//TODO log warning
			return;
		}
		
		final byte[] data = packet.getArgumentData(1);
		try {
			job.onComplete(GearmanJobResult.workSuccessful(data));
		} catch (Throwable t) {	
			// If the user throws an exception, catch it, print it, and continue.
			t.printStackTrace();
		}
	}
	
	private final void error(final GearmanPacket packet) {
		//TODO log error
	}
}
