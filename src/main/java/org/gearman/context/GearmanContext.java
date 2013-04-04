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

package org.gearman.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearmanContext {
	private GearmanContext() {}
	
	private static final String MAIN_PROPERTIES_FILE_PATH = "jgs.properties";
	
	public static final String PROPERTY_PROJECT_NAME = "gearman.projectName";
	public static final String PROPERTY_VERSION = "gearman.version";
	public static final String PROPERTY_CLASSNAME = "gearman.classname";
	public static final String PROPERTY_PORT = "gearman.port";
	public static final String PROPERTY_THREAD_TIMEOUT = "gearman.threadTimeout";
	public static final String PROPERTY_LOGGER_NAME = "gearman.loggerName";
	public static final String PROPERTY_WORKER_THREADS = "gearman.workerThreads";
	public static final String PROPERTY_JOB_HANDLE_PREFIX = "gearman.jobHandlePrefix";
	
	/**<b>Attribute Type:</b> java.lang.Integer<br><br>The default port number. */
	public static final String ATTRIBUTE_PORT = "gearman.port";
	/** <b>Attribute Type: java.lang.Long</b><br><br>The amount of time before a thread is allowed to die. */
	public static final String ATTRIBUTE_THREAD_TIMEOUT = "gearman.threadTimeout";
	public static final String ATTRIBUTE_WORKER_THREADS = "gearman.workerThreads";
	public static final String ATTRIBUTE_CHARSET = "gearman.charset";
	
	private static final Properties properties = initProperties();
	private static final Map<String, Object> attributes = initAttributes();
	
	public static final Logger LOGGER = initLogger();
	
	private static final Properties initProperties() {
		try {
			final Properties value = new Properties();
			loadProperties(MAIN_PROPERTIES_FILE_PATH, true, value);
			
			return value;
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	private static final Logger initLogger() {
		String loggerName = getProperty(PROPERTY_LOGGER_NAME);
		return LoggerFactory.getLogger(loggerName);
	}

	private static final Map<String, Object> initAttributes() {
		Map<String, Object> value = new HashMap<String, Object>();
		
		String port = getProperty(PROPERTY_PORT);
		value.put(ATTRIBUTE_PORT, Integer.parseInt(port));
		
		String threadTimeout = getProperty(PROPERTY_THREAD_TIMEOUT);
		value.put(ATTRIBUTE_THREAD_TIMEOUT, Long.parseLong(threadTimeout));
		
		String workerThreads = getProperty(PROPERTY_WORKER_THREADS);
		value.put(ATTRIBUTE_WORKER_THREADS, Integer.parseInt(workerThreads));
		
		String charset = "UTF-8";
		value.put(ATTRIBUTE_CHARSET, Charset.forName(charset));
		
		return value;
	}
	
	public static void setAttribute(String name, Object obj) {
		setAttribute(name, obj, attributes);
	}
	
	private static void setAttribute(String name, Object obj, Map<String, Object> attributes) {
		if(obj==null)
			attributes.remove(name);
		else
			attributes.put(name, obj);
	}
	
	public static Object getAttribute(String name) {
		return attributes.get(name);
	}
	
	public static String getProperty(String name) {
		return properties.getProperty(name);
	}
	
	/**
	 * Used to set/overwrite properties programmatically
	 * @param name
	 * 		property name
	 * @param value
	 * 		property value
	 */
	public static void setProperty(String name, String value) {
		if(value==null)
			properties.remove(name);
		else
			properties.setProperty(name, value);
	}
	
	public static void loadProperties(String name, boolean isFileOverwrite) throws IOException {
		loadProperties(name, isFileOverwrite, properties);
	}
	
	private static void loadProperties(String name, boolean isFileOverwrite, Properties properties) throws IOException {
		loadProperties(name, properties);
		
		try {
			if(isFileOverwrite) loadProperties(new File(name), properties);
		} catch(FileNotFoundException e) {
			// If the file does not exist, we're okay
		}
	}
	
	public static void loadProperties(String name) throws IOException {
		loadProperties(name, properties);
	}
	
	private static void loadProperties(String name, Properties properties) throws IOException {
		InputStream in = GearmanContext.class.getClassLoader().getResourceAsStream(name);
		loadProperties(in, properties);
	}
	
	public static void loadProperties(File file) throws IOException {
		loadProperties(file, properties);
	}
	
	private static void loadProperties(File file, Properties properties) throws IOException {
		InputStream in = new FileInputStream(file);
		loadProperties(in, properties);
	}
	
	public static void loadProperties(InputStream stream) throws IOException {
		loadProperties(stream, properties);
	}
	
	private static void loadProperties(InputStream stream, Properties properties) throws IOException {
		properties.load(stream);
	}
	
	public static void saveProperties(File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		saveProperties(out);
	}
	
	public static void saveProperties(OutputStream stream) throws IOException {
		properties.store(stream, null);
	}
}