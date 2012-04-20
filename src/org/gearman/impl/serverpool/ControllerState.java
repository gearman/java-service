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

public enum ControllerState {
	
	/**
	 * The state specifying the ConnectionController is in the processes of connecting to a server<br>
	 * <br>
	 * You can enter this state from the CLOSED and WAITING state
	 */
	CONNECTING,
	
	/**
	 * The state specifying the ConnectionController is connected to a job server<br>
	 * <br>
	 * You can enter this state from the CONNECTING state.
	 */
	OPEN,
	
	/**
	 * The state specifying the ConnectionController is waiting to close. The controller
	 * will enter this state if the user has specified it wants to close the connection
	 * but there are pending JOB_STATUS 
	 */
	CLOSE_PENDING,
	
	
	/**
	 * The state specifying the ConnectionController is not connected to a job server<br>
	 * <br>
	 * You can enter this state from the CONNECTING, OPEN, CLOSED, and WAITING states. This is the initial state.
	 */
	CLOSED,
	
	/**
	 * The state specifying the ConnectionController is no longer in regular use or controlled by
	 * the ServiceClient
	 * <br>
	 *  You can enter this state from the CONNECTING, OPEN, CLOSED, CLOSE_PENDING ,DROPPED, and WAITING states. This is the final state 
	 */
	DROPPED,
	
	/**
	 * The state specifying the ConnectionController is in a suggested timeout period. It is
	 * suggested that we wait out a period time before attempting to move to the OPEN state.
	 * However, this is only a suggestion. It is legal to move into the OPEN state.   
	 * <br>
	 * You can enter this state from the CONNECTING, OPEN, CLOSED, and WAITING states.
	 */
	WAITING
}
