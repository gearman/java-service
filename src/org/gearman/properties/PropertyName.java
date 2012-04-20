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

package org.gearman.properties;

public enum PropertyName {
	/** The class name of the gearman implementation */
	GEARMAN_CLASSNAME("gearman.classname", "org.gearman.impl.GearmanImpl"),
	
	/** The default gearman port number */
	GEARMAN_PORT("gearman.port", "4730"),
	
	/** The amount of time a thread must be ideal before it can die */
	GEARMAN_THREADTIMEOUT("gearman.threadtimeout", "30000"),
	
	GEARMAN_LOGGER_NAME("gearman.loggername", "gearman"),
	
	GEARMAN_WORKER_THREADS("gearman.workerthreads", "1");
	
	public final String name;
	public final String defaultValue;
	private PropertyName(String name, String value) {
		this.name = name;
		this.defaultValue = value;
	}
}
