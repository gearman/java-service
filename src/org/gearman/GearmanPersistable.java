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
 * An object representing a persistable background job. It contains all of the
 * information used to execute a gearman job. Instances of this class is used by
 * the {@link GearmanPersistence} to persist background jobs.
 * 
 * @author isaiah
 */
public interface GearmanPersistable {

	/**
	 * Returns the function name
	 * 
	 * @return The function name
	 */
	public String getFunctionName();

	/**
	 * Returns the jobs data
	 * 
	 * @return The job data
	 */
	public byte[] getData();

	/**
	 * The job handle defined by the gearman server
	 * 
	 * @return The job handle
	 */
	public byte[] getJobHandle();

	/**
	 * The unique id defined by the client to identify jobs within a function
	 * 
	 * @return The unique id
	 */
	public byte[] getUniqueID();

	/**
	 * Returns the epoch time for when this job can execute. The given job will
	 * not be sent to a worker until after the epoch time has elapsed
	 * 
	 * @return The epoch time for when this job may run
	 */
	public long epochTime();

	/**
	 * Returns the job's priority level
	 * 
	 * @return The job's priority level
	 */
	public GearmanJobPriority getPriority();
}
