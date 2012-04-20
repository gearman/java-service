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

package org.gearman.impl.server.local;

import org.gearman.GearmanJobPriority;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.util.ByteArray;

interface Job {

	/** An enumerator for defining the job's state */
	public static enum JobState {
		QUEUED, WORKING, COMPLETE
	};

	public GearmanPacket createStatusResPacket();
	
	/**
	 * Returns a JOB_ASSIGN packet as specified in the gearman protocol
	 * 
	 * @return a JOB_ASSIGN packet as specified in the gearman protocol
	 */
	public GearmanPacket createJobAssignPacket();

	/**
	 * Returns a JOB_ASSIGN_UNIQ packet as specified in the gearman protocol
	 * 
	 * @return a JOB_ASSIGN_UNIQ packet as specified in the gearman protocol
	 */
	public GearmanPacket createJobAssignUniqPacket();

	/**
	 * Returns a JobCreated packet as specified by the gearman protocol
	 * 
	 * @return a JOB_CREATED packet as specified by the gearman protocol
	 */
	public GearmanPacket createJobCreatedPacket();

	/**
	 * Returns a WORK_STATUS packet as specified by the gearman protocol
	 * 
	 * @return a WORK_STATUS packet as specified by the gearman protocol
	 */
	public GearmanPacket createWorkStatusPacket();

	/**
	 * Returns the opaque data that is given to the function as an argument
	 * 
	 * @return the opaque data that is given to the function as an argument
	 */
	public byte[] getData();

	/**
	 * Returns the function that manages this job
	 * 
	 * @return The functions that manages this job
	 */
	public Function getFunction();

	/**
	 * Returns the server assigned job id
	 * 
	 * @return the server assigned job id
	 */
	public ByteArray getJobHandle();

	/**
	 * Returns the priority of this job
	 * 
	 * @return the priority of this job
	 */
	public GearmanJobPriority getPriority();

	/**
	 * Returns the current state of this job
	 * 
	 * @return the current state of this job
	 */
	public JobState getState();

	/**
	 * Returns the function local ID specified by the user
	 * 
	 * @return the function local ID specified by the user
	 */
	public ByteArray getUniqueID();

	/**
	 * Tests if this is a background job or not
	 * 
	 * @return true if this job is a background job. false this is not a
	 *         background job
	 */
	public boolean isBackground();

	/**
	 * Sends a packet to listening clients if and only if they forward exception packets
	 * 
	 * @param packet
	 *            The exception packet to send out
	 */
	public void sendExceptionPacket(GearmanPacket packet);

	/**
	 * Sends a packet to all the listening clients asynchronously
	 * 
	 * @param packet
	 *            The packet to send out
	 */
	public void sendPacket(GearmanPacket packet);

	/**
	 * Sets the current status, and sends a WORK_STATUS packet to all listing
	 * packets.
	 * 
	 * The status byte[] must contain: - NULL byte terminated percent complete
	 * numerator - Percent complete denominator
	 * 
	 * @param status
	 *            The status array as specified in the description
	 */
	public void setStatus(byte[] numerator, byte[] denominator);

	/**
	 * Sets the current state to the COMPLETE state. When a job is in a COMPLETE
	 * state, the job is voided and removed from the owning function.
	 * 
	 * Note: The complete states are the final states
	 * 
	 * @param packet
	 *            The packet being sent to all the listening clients
	 */
	public void workComplete(GearmanPacket packet);
}
