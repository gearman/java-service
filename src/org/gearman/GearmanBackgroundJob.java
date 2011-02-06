package org.gearman;

public final class GearmanBackgroundJob extends GearmanJob {

	public GearmanBackgroundJob(String function, byte[] jobData) {
		super(function, jobData);
	}
	
	public GearmanBackgroundJob(String function, byte[] jobData, Priority priority) {
		super(function, jobData, priority);
	}

	/**
	 * Background jobs are detached from the client. Once submitted to the 
	 */
	@Override
	public final void callbackData(byte[] data) {
	}

	@Override
	public void callbackStatus(long numerator, long denominator) {
		// TODO Auto-generated method stub
	}

	@Override
	public void callbackWarning(byte[] warning) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onComplete(GearmanJobResult result) {
		// TODO Auto-generated method stub
	}
}
