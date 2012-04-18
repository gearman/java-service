package org.gearman.impl.data;

public class GearmanJob {
	
	private final String functionName;
	private final byte[] data;
	
	public GearmanJob(final String functionName, byte[] data) {
		this.functionName = functionName;
		this.data = data;
	}
	
	public String getFunctionName() {
		return functionName;
	}

	public byte[] getData() {
		return data;
	}
}
