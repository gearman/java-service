package org.gearman;

class JobStatus implements GearmanJobStatus {

	public static final long TIMEOUT = 20000;
	
	private OperationResult	opResult	= null;
	
	private boolean		isKnown		= false;
	private boolean		isRunning	= false;
	private long		numerator	= 0L;
	private long		denominator	= 0L;
	
	@Override
	public final boolean isOperationSuccessful() {
		if(!this.isDone()) throw new IllegalStateException("should not be called until result is set");
		return this.opResult.isSuccessful();
	}
	
	@Override
	public final OperationResult getOperationResult() {
		if(!this.isDone()) throw new IllegalStateException("should not be called until result is set");
		return this.opResult;
	}
	
	@Override
	public final long getDenominator() {
		if(!this.isDone()) throw new IllegalStateException("should not be called until result is set");
		return this.denominator;
	}

	@Override
	public final long getNumerator() {
		if(!this.isDone()) throw new IllegalStateException("should not be called until result is set");
		return this.numerator;
	}

	@Override
	public final boolean isKnown() {
		if(!this.isDone()) throw new IllegalStateException("should not be called until result is set");
		return this.isKnown;
	}

	@Override
	public final boolean isRunning() {
		if(!this.isDone()) throw new IllegalStateException("should not be called until result is set");
		return this.isRunning;
	}

	protected final synchronized void complete(final OperationResult opResult, boolean isKnown, final boolean isRunning, final long numerator, final long denominator) {
		if(opResult.isSuccessful()) {
			this.isKnown = isKnown;
			this.isRunning = isRunning;
			this.numerator = numerator;
			this.denominator = denominator;
		}
		
		this.notifyAll();
		this.onComplete(opResult);
	}
	
	public final boolean isDone() {
		return this.opResult!=null;
	}
	
	public final synchronized void join() {
		try {
			if(!this.isDone()) 
				this.wait(TIMEOUT);
			if(!this.isDone()) 
				this.complete(OperationResult.OPERATION_TIMED_OUT, false, false, 0, 0);
		} catch (InterruptedException ie) {
			this.complete(OperationResult.OPERATION_INTERRUPTED, false, false, 0, 0);
		}
	}
	
	protected void onComplete(final OperationResult result) {}
}
