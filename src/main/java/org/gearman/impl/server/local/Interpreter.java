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

import java.util.Arrays;
import java.util.Collection;

import org.gearman.GearmanJobPriority;
import org.gearman.GearmanPersistable;
import org.gearman.GearmanPersistence;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanPacket.Magic;
import org.gearman.impl.core.GearmanPacket.Type;
import org.gearman.impl.util.ByteArray;
import org.gearman.impl.util.GearmanUtils;


/**
 * The Interpreter class defines how packets will be processed. This is in compliance
 * with the gearman protocol defined at http://gearman.org/index.php?id=protocol 
 * 
 * @author isaiah
 */
final class Interpreter {
	
	private final FunctionMap funcMap = new FunctionMap();
	private final GearmanPersistence persistence;
	
	private final GearmanServerLocal server;
	
	public Interpreter(GearmanServerLocal server, GearmanPersistence persistence) {
		this.server = server;
		this.persistence = persistence;
		
		if(persistence==null) return;
		
		Collection<GearmanPersistable> items = null;
		try {
			items = persistence.readAll();
		} catch (Exception ex) {
			// TODO LOG
		}
		
		if(items==null) return;
		for(GearmanPersistable item : items) {
			try {
				String functionName = item.getFunctionName();
				if(functionName==null) {
					// TODO log
				}
				
				Function func = funcMap.getFunction(new ByteArray(functionName));
				
				byte[] uniqueID = item.getUniqueID();
				byte[] data = item.getData();
				GearmanJobPriority priority = item.getPriority();
				byte[] jobHandle = item.getJobHandle();
				
				if(uniqueID==null || data==null || priority==null || jobHandle==null) {
					// TODO log
				} else {
					func.put(new ByteArray(uniqueID), data, jobHandle, priority);
				}
			} catch (Exception e) {
				// TODO log
			}
		}
	}
		
	/**
	 * Once a packet has been acquired from a client, it's processed here.
	 * The packet is processed in compliance with the gearman protocol.
	 *  
	 * @param packet	The packet sent by the client 
	 * @param client	The client associated with the client that sent
	 * 					the packet 
	 */
	public final void execute(final GearmanPacket packet, final Client client) {
		
		switch(packet.getPacketType()) {
		
		// Text Packets
		case TEXT:
			text_packet(packet,client);
			return;
		
		// Binary Packets
		case CAN_DO:
			can_do(packet,client);
			return;
		case CAN_DO_TIMEOUT:
			can_do(packet,client);
			return;
		case CANT_DO:
			cant_do(packet,client);
			return;
		case ECHO_REQ:
			echo_req(packet,client);
			return;
		case GET_STATUS:
			get_status(packet,client);
			return;
		case GRAB_JOB:
			grab_job(packet,client);
			return;
		case GRAB_JOB_UNIQ:
			grab_job_uniq(packet,client);
			return;
		case OPTION_REQ:
			option_req(packet,client);
			return;
		case PRE_SLEEP:
			pre_sleep(packet,client);
			return;
		case RESET_ABILITIES:
			reset_abilities(packet,client);
			return;
		case SET_CLIENT_ID:
			set_client_id(packet,client);
			return;
		case SUBMIT_JOB:
			submit_job(packet,client, GearmanJobPriority.NORMAL_PRIORITY, false);
			return;
		case SUBMIT_JOB_BG:
			submit_job(packet,client,GearmanJobPriority.NORMAL_PRIORITY, true);
			return;
		case SUBMIT_JOB_HIGH:
			submit_job(packet,client,GearmanJobPriority.HIGH_PRIORITY, false);
			return;
		case SUBMIT_JOB_HIGH_BG:
			submit_job(packet,client,GearmanJobPriority.HIGH_PRIORITY, true);
			return;
		case SUBMIT_JOB_LOW:
			submit_job(packet,client,GearmanJobPriority.LOW_PRIORITY, false);
			return;
		case SUBMIT_JOB_LOW_BG:
			submit_job(packet,client,GearmanJobPriority.LOW_PRIORITY, true);
			return;
		case WORK_COMPLETE:
			work_complete(packet,client);
			return;
		case WORK_DATA:
		case WORK_WARNING:
			work_data(packet,client);
			return;
		case WORK_EXCEPTION:
			work_exception(packet,client);
			return;
		case WORK_FAIL:
			work_fail(packet,client);
			return;
		case WORK_STATUS:
			work_status(packet,client);
			return;
		
		// Response Only Packets
		case NOOP:
		case JOB_CREATED:
		case NO_JOB:
		case ECHO_RES:
		case ERROR:
		case STATUS_RES:
		case OPTION_RES:
		case JOB_ASSIGN:
		case JOB_ASSIGN_UNIQ:
			
		// Packets Not Yet Implemented
		case ALL_YOURS:
		case SUBMIT_JOB_EPOCH:
		case SUBMIT_JOB_SCHED:
			client.sendPacket(StaticPackets.ERROR_BAD_COMMAND, null);
			return;
		
		// Unknown Command
		default:
			client.sendPacket(StaticPackets.ERROR_BAD_COMMAND, null);
			return;
		}
	}
	
	/**
	 * Called when a text-based packet is received.  When a packet with type -1 is received,
	 * it's assumed to be a text-based packet. The first argument (The command argument in a
	 * text-based packet) is checked against the set of known commands.  Then, the command
	 * is executed or an exception is returned to the client if the command is unknown.  
	 * 
	 * @param packet	The received packet with a type of -1
	 * @param client	The client who received the packet
	 */
	private final void text_packet(final GearmanPacket packet, final Client client) {
		final String pkt= new String(packet.toBytes(), GearmanUtils.getCharset());
		final String[] args = pkt.trim().split("[ \t]");
		
		String args0 = args[0];
		if(args0==null || args0.isEmpty())
			return;
		else
			args0 = args0.toUpperCase();
		
		switch(args0) {
		case "WORKERS":
			text_workers(args, client);
			return;
		case "STATUS":
			text_status(args, client);
			return;
		case "MAXQUEUE":
			text_maxqueue(args, client);
			return;
		case "SHUTDOWN":
			text_shutdown(args, client);
			return;
		case "VERSION":
			text_version(args, client);
			return;
		default:
			client.sendPacket(StaticPackets.TEXT_UNKNOWN_COMMAND, null);
		}
	}
	
	private final void text_workers(final String[] args, final Client client) {
		for(Client c : this.server.getClientSet())
			client.sendPacket(c.getStatus(), null);
		
		client.sendPacket(StaticPackets.TEXT_DONE, null);
	}
	
	private final void text_status(final String[] args, final Client client) {
		this.funcMap.sendStatus(client);
	}
	
	private final void text_maxqueue(final String[] args, final Client client) {
		final byte[] funcName = args[1].getBytes(GearmanUtils.getCharset());
		if(funcName==null) {
			client.sendPacket(StaticPackets.TEXT_INCOMPLETE_ARGS, null);
			return;
		}
		final ByteArray funcNameBA = new ByteArray(funcName);
		
		final String sizeStr = args[2];
		if(sizeStr==null) {
			client.sendPacket(StaticPackets.TEXT_INCOMPLETE_ARGS, null);
			return;
		}
		
		final int size;
		try { size = Integer.parseInt(sizeStr); }
		catch (NumberFormatException e) {
			client.sendPacket(StaticPackets.TEXT_OK, null);
			return;
		}
		
		final Function func = this.funcMap.getFunctionIfDefined(funcNameBA);
		if(func!=null) func.setMaxQueue(size);
	}
	
	private final void text_shutdown(final String[] args, final Client client) {
		if(client.getLocalPort()==-1) return; // Don't shutdown if local
		this.server.shutdown();
//		this.server.closePort(client.getLocalPort());
	}
	
	private final void text_version(final String[] args, final Client client) {
		client.sendPacket(StaticPackets.TEXT_VERSION, null);
	}
	
	/**
	 * Called when a CAN_DO packet comes in.<br>
	 * <br><i>
	 * CAN_DO:<br>
	 * This is sent to notify the server that the client is able to
	 * perform the given function. The client is then put on a list to be
	 * waken up whenever the job server receives a job for that function.<br>
	 * <br>
	 * Arguments:<br>
	 * - Function name.<br>
	 * </i>
	 * 
	 * @param packet
	 * 		The CAN_DO packet
	 * @param client
	 * 		The client who acquired the packet.
	 */
	private final void can_do(final GearmanPacket packet, final Client client) {
		
		// Note: Currently the CAN_DO_TIMEOUT maps to this method, the timeout
		// feature will be fully implemented in the future.
		
		//Function Name
		final byte[] funcName = packet.getArgumentData(0);
		assert funcName!=null;
		
		if(funcName.length==0) {
			//TODO send error
		}
		
		final ByteArray funcNameBA = new ByteArray(funcName);
		
		/*
		 * Get function. If the function is not
		 */
		final Function func = this.funcMap.getFunction(funcNameBA);
		client.can_do(func);
	}
	
	
	/**
	 * Called when a CANT_DO packet comes in.<br>
	 * <br> <i>
	 * This is sent to notify the server that the client is no longer able to
	 * perform the given function.<br>
	 * <br>
	 * Arguments:<br>
	 * - Function name.<br>
	 * </i>
	 * @param packet
	 * 		The CANT_DO packet 
	 * @param client
	 * 		The client who aquired the packet
	 */
	private final void cant_do(final GearmanPacket packet, final Client client) {		
		/*
		 * This is sent to notify the server that the client is no longer able to
		 * perform the given function.
		 * 
		 * Arguments:
		 * - Function name.
		 */
		
		// Function Name
		final byte[] funcName = packet.getArgumentData(0);
		assert funcName!=null;
		if(funcName.length==0) {
			//TODO send error
		}
		
		client.cant_do(new ByteArray(funcName));
	}
	
	private final void echo_req(final GearmanPacket packet, final Client client) {
		/*
		 * When a job server receives this request, it simply generates a
		 * ECHO_RES packet with the data. This is primarily used for testing
		 * or debugging.
		 * 
		 * Arguments:
		 * - Opaque data that is echoed back in response.
		 */
		
		packet.setMagic(Magic.RES);
		packet.setPacketType(GearmanPacket.Type.ECHO_RES);
		client.sendPacket(packet, null);
	}
	
	private final void get_status(final GearmanPacket packet, final Client client) {
		
		/*
		 * A client issues this to get status information for a submitted job.
		 * 
		 * Arguments:
		 *  - Job handle that was given in JOB_CREATED packet.
		 */
		
		final byte[] jobHandle = packet.getArgumentData(0);
		assert jobHandle != null;
		final ByteArray jobHandleBA = new ByteArray(jobHandle);
		
		final Job job = JobAbstract.getJob(jobHandleBA);
		if(job==null){
			//Send unknown job STATUS_RES packet 
			final byte[] unknown = new byte[]{'0'};
			final GearmanPacket status_res = new GearmanPacket(Magic.RES, Type.STATUS_RES, jobHandle, unknown, unknown,unknown,unknown);
			client.sendPacket(status_res, null);
			return;
		}
		
		final GearmanPacket status = job.createStatusResPacket();
		
		client.sendPacket(status, null);
	}
	
	private final void grab_job(final GearmanPacket packet, final Client client) {
		
		/* 
		 * This is sent to the server to request any available jobs on the
		 * queue. The server will respond with either NO_JOB or JOB_ASSIGN,
		 * depending on whether a job is available.
		 * 
		 * Arguments:
		 * - None.
		 */
		
		client.grabJob();
	}
	
	private final void grab_job_uniq(final GearmanPacket packet, final Client client) {
		/*
		 * Just like GRAB_JOB, but return JOB_ASSIGN_UNIQ when there is a job.
		 * 
		 * Arguments:
		 * - None.
		 */
		
		client.grabJobUniq();
	}
	private final void option_req(final GearmanPacket packet, final Client client) {
		/*
		 * A client issues this to set an option for the connection in the
		 * job server. Returns a OPTION_RES packet on success, or an ERROR
		 * packet on failure.
		 * 
		 * Arguments:
		 * - Name of the option to set. Possibilities are:
		 * 		"exceptions" - Forward WORK_EXCEPTION packets to the client.
		 */
		
		final byte[] option = packet.getArgumentData(0);
		assert option != null;
		
		final byte[] exceptions = new byte[] {'e','x','c','e','p','t','i','o','n','s'};
		
		if(Arrays.equals(option, exceptions)) {
			// exceptions option
			client.setForwardsExceptions(true);
			
			client.sendPacket(StaticPackets.OPTION_RES_EXCEPTIONS, null);
			
		} else {
			// unknown option
			client.sendPacket(StaticPackets.ERROR_UNKNOWN_OPTION, null);
		}
	}
	
	private final void pre_sleep(final GearmanPacket packet, final Client client) {	
		/*
		 * This is sent to notify the server that the client is about to
		 * sleep, and that it should be woken up with a NOOP packet if a
		 * job comes in for a function the client is able to perform.
		 * 
		 * Arguments:
		 * - None.
		 */
		
		// Places the client in sleep mode.  This will essentially tell the client to
		// send a NOOP packet when it is notified of a Job coming in
		client.sleep();
	}
	
	private final void reset_abilities(final GearmanPacket packet, final Client client) {
		/*
		 * This is sent to notify the server that the client is no longer
		 * able to do any functions it previously registered with CAN_DO or
		 * CAN_DO_TIMEOUT.
		 * 
		 * Arguments:
		 * - None.
	     */
		
		client.reset();
	}
	
	private final void set_client_id(final GearmanPacket packet, final Client client) {
		/*
		 * This sets the client ID in a job server so monitoring and reporting
		 * commands can uniquely identify the various clients, and different
		 * connections to job servers from the same client.
		 * 
		 * Arguments:
		 * - Unique string to identify the client instance.
		 */
		
		//Get Client ID from packet
		final byte[] clientIdBytes = packet.getArgumentData(0);
		assert clientIdBytes != null;
		
		//Convert the worker ID into a String
		final String clientId = new String(clientIdBytes,GearmanUtils.getCharset());
		
		//Set the client's client ID
		client.setClientId(clientId);
	}
	
	private final void submit_job(final GearmanPacket packet, final Client client, GearmanJobPriority priority, boolean isBackground) {
		
		/*
		 * A client issues this when a job needs to be run. The server will
		 * then assign a job handle and respond with a JOB_CREATED packet.
		 * 
		 * If on of the BG versions is used, the client is not updated with
		 * status or notified when the job has completed (it is detached).
		 * 
		 * The Gearman job server queue is implemented with three levels:
		 * normal, high, and low. Jobs submitted with one of the HIGH versions
		 * always take precedence, and jobs submitted with the normal versions
		 * take precedence over the LOW versions.
		 * 
		 * Arguments:
		 * - NULL byte terminated function name.
		 * - NULL byte terminated unique ID.
		 * - Opaque data that is given to the function as an argument.
		 */
		
		// Argument: function name
		final byte[] funcName = packet.getArgumentData(0);
		assert funcName != null;
		final ByteArray funcNameBA = new ByteArray(funcName); 
		
		// Argument: unique ID
		final byte[] uniqueID = packet.getArgumentData(1);
		assert uniqueID != null;
		final ByteArray uniqueIDBA = new ByteArray(uniqueID);
		
		// Argument: data
		final byte[] data = packet.getArgumentData(2);
		assert data!=null;
		
		final Function func = this.funcMap.getFunction(funcNameBA);
		func.createJob(uniqueIDBA, data, priority, client, isBackground, persistence);	
	}
	
	private final void work_complete(final GearmanPacket packet, final Client client) {
		/*
		 * This is to notify the server (and any listening clients) that
		 * the job completed successfully.
		 * 
		 * Arguments:
		 * - NULL byte terminated job handle.
		 * - Opaque data that is returned to the client as a response.
		 */
		
		final byte[] jobHandle = packet.getArgumentData(0);
		assert jobHandle != null;
		final ByteArray jobHandleBA = new ByteArray(jobHandle);
		
		final Job job = JobAbstract.getJob(jobHandleBA);
		if(job==null) {
			client.sendPacket(StaticPackets.ERROR_JOB_NOT_FOUND, null);
		} else {
			
			try {
				if(job.isBackground() && this.persistence!=null) {
					persistence.delete(new ServerPersistable(job));
				}
			} catch (Exception e) {
				// TODO log issue
			}
			
			synchronized(job) {
				// Construct a WORK_COMPLETE response packet
				
				// This operation must be synchronized. There is a race condition with
				// ServerFunction#createJob() method.
				job.workComplete(packet);
			}
		}
	}
	
	private final void work_data(final GearmanPacket packet, final Client client) {
		/*
		 * This is sent to update the client with data from a running job. A
		 * client should use this when it needs to send updates, send partial
		 * results, or flush data during long running jobs. It can also be
		 * used to break up a result so the client does not need to buffer
		 * the entire result before sending in a WORK_COMPLETE packet.
		 * 
		 * Arguments:
		 * - NULL byte terminated job handle.
		 * - Opaque data that is returned to the client.
		 */
		
		final byte[] jobHandle = packet.getArgumentData(0);
		assert jobHandle != null;
		final ByteArray jobHandleBA = new ByteArray(jobHandle);
		
		final Job job = JobAbstract.getJob(jobHandleBA);
		if(job==null) {
			client.sendPacket(StaticPackets.ERROR_JOB_NOT_FOUND, null);
		} else {
			packet.setMagic(Magic.RES);
			job.sendPacket(packet);
		}
	}

	private final void work_exception(final GearmanPacket packet, final Client client) {
		/*
		 * This is to notify the server (and any listening clients) that
		 * the job failed with the given exception.
		 * 
		 * Arguments:
		 * - NULL byte terminated job handle.
		 * - Opaque data that is returned to the client as an exception.
		 */
		
		// Note: The protocol states this packet notifies the server that the specified job
		// has failed. However, the C server does not fail the Job.  It is effectively a
		// WORK_WARNING packet that is only sent to clients that have specified they want
		// exceptions forwarded to them.  This server will do the same as long as the C
		// server does so.
		
		/*
		 * This is sent to update the client with data from a running job. A
		 * client should use this when it needs to send updates, send partial
		 * results, or flush data during long running jobs. It can also be
		 * used to break up a result so the client does not need to buffer
		 * the entire result before sending in a WORK_COMPLETE packet.
		 * 
		 * Arguments:
		 * - NULL byte terminated job handle.
		 * - Opaque data that is returned to the client.
		 */
		
		final byte[] jobHandle = packet.getArgumentData(0);;
		assert jobHandle != null;
		final ByteArray jobHandleBA = new ByteArray(jobHandle);
		
		final Job job = JobAbstract.getJob(jobHandleBA);
		if(job==null) {
			client.sendPacket(StaticPackets.ERROR_JOB_NOT_FOUND, null);
		} else {
			packet.setMagic(Magic.RES);
			job.sendPacket(packet);
		}
	}
	
	private final void work_fail(final GearmanPacket packet, final Client client) {
		
		/*
		 * This is to notify the server (and any listening clients) that
		 * the job failed.
		 * 
		 * Arguments:
		 * - Job handle.
		 */

		final byte[] jobHandle = packet.getArgumentData(0);
		assert jobHandle != null;
		final ByteArray jobHandleBA = new ByteArray(jobHandle);
		
		final Job job = JobAbstract.getJob(jobHandleBA);
		if(job==null){
			client.sendPacket(StaticPackets.ERROR_JOB_NOT_FOUND, null);
		} else {
			
			try {
				if(job.isBackground() && this.persistence!=null) {
					persistence.delete(new ServerPersistable(job));
				}
			} catch (Exception e) {
				// TODO log issue
			}
			
			job.workComplete(packet);
		}
	}
	
	private final void work_status(final GearmanPacket packet, final Client client) {
		/*
		 * This is sent to update the server (and any listening clients)
		 * of the status of a running job. The client should send these
		 * periodically for long running jobs to update the percentage
		 * complete. The job server should store this information so a client
		 * who issued a background command may retrieve it later with a
		 * GET_STATUS request.
		 * 
		 * Arguments:
		 * - NULL byte terminated job handle.
		 * - NULL byte terminated percent complete numerator.
		 * - Percent complete denominator.
		 */
		
		final byte[] jobHandle = packet.getArgumentData(0);
		assert jobHandle != null;
		final ByteArray jobHandleBA = new ByteArray(jobHandle);
		
		final byte[] num = packet.getArgumentData(1);
		assert num != null;
		
		final byte[] den = packet.getArgumentData(2);
		assert den != null;
		
		final Job job = JobAbstract.getJob(jobHandleBA);
		if(job==null) {
			client.sendPacket(StaticPackets.ERROR_JOB_NOT_FOUND, null);
		} else {
			packet.setMagic(Magic.RES);
			job.setStatus(num,den);
			job.sendPacket(packet);
		}
	}
	
}
