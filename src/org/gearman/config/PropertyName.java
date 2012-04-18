package org.gearman.config;

public enum PropertyName {
	/** The class name of the gearman implementation */
	GEARMAN_CLASSNAME("gearman.classname", "org.gearman.impl.GearmanImpl"),
	
	/** The default gearman port number */
	GEARMAN_PORT("gearman.port", "4730"),
	
	/** The amount of time a thread must be ideal before it can die */
	GEARMAN_THREADTIMEOUT("gearman.threadtimeout", "60000"),
	
	GEARMAN_LOGGER_NAME("gearman.loggername", "gearman");
	
	public final String name;
	public final String defaultValue;
	private PropertyName(String name, String value) {
		this.name = name;
		this.defaultValue = value;
	}
}
