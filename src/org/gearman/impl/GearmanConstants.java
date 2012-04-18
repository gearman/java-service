package org.gearman.impl;

import java.nio.charset.Charset;

import org.gearman.config.GearmanProperties;
import org.gearman.config.PropertyName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GearmanConstants {
	
	public static final String CHARSET_NAME = "UTF-8";
	public static final String PROJECT_NAME = "@project.name@";
	public static final String VERSION = "@project.version@";
	
	public static final int PORT; 
	public static final long THREAD_TIMEOUT;
	
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
			LOGGER.warn("parsing thread timeout failed, reverting to " + PropertyName.GEARMAN_THREADTIMEOUT.defaultValue + "milliseconds");
			timeout = Integer.parseInt(PropertyName.GEARMAN_THREADTIMEOUT.defaultValue);
		}
		THREAD_TIMEOUT = timeout;
	}
}
