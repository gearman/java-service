package org.gearman;

import org.gearman.JobServerPoolAbstract.ConnectionController;
import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanPacket.Magic;
import org.gearman.util.ByteArray;

/**
 * The {@link GearmanJob} implementation for the {@link GearmanFunction} object. It provides
 * the underlying implementation in sending data back to the client.
 * 
 * @author isaiah.v
 */
class WorkerJob extends GearmanJob {

	private final static byte[] DEFAULT_UID = new byte[]{0};
	
	protected WorkerJob(final String function,final byte[] jobData,final ConnectionController<?,?> conn,final ByteArray jobHandle) {
		super(function, jobData, DEFAULT_UID);
		super.setConnection(conn, jobHandle);
	}

	@Override
	public synchronized final void callbackData(final byte[] data) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		
		final GearmanConnection<?> conn = super.getConnection();
		final ByteArray jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
		
		conn.sendPacket(GearmanPacket.createWORK_DATA(Magic.REQ, jobHandle.getBytes(), data), null /*TODO*/);
	}
	
	@Override
	public synchronized final void callbackWarning(final byte[] warning) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		
		final GearmanConnection<?> conn = super.getConnection();
		final ByteArray jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
		
		conn.sendPacket(GearmanPacket.createWORK_WARNING(Magic.REQ, jobHandle.getBytes(), warning), null /*TODO*/);
	}
	
/*
	@Override
	public synchronized final void callbackException(final byte[] exception) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		
		final GearmanConnection<?> conn = super.getConnection();
		final byte[] jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
		
		conn.sendPacket(GearmanPacket.createWORK_EXCEPTION(Magic.REQ, jobHandle, exception),null ,null); //TODO
	}
*/
	
	@Override
	public void callbackStatus(long numerator, long denominator) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		
		final GearmanConnection<?> conn = super.getConnection();
		final ByteArray jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
				
		conn.sendPacket(GearmanPacket.createWORK_STATUS(Magic.REQ, jobHandle.getBytes(), numerator, denominator), null /*TODO*/);
	}
	
	@Override
	protected synchronized void onComplete(GearmanJobResult result) {
		final GearmanConnection<?> conn = super.getConnection();
		final ByteArray jobHandle = super.getJobHandle();
		
		assert conn!=null;
		assert jobHandle!=null;
		
		if(result.isSuccessful()) {
			conn.sendPacket(GearmanPacket.createWORK_COMPLETE(Magic.REQ, jobHandle.getBytes(), result.getResultData()),null /*TODO*/);
		} else {
			conn.sendPacket(GearmanPacket.createWORK_FAIL(Magic.REQ, jobHandle.getBytes()),null /*TODO*/);
		}
	}
}
