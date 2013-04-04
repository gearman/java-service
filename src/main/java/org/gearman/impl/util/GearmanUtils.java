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

package org.gearman.impl.util;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.UUID;

import org.gearman.context.GearmanContext;
import org.gearman.impl.core.GearmanConnection;

public class GearmanUtils {
	
	public boolean testJDBCConnection(String jdbcDriver, String jdbcConnectionString) {
		return false;
	}
	
	public boolean testGearmanConnection(InetSocketAddress address) {
		return false;
	}
	
	public static final String toString(GearmanConnection<?> conn) {
		if(conn==null)
			System.out.println("error");
		return "["+conn.getHostAddress() + ":" + conn.getPort() +"]";
	}
	
	public static final byte[] createUID() {
		return UUID.randomUUID().toString().getBytes();
	}
	
	public static final String getProjectName() {
		return GearmanContext.getProperty(GearmanContext.PROPERTY_PROJECT_NAME);
	}
	
	public static final String getVersion() {
		return GearmanContext.getProperty(GearmanContext.PROPERTY_VERSION);
	}
	
	public static final int getPort() {
		return (Integer) GearmanContext.getAttribute(GearmanContext.ATTRIBUTE_PORT);
	}
	
	public static final long getThreadTimeout() {
		return (Long) GearmanContext.getAttribute(GearmanContext.ATTRIBUTE_THREAD_TIMEOUT); 
	}
	
	public static final int getWorkerThreads() {
		return (Integer) GearmanContext.getAttribute(GearmanContext.ATTRIBUTE_WORKER_THREADS);
	}
	
	public static final Charset getCharset() {
		return (Charset) GearmanContext.getAttribute(GearmanContext.ATTRIBUTE_CHARSET);
	}
	
	public static final String getJobHandlePrefix() {
		return GearmanContext.getProperty(GearmanContext.PROPERTY_JOB_HANDLE_PREFIX);
	}
}
 