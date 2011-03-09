package org.gearman;

import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanCallbackResult;

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
	public static enum SubmitCallbackResult implements GearmanCallbackResult{
		
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
		
		@Override
		public boolean isSuccessful() {
			return this==SUBMIT_SUCCESSFUL;
		}
		
	}
	
	public interface GearmanSubmitHandler extends GearmanCallbackHandler<GearmanJob, SubmitCallbackResult>{}
	
	public void submitJob(GearmanJob job, GearmanSubmitHandler callback);
}
