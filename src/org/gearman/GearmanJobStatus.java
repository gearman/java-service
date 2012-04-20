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
 * An immutable object specifying a job's status at a 
 * @author isaiah
 */
public interface GearmanJobStatus {
	
	/**
	 * Tests if the server knew the status of the job in question.
	 * 
	 * Most job servers will return unknown if it never received the job or
	 * if the job has already been completed.
	 * 
	 * If the status is known but not running, then a worker has not yet
	 * polled the job
	 * 
	 * @return
	 * 		<code>true</code> if the server knows the status of the job in question
	 */
	public boolean isKnown();
	
	/**
	 * Tests if the job is currently running.
	 * 
	 * If the status is unknown, this value will always be false.
	 *  
	 * @return
	 * 		<code>true</code> if the job is currently being worked on. <code>
	 * 		false</code> otherwise.
	 */
	public boolean isRunning();
	
	/**
	 * The percent complete numerator.
	 * @return
	 * 		the percent complete numerator.
	 */
	public long getNumerator();
	
	/**
	 * The percent complete denominator.
	 * @return
	 * 		the percent complete denominator.
	 */
	public long getDenominator();
}
