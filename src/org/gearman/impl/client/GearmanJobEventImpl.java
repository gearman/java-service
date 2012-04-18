package org.gearman.impl.client;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventType;

public class GearmanJobEventImpl implements GearmanJobEvent {
	
	private final GearmanJobEventType type;
	private final byte[] data;
	
	public GearmanJobEventImpl(GearmanJobEventType type, byte[] data) {
		this.type = type;
		this.data = data;
	}
	
	@Override
	public GearmanJobEventType getEventType() {
		return type;
	}

	@Override
	public byte[] getData() {
		return data;
	}
}
