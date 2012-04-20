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
 * The interface used by the {@link GearmanFunction} to send data back to the client mid-process.
 * @author isaiah
 */
public interface GearmanFunctionCallback {
	
	/**
	 * The warning callback channel. This is just like the data callback channel,
	 * but its purpose is to send warning information.<br>
	 * <br>
	 * When the {@link GearmanFunction} receives a GearmanJob, this method can
	 * used to send information to the client while the job is executing<br>
	 * <br>
	 * Client Side: Override this method to define how information received on
	 * this callback channel should be handled. If this method is not
	 * overwritten, the information will be dropped.<br>
	 * <br>
	 * Worker Side: This method will throw a IllegalStateException if the job
	 * has completed. Only while the job is executing can the callback methods
	 * be used.<br>
	 * 
	 * @param warning
	 *            Information to send on the data callback channel
	 */
	public void sendWarning(byte[] warning);
	
	/**
	 * The data callback channel.<br>
	 * <br>
	 * When the {@link GearmanFunction} receives a GearmanJob, this method can
	 * used to send information to the client while the job is executing<br>
	 * <br>
	 * Client Side: Override this method to define how information received on
	 * this callback channel should be handled. If this method is not
	 * overwritten, the information will be dropped.<br>
	 * <br>
	 * Worker Side: This method will throw a IllegalStateException if the job
	 * has completed. Only while the job is executing can the callback methods
	 * be used.<br>
	 * 
	 * @param data
	 *            Information to send on the data callback channel
	 */
	public abstract void sendData(byte[] data);
	
	/**
	 * Updates the client of the job progress.<br>
	 * <br>
	 * When the {@link GearmanFunction} receives a GearmanJob, this method can
	 * used to send information about the jobs progress back to the client.<br>
	 * <br>
	 * Client Side: Override this method to define how information received on
	 * this callback channel should be handled. If this method is not
	 * overwritten, the information will be dropped.<br>
	 * <br>
	 * Worker Side: This method will throw a IllegalStateException if the job
	 * has completed. Only while the job is executing can the callback methods
	 * be used.<br>
	 * @param numerator
	 * 		A number typically specifying the numerator in the fraction work that's
	 * 		completed 
	 * @param denominator
	 * 		A number typically specifying the denominator in the fraction work that's
	 * 		completed
	 */
	public abstract void sendStatus(long numerator, long denominator);
}
