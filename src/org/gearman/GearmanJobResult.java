package org.gearman;

public class GearmanJobResult {
	
	static final GearmanJobResult WORKER_FAIL = new GearmanJobResult(null, Type.WORKER_FAIL);
	static final GearmanJobResult SUBMISSION_FAIL = new GearmanJobResult(null, Type.SUBMISSION_FAIL);
	static final GearmanJobResult DISCONNECT_FAIL = new GearmanJobResult(null, Type.DISCONNECT_FAIL);
	static final GearmanJobResult SUCCESS = new GearmanJobResult(null, Type.SUCCESSFUL);
	
	public enum Type {SUCCESSFUL, DISCONNECT_FAIL, SUBMISSION_FAIL, WORKER_FAIL}
	
	private final byte[] resultData;
	private final Type resultStatus;
	
	
	GearmanJobResult (final byte[] resultData, final Type resultStatus) {
		this.resultData = resultData;
		this.resultStatus = resultStatus;
	}
	
	public static final GearmanJobResult workSuccessful(final byte[] data) {
		return new GearmanJobResult(data, Type.SUCCESSFUL);
	}
	
	public static final GearmanJobResult workSuccessful() {
		return SUCCESS;
	}
	
	public static final GearmanJobResult workFailed() {
		return WORKER_FAIL;
	}
	
	public boolean isSuccessful() {
		return resultStatus.equals(Type.SUCCESSFUL);
	}
	
	public Type getResultType() {
		return resultStatus;
	}
	
	public byte[] getResultData() {
		return this.resultData;
	}
}
