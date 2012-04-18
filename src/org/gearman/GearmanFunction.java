/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

/**
 * A GearmanFunction provides the interface to create functions that can be executed
 * by the {@link GearmanWorker}.
 * 
 * @author isaiah
 */
public interface GearmanFunction {

	/**
	 * The working method of a gearman function. A GearmanFunction is registered
	 * with a {@link GearmanWorker} to let it know when a job for this function
	 * comes in, to execute this method.<br>
	 * <br>
	 * If a runtime exception is thrown while executing the function. The job
	 * fails. A fail event is sent back to the client and the exception message
	 * is logged on the local machine (worker size).<br>
	 * <br>
	 * If a null value is returned, it is assumed the execution was successful
	 * but no data is to be sent back to the client.<br>
	 * <br>
	 * Once this method has returned, the job is complete. Calling any method from
	 * the given {@link GearmanFunctionCallback} will result in an {@link IllegalStateException}
	 * @param function
	 * 		gearman function name
	 * @param data
	 * 		gearman job data
	 * @param callback
	 * 		An object used to send intermediate data back to the client while the job is executing
	 * @return
	 * 		The result data of the job's execution
	 * @throws Exception
	 * 		If the job's execution fails
	 */
	public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception;
}
