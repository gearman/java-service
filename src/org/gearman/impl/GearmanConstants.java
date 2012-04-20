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

package org.gearman.impl;

import java.nio.charset.Charset;

import org.gearman.properties.GearmanProperties;
import org.gearman.properties.PropertyName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearmanConstants {
	
	public static final String PROJECT_NAME = "@project.name@";
	public static final String VERSION = "@project.version@";
	
	public static final int PORT; 
	public static final long THREAD_TIMEOUT;
	public static final int WORKER_THREADS;
	
	private static final String CHARSET_NAME = "UTF-8";
	public static final Charset CHARSET;
	
	public static final Logger LOGGER;
	
	static {
		CHARSET = Charset.forName(CHARSET_NAME);
		LOGGER = LoggerFactory.getLogger(GearmanProperties.getProperty(PropertyName.GEARMAN_LOGGER_NAME));
		
		String portStr = GearmanProperties.getProperty(PropertyName.GEARMAN_PORT);
		int port;
		try {
			port = Integer.parseInt(portStr);
		} catch (NumberFormatException nfe) {
			LOGGER.warn("parsing port number failed, reverting to port " + PropertyName.GEARMAN_PORT.defaultValue);
			port = Integer.parseInt(PropertyName.GEARMAN_PORT.defaultValue);
		}
		PORT = port;
		
		String timeoutStr = GearmanProperties.getProperty(PropertyName.GEARMAN_THREADTIMEOUT);
		long timeout;
		try {
			timeout = Long.parseLong(timeoutStr);
		} catch(NumberFormatException nfe) {
			LOGGER.warn("parsing thread timeout failed, reverting to " + PropertyName.GEARMAN_THREADTIMEOUT.defaultValue + " milliseconds");
			timeout = Integer.parseInt(PropertyName.GEARMAN_THREADTIMEOUT.defaultValue);
		}
		THREAD_TIMEOUT = timeout;
		
		String workerThreadsStr = GearmanProperties.getProperty(PropertyName.GEARMAN_WORKER_THREADS);
		int workerThreads;
		try {
			workerThreads = Integer.parseInt(workerThreadsStr);
		} catch(NumberFormatException nfe) {
			LOGGER.warn("parsing worker threads failed, reverting to " + PropertyName.GEARMAN_WORKER_THREADS.defaultValue + " threads");
			workerThreads = Integer.parseInt(PropertyName.GEARMAN_WORKER_THREADS.defaultValue);
		}
		WORKER_THREADS = workerThreads;
		
	}
}
