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
import org.gearman.GearmanPersistable;
import org.gearman.impl.util.GearmanUtils;

class ServerPersistable implements GearmanPersistable {
	
	private final String functionName;
	private final byte[] data;
	private final byte[] jobHandle;
	private final byte[] uniqueID;
	private final long epoch;
	private final GearmanJobPriority priority;
	
	ServerPersistable(Job job) {
		this.functionName = job.getFunction().getName().toString(GearmanUtils.getCharset());
		this.data = job.getData().clone();
		this.jobHandle = job.getJobHandle().getBytes();
		this.uniqueID = job.getUniqueID().getBytes();
		this.epoch = 0;
		this.priority = job.getPriority();
	}
	
	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public byte[] getJobHandle() {
		return jobHandle;
	}

	@Override
	public byte[] getUniqueID() {
		return uniqueID;
	}

	@Override
	public long epochTime() {
		return epoch;
	}

	@Override
	public GearmanJobPriority getPriority() {
		return priority;
	}
}
