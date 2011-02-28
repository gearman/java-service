package org.gearman;

import org.gearman.GearmanJobStatus.StatusResult;
import org.gearman.core.GearmanCallbackHandler;

class JobStatus implements GearmanJobStatus, StatusResult {

	public static final long OP_TIMEOUT = 20000;
	
	private StatusCallbackResult result;
	
	private GearmanCallbackHandler<GearmanJob, StatusResult> callback;
	private GearmanJob job;
	
	private boolean		isKnown		= false;
	private boolean		isRunning	= false;
	private long		numerator	= 0L;
	private long		denominator	= 0L;
	
	public JobStatus(GearmanJob job, GearmanCallbackHandler<GearmanJob, StatusResult> callback) {
		this.job = job;
		this.callback = callback;
	}
	
	@Override
	public final long getDenominator() {
		return this.denominator;
	}

	@Override
	public final long getNumerator() {
		return this.numerator;
	}

	@Override
	public final boolean isKnown() {
		return this.isKnown;
	}

	@Override
	public final boolean isRunning() {
		return this.isRunning;
	}

	protected synchronized void complete(StatusCallbackResult result, boolean isKnown, final boolean isRunning, final long numerator, final long denominator) {
		this.result = result;
		
		this.isKnown = isKnown;
		this.isRunning = isRunning;
		this.numerator = numerator;
		this.denominator = denominator;
		
		final GearmanCallbackHandler<GearmanJob, StatusResult> callback = this.callback;
		final GearmanJob job = this.job;
		
		this.callback = null;
		this.job = null;
		
		callback.onComplete(job, this);
	}

	@Override
	public GearmanJobStatus getGearmanJobStatus() {
		// This method should not be accessible to the user until after the result is defined.
		assert this.result!=null;		
		return this.result.isSuccessful()? this: null;
	}

	@Override
	public StatusCallbackResult getStatusCallbackResult() {
		// This method should not be accessible to the user until after the result is defined.
		assert this.result!=null;
		return result;
	}

	@Override
	public boolean isSuccessful() {
		// This method should not be accessible to the user until after the result is defined.
		assert this.result!=null;
		return this.result.isSuccessful();
	}
}
