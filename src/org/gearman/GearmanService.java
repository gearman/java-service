package org.gearman;

public interface GearmanService {
	public boolean isShutdown();
	public void shutdown();
	public Gearman getGearman();
}
