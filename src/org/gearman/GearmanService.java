package org.gearman;

public interface GearmanService {
	public boolean isShutdown();
	public void shutdown();
	public Gearman getGearman();
	public void setLoggerID(String loggerId);
	public String getLoggerID();
}
