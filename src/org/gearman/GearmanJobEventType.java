/*
 * Copyright (c) 2012, Isaiah van der Elst (isaiah.v@comcast.net)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

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
