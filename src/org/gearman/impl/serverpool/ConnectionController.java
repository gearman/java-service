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

package org.gearman.impl.serverpool;

import java.util.concurrent.TimeUnit;

import org.gearman.GearmanLostConnectionGrounds;
import org.gearman.GearmanLostConnectionPolicy;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.core.GearmanConnection.SendCallbackResult;
import org.gearman.impl.util.ByteArray;

public interface ConnectionController {
	public ControllerState getControllerState();
	public void getStatus(final ByteArray jobHandle, GearmanJobStatusCallback callback);
	public void dropServer();
	public void closeServer();
	public void waitServer(Runnable callback);
	public void waitServer(Runnable callback, long waittime, final TimeUnit unit);
	public boolean openServer(final boolean force);
	public boolean sendPacket(GearmanPacket packet, GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback);
	
	public void onConnect(ControllerState oldState);
	public void onOpen(ControllerState oldState);
	public void onClose(ControllerState oldState);
	public void onDrop(ControllerState oldState);
	public void onWait(ControllerState oldState);
	public void onNew();
	public void onLostConnection(GearmanLostConnectionPolicy policy, GearmanLostConnectionGrounds grounds);
}
