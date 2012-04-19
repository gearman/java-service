package org.gearman.impl.client;

import org.gearman.GearmanJobEvent;

interface BackendJobReturn {
	public void put(GearmanJobEvent event);
	public void eof(GearmanJobEvent lastevent);
}
