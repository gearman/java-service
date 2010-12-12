package org.gearman;

public class GearmanJobResult {
	
	private static final GearmanJobResult FAIL = new GearmanJobResult(null, ResultStatus.WORKER_FAIL);
	private static final GearmanJobResult SUCCESS = new GearmanJobResult(null, ResultStatus.SUCCESSFUL);
	
	public enum ResultStatus {SUCCESSFUL, DISCONNECT_FAIL, SUBMISSION_FAIL, WORKER_FAIL}
	
	private final byte[] resultData;
	private final ResultStatus resultStatus;
	
	
	GearmanJobResult (final byte[] resultData, final ResultStatus resultStatus) {
		this.resultData = resultData;
		this.resultStatus = resultStatus;
	}
	
	public static final GearmanJobResult workSuccessful(final byte[] data) {
		return new GearmanJobResult(data, ResultStatus.SUCCESSFUL);
	}
	
	public static final GearmanJobResult workSuccessful() {
		return SUCCESS;
	}
	
	public static final GearmanJobResult workFailed() {
		return FAIL;
	}
	
	public boolean isSuccessful() {
		return resultStatus.equals(ResultStatus.SUCCESSFUL);
	}
	
	public ResultStatus getResultStatus() {
		return resultStatus;
	}
	
	public byte[] getResultData() {
		return this.resultData;
	}
}
