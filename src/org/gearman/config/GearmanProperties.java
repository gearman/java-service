package org.gearman.config;

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
		
		final File propertiesFile = new File(PROPERTIES_FILE_PATH);
		
		if(propertiesFile.canRead()) {
			try (FileInputStream in = new FileInputStream(propertiesFile)){
				PROPERTIES.load(in);
			} catch(IOException ioe) {
				Logger logger = LoggerFactory.getLogger(PropertyName.GEARMAN_LOGGER_NAME.defaultValue);
				logger.warn("failed to load properties", ioe);
			}
		}
	}
	
	public static String getProperty(PropertyName name) {
		return PROPERTIES.getProperty(name.name, name.defaultValue);
	}
	
	public static void setProperty(PropertyName name, String value) {
		PROPERTIES.getProperty(name.name, name.defaultValue);
		save();
	}
	
	private static void save() {
		final File propertiesFile = new File(PROPERTIES_FILE_PATH);
		
		try (FileOutputStream out = new FileOutputStream(propertiesFile)){
			PROPERTIES.store(out, null);
		} catch (IOException ioe) {
			Logger logger = LoggerFactory.getLogger(PropertyName.GEARMAN_LOGGER_NAME.defaultValue);
			logger.warn("failed to save properties", ioe);
		}
	}
}
