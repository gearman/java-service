package org.gearman.impl.server.local;

import org.gearman.GearmanJobPriority;
import org.gearman.GearmanPersistable;
import org.gearman.impl.GearmanConstants;

class ServerPersistable implements GearmanPersistable {
	
	private final String functionName;
	private final byte[] data;
	private final byte[] jobHandle;
	private final byte[] uniqueID;
	private final long epoch;
	private final GearmanJobPriority priority;
	
	ServerPersistable(Job job) {
		this.functionName = job.getFunction().getName().toString(GearmanConstants.CHARSET);
		this.data = job.getData().clone();
		this.jobHandle = job.getJobHandle().getBytes();
		this.uniqueID = job.getUniqueID().getBytes();
		this.epoch = 0;
		this.priority = job.getPriority();
	}
	
	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public byte[] getJobHandle() {
		return jobHandle;
	}

	@Override
	public byte[] getUniqueID() {
		return uniqueID;
	}

	@Override
	public long epochTime() {
		return epoch;
	}

	@Override
	public GearmanJobPriority getPriority() {
		return priority;
	}
}
