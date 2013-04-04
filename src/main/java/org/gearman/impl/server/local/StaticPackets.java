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

import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanPacket.Magic;
import org.gearman.impl.core.GearmanPacket.Type;
import org.gearman.impl.util.GearmanUtils;

/**
 * All static gearman packets are located in this container
 * 
 * @author isaiah
 */
final class StaticPackets {

	private StaticPackets() {
	}

	/**
	 * This is given to notify the client that a bad magic number was used.
	 */
	public static final GearmanPacket ERROR_BAD_MAGIC = new GearmanPacket(
			Magic.RES, Type.ERROR, "bad_magic".getBytes(), "Request magic expected".getBytes(GearmanUtils.getCharset()));

	/**
	 * Sent to a client when an unknown or unimplemented packet type is received
	 */
	public static final GearmanPacket ERROR_BAD_COMMAND = new GearmanPacket(
			Magic.RES, Type.ERROR, "bad command".getBytes(), "Command not expected".getBytes());

	/**
	 * Sent to a client when a job cannot be found in with the following
	 * requests WORK_STATUS, WORK_COMPLETE, WORK_FAIL, WORK_EXCEPTION,
	 * WORK_DATA, and WORK_WARNING,
	 */
	public static final GearmanPacket ERROR_JOB_NOT_FOUND = new GearmanPacket(
			Magic.RES, Type.ERROR, new byte[] { 106,
					111, 98, 95, 110, 111, 116, 95, 102, 111, 117, 110, 100}, new byte[]{
					74, 111, 98, 32, 103, 105, 118, 101, 110, 32, 105, 110, 32,
					119, 111, 114, 107, 32, 114, 101, 115, 117, 108, 116, 32,
					110, 111, 116, 32, 102, 111, 117, 110, 100 });

	public static final GearmanPacket ERROR_UNKNOWN_OPTION = new GearmanPacket(
			Magic.RES, Type.ERROR, new byte[] { 117,
					110, 107, 110, 111, 119, 110, 95, 111, 112, 116, 105, 111,
					110}, new byte[]{ 83, 101, 114, 118, 101, 114, 32, 100, 111, 101,
					115, 32, 110, 111, 116, 32, 114, 101, 99, 111, 103, 110,
					105, 122, 101, 32, 103, 105, 118, 101, 110, 32, 111, 112,
					116, 105, 111, 110 });

	public static final GearmanPacket ERROR_QUEUE_FULL = new GearmanPacket(
			Magic.RES, Type.ERROR, new byte[] { 113,
					117, 101, 117, 101, 95, 102, 117, 108, 108}, new byte[]{ 74, 111, 98,
					32, 113, 117, 101, 117, 101, 32, 105, 115, 32, 102, 117,
					108, 108 });

	public static final GearmanPacket OPTION_RES_EXCEPTIONS = new GearmanPacket(
			Magic.RES, Type.OPTION_RES, new byte[] {
					101, 120, 99, 101, 112, 116, 105, 111, 110, 115 });

	public static final GearmanPacket TEXT_UNKNOWN_COMMAND = GearmanPacket.createTEXT("ERR unknown_command Unknown+server+command\n");
	public static final GearmanPacket TEXT_INCOMPLETE_ARGS = GearmanPacket.createTEXT("ERR incomplete_args An+incomplete+set+of+arguments+was+sent+to+this+command\n");
	public static final GearmanPacket TEXT_OK = GearmanPacket.createTEXT("OK\n");
	public static final GearmanPacket TEXT_DONE = GearmanPacket.createTEXT(".\n");
	public static final GearmanPacket TEXT_VERSION = GearmanPacket.createTEXT(GearmanUtils.getVersion());
}
