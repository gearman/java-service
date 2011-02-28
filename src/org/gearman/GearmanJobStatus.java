package org.gearman;

import org.gearman.core.GearmanCallbackResult;

public interface GearmanJobStatus {
	public static enum StatusCallbackResult implements GearmanCallbackResult {
		SUCCESS,
		SERVER_NOT_AVAILABLE,
		SERVER_DROPPED,
		CONNECTION_FAILED,
		SERVER_DISCONNECTED,
		WORK_COMPLETE,
		SEND_FAILED;

		@Override
		public boolean isSuccessful() {
			return this.equals(SUCCESS);
		}
	}

	public interface StatusResult extends GearmanCallbackResult {		
		public GearmanJobStatus getGearmanJobStatus();
		public StatusCallbackResult getStatusCallbackResult();
	}
	
	/**
	 * Tests if the server knew the status of the job in question.
	 * 
	 * Most job servers will return unknown if it never received the job or
	 * if the job has already been completed.
	 * 
	 * If the status is known but not running, then a worker has not yet
	 * polled the job
	 * 
	 * @return
	 * 		
	 */
	public boolean isKnown();
	
	/**
	 * Tests if the job is currently running.
	 * 
	 * If the status is unknown, this value will always be false.
	 *  
	 * @return
	 * 		<code>true</code> if the job is currently being worked on. <code>
	 * 		false</code> otherwise.
	 */
	public boolean isRunning();
	
	/**
	 * The percent complete numerator.
	 * @return
	 */
	public long getNumerator();
	
	/**
	 * The percent complete denominator.
	 * @return
	 */
	public long getDenominator();
}
