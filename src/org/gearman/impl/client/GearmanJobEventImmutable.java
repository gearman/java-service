package org.gearman.impl.client;

import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventType;
import org.gearman.impl.GearmanConstants;

class GearmanJobEventImmutable extends GearmanJobEventImpl {
	
	public static final GearmanJobEvent GEARMAN_EOF = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_EOF, "EOF".getBytes(GearmanConstants.CHARSET));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_CONNECTION_FAILED = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Connection Failed".getBytes(GearmanConstants.CHARSET));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SERVER_NOT_AVAILABLE = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Server Not Available".getBytes(GearmanConstants.CHARSET));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SERVICE_SHUTDOWN = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Service Shutdown".getBytes(GearmanConstants.CHARSET));
	public static final GearmanJobEvent GEARMAN_SUBMIT_FAIL_SEND_FAILED = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_SUBMIT_FAIL, "Failed to Send Job".getBytes(GearmanConstants.CHARSET));
	public static final GearmanJobEvent GEARMAN_JOB_DISCONNECT = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_JOB_FAIL, "Server Disconnect".getBytes(GearmanConstants.CHARSET));
	public static final GearmanJobEvent GEARMAN_JOB_FAIL = new GearmanJobEventImmutable(GearmanJobEventType.GEARMAN_JOB_FAIL, "Failed By Worker".getBytes(GearmanConstants.CHARSET));

	private GearmanJobEventImmutable(GearmanJobEventType type, byte[] data) {
		super(type, data);
	}
	
	@Override
	public byte[] getData() {
		return super.getData().clone();
	}

}
