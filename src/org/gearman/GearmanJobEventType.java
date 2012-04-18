package org.gearman;

import java.io.Serializable;

/**
 * An enumerator defining the job event type
 * @author isaiah
 */
public enum GearmanJobEventType implements Serializable {
	
	/**
	 * Intermediate data received in the normal data channel.<br>
	 * <br>
	 * <b>Return Type:</b>intermediate data
	 */
	GEARMAN_JOB_DATA,
	
	/**
	 * Intermediate data received in the warning channel.<br>
	 * <br>
	 * <b>Return Type:</b>intermediate warning data
	 */
	GEARMAN_JOB_WARNING,
	
	/**
	 * A job status update<br>
	 * <br>
	 * <b>Return Type:</b>a NULL byte terminated numerator followed by the denominator
	 */
	GEARMAN_JOB_STATUS,
	
	/**
	 * The job's execution was successful<br>
	 * <br>
	 * <b>Return Type:</b> Job's result data
	 */
	GEARMAN_JOB_SUCCESS,
	
	/**
	 * The job's execution failed because the worker failed<br>
	 * <br>
	 * <b>Return Type:</b> None
	 */
	GEARMAN_JOB_FAIL,
	
	/**
	 * The job submission failed because there are no available job servers to submit to<br>
	 * <br>
	 * <b>Return Type:</b> None
	 */
	GEARMAN_SUBMIT_FAIL,
	
	/**
	 * The job was successfully submitted to the job server<br>
	 * <br>
	 * <b>Return Value:</b> The job handle
	 */
	GEARMAN_SUBMIT_SUCCESS,
	
	/** 
	 * The <i>End-Of-File</i> has been reached and no more data will be returned for this job.<br>
	 * <br>
	 * <b>Return Value:</b> None
	 */
	GEARMAN_EOF;
}
