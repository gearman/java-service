package org.gearman;

/**
 * A GearmanCliet submits {@link GearmanJob}s and {@link GearmanBackgroundJob}s
 * to gearman job servers
 * 
 * @author isaiah.v
 */
public interface GearmanClient extends GearmanJobServerPool {
	
	/**
	 * 
	 * @author isaiah
	 *
	 */
	public static enum SubmitResult {
		
		/**
		 * The job was sucessfuly submitted to a job server 
		 */
		SUBMIT_SUCCESSFUL,
		
		/**
		 * Failed to send the job due to there being no job servers to
		 * submit to.
		 */
		FAILED_TO_NO_SERVER,
		
		/**
		 * 
		 */
		FAILED_TO_CONNECT,
		
		/**
		 * The job has already been submitted to a client and has not yet completed 
		 */
		FAILED_TO_INVALID_JOB_STATE,
		
		/**
		 * The {@link GearmanClient} is shutdown
		 */
		FAILED_TO_SHUTDOWN;
				
		public boolean isSuccessful() {
			return this==SUBMIT_SUCCESSFUL;
		}
		
	}
	
	public static interface SubmitHandler {
		public void onSubmissionComplete(GearmanJob job, SubmitResult result);
	}
	
	public void submitJob(GearmanJob job, SubmitHandler callback);
}
