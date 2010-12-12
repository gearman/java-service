/*
 * Copyright (C) 2010 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

import org.gearman.core.GearmanPacket;

interface ServerJob {

	/** An enumerator for defining the job's priority */
	public static enum JobPriority {
		LOW, MID, HIGH
	};

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
	public ServerFunction getFunction();

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
	public JobPriority getPriority();

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
