package org.gearman;

import org.gearman.core.GearmanCallbackResult;

public class GearmanJobResult implements GearmanCallbackResult {
		
	static final GearmanJobResult WORKER_FAIL = new GearmanJobResult(null, JobCallbackResult.WORKER_FAIL);
	static final GearmanJobResult SUBMISSION_FAIL = new GearmanJobResult(null, JobCallbackResult.SUBMISSION_FAIL);
	static final GearmanJobResult DISCONNECT_FAIL = new GearmanJobResult(null, JobCallbackResult.DISCONNECT_FAIL);
	static final GearmanJobResult SUCCESS = new GearmanJobResult(null, JobCallbackResult.SUCCESSFUL);
	
	public enum JobCallbackResult implements GearmanCallbackResult {
		SUCCESSFUL,
		DISCONNECT_FAIL,
		SUBMISSION_FAIL,
		WORKER_FAIL;

		@Override
		public boolean isSuccessful() {
			return this.equals(SUCCESSFUL);
		}
	}
	
	private final byte[] data;
	private final JobCallbackResult status;
	
	GearmanJobResult (final byte[] resultData, final JobCallbackResult resultStatus) {
		this.data = resultData;
		this.status = resultStatus;
	}
	
	public static final GearmanJobResult workSuccessful(final byte[] data) {
		return new GearmanJobResult(data, JobCallbackResult.SUCCESSFUL);
	}
	
	public static final GearmanJobResult workSuccessful() {
		return SUCCESS;
	}
	
	public static final GearmanJobResult workFailed() {
		return WORKER_FAIL;
	}
	
	public boolean isSuccessful() {
		return this.status.isSuccessful();
	}
	
	public JobCallbackResult getJobCallbackResult() {
		return status;
	}
	
	public byte[] getData() {
		return this.data;
	}
}
