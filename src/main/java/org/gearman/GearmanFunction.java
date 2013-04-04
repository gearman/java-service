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
