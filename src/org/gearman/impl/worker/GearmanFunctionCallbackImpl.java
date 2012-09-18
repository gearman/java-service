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

package org.gearman.impl.worker;

import java.util.Arrays;

import org.gearman.GearmanFunctionCallback;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanPacket.Magic;

class GearmanFunctionCallbackImpl implements GearmanFunctionCallback {
	
	private final byte[] jobHandle;
	private final int connectionID;
	private final WorkerConnectionController wcc;
	
	private boolean isComplete = false;
	
	GearmanFunctionCallbackImpl(byte[] jobHandle, WorkerConnectionController wcc) {
		this.jobHandle = jobHandle;
		this.wcc = wcc;
		this.connectionID = wcc.getConnectionId();
	}

	@Override
	public synchronized void sendWarning(byte[] warning) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		wcc.sendPacket(GearmanPacket.createWORK_WARNING(Magic.REQ, jobHandle, warning), null /*TODO*/, connectionID);
	}

	@Override
	public synchronized void sendData(byte[] data) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");		
		wcc.sendPacket(GearmanPacket.createWORK_DATA(Magic.REQ, jobHandle, data), null /*TODO*/, connectionID);
	}

	@Override
	public synchronized void sendStatus(long numerator, long denominator) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		wcc.sendPacket(GearmanPacket.createWORK_STATUS(Magic.REQ, jobHandle, numerator, denominator), null /*TODO*/, connectionID);
	}
	
	synchronized void success(byte[] data) {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		this.isComplete = true;
		wcc.sendPacket(GearmanPacket.createWORK_COMPLETE(Magic.REQ, jobHandle, data), null /*TODO*/, connectionID);
	}
	
	synchronized void fail() {
		if(this.isComplete()) throw new IllegalStateException("Job has completed");
		this.isComplete = true;
		wcc.sendPacket(GearmanPacket.createWORK_FAIL(Magic.REQ, jobHandle), null /*TODO*/, connectionID);
	}
	
	private boolean isComplete() {
		return isComplete;
	}

	@Override
	public byte[] getJobHandle() {
		return Arrays.copyOf(jobHandle, jobHandle.length);
	}

	@Override
	public boolean isAlive() {
		return (wcc.isOpen())&&(wcc.getConnectionId()==connectionID)&&(!isComplete);
	}

}
