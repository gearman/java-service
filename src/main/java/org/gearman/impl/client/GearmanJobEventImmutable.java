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

package org.gearman.impl.client;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventType;
import org.gearman.impl.util.GearmanUtils;

class GearmanJobEventImmutable extends GearmanJobEventImpl {
	
	public static final GearmanJobEvent GEARMAN_EOF = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_EOF, "EOF".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_CONNECTION_FAILED = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Connection Failed".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SERVER_NOT_AVAILABLE = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Server Not Available".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SERVICE_SHUTDOWN = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Service Shutdown".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SEND_FAILED = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Failed to Send Job".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_JOB_DISCONNECT = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_JOB_FAIL, "Server Disconnect".getBytes(GearmanUtils.getCharset()));
	public static final GearmanJobEvent GEARMAN_JOB_FAIL = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_JOB_FAIL, "Failed By Worker".getBytes(GearmanUtils.getCharset()));

	private GearmanJobEventImmutable(GearmanJobEventType type, byte[] data) {
		super(type, data);
	}
	
	@Override
	public byte[] getData() {
		return super.getData().clone();
	}

}
