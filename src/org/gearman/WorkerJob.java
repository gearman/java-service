package org.gearman;

import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanPacket.Magic;

/**
 * The {@link GearmanJob} implementation for the {@link GearmanFunction} object. It provides
 * the underlying implementation in sending data back to the client.
 * 
 * @author isaiah.v
 */
class WorkerJob extends GearmanJob {

	private final static byte[] DEFAULT_UID = new byte[]{0};
	
	protected WorkerJob(final String function,final byte[] jobData,final GearmanConnection<?> conn,final byte[] jobHandle) {
		super(function, jobData, DEFAULT_UID);
		super.setConnection(conn, jobHandle);
	}

	@Override
	public synchronized final void callbackData(final byte[] data) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		
		final GearmanConnection<?> conn = super.getConnection();
		final byte[] jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
		
		conn.sendPacket(GearmanPacket.createWORK_DATA(Magic.REQ, jobHandle, data), null ,null /*TODO*/);
	}
	
	@Override
	public synchronized final void callbackWarning(final byte[] warning) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		
		final GearmanConnection<?> conn = super.getConnection();
		final byte[] jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
		
		conn.sendPacket(GearmanPacket.createWORK_WARNING(Magic.REQ, jobHandle, warning), null ,null /*TODO*/);
	}
	
	@Override
	public synchronized final void callbackException(final byte[] exception) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		
		final GearmanConnection<?> conn = super.getConnection();
		final byte[] jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
		
		conn.sendPacket(GearmanPacket.createWORK_EXCEPTION(Magic.REQ, jobHandle, exception),null ,null /*TODO*/);
	}
	
	@Override
	public void status(long numerator, long denominator) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		
		final GearmanConnection<?> conn = super.getConnection();
		final byte[] jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
				
		conn.sendPacket(GearmanPacket.createWORK_STATUS(Magic.REQ, jobHandle, numerator, denominator), null,null /*TODO*/);
	}
	
	@Override
	protected synchronized void onComplete(GearmanJobResult result) {
		final GearmanConnection<?> conn = super.getConnection();
		final byte[] jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
		
		if(result.isSuccessful()) {
			conn.sendPacket(GearmanPacket.createWORK_COMPLETE(Magic.REQ, jobHandle, result.getResultData()),null,null /*TODO*/);
		} else {
			conn.sendPacket(GearmanPacket.createWORK_FAIL(Magic.REQ, jobHandle),null ,null /*TODO*/);
		}
	}
}
