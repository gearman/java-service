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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearmanProperties {
	private GearmanProperties() {}
	
	private static final String PROPERTIES_FILE_PATH = "gearman.properties";
	
	private static final Properties PROPERTIES;
	
	static {
		
		PROPERTIES = new Properties();
		
		for(PropertyName name : PropertyName.values()) {
			PROPERTIES.setProperty(name.name, name.defaultValue);
		}
		
		final File propertiesFile = new File(PROPERTIES_FILE_PATH);
		
		if(propertiesFile.canRead()) {
			try (FileInputStream in = new FileInputStream(propertiesFile)){
				PROPERTIES.load(in);
			} catch(IOException ioe) {
				Logger logger = LoggerFactory.getLogger(getProperty(PropertyName.GEARMAN_LOGGER_NAME));
				logger.warn("failed to load properties", ioe);
			}
		}
	}
	
	public static String getProperty(PropertyName name) {
		return PROPERTIES.getProperty(name.name, name.defaultValue);
	}
	
	public static void setProperty(PropertyName name, String value) {
		PROPERTIES.getProperty(name.name, name.defaultValue);
	}
	
	public static void save() throws IOException {
		final File propertiesFile = new File(PROPERTIES_FILE_PATH);
		
		try (FileOutputStream out = new FileOutputStream(propertiesFile)){
			PROPERTIES.store(out, null);
		} catch (IOException ioe) {
			Logger logger = LoggerFactory.getLogger(getProperty(PropertyName.GEARMAN_LOGGER_NAME));
			logger.warn("failed to save properties", ioe);
			
			throw ioe;
		}
	}
}
