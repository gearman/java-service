package org.gearman;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class JobStatus implements GearmanJobStatus, Future<GearmanJobStatus> {

	public static final long OP_TIMEOUT = 20000;
	
	private OperationResult	opResult	= null;
	
	private boolean		isCanceled	= false;
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

	protected synchronized void complete(final OperationResult opResult, boolean isKnown, final boolean isRunning, final long numerator, final long denominator) {
		if(opResult.isSuccessful()) {
			this.isKnown = isKnown;
			this.isRunning = isRunning;
			this.numerator = numerator;
			this.denominator = denominator;
		}
		
		this.notifyAll();
	}
	
	@Override
	public final boolean isDone() {
		return this.opResult!=null;
	}
	
	@Override
	public synchronized boolean cancel(boolean arg0) {
		if(this.isDone()) 
			return false;
		
		this.isCanceled = true;
		this.notifyAll();
		return true;
	}

	@Override
	public final synchronized GearmanJobStatus get() throws InterruptedException, ExecutionException {
		if(this.isCanceled) {
			throw new CancellationException("Operation Cancelled");
		} if(!this.isDone()) {
			this.wait();
			
			if(this.isCanceled)
				throw new CancellationException("Operation Cancelled");
			
			// If the operation was not canceled, we should be able to assume it's complete 
			assert this.isDone();
		}
		return this;
	}

	@Override
	public final synchronized GearmanJobStatus get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if(this.isCanceled) {
			throw new CancellationException("Operation Cancelled");
		} else if (!this.isDone()) {
			unit.timedWait(this, timeout);
			
			if(this.isCanceled)
				throw new CancellationException("Operation Cancelled");
			if(!this.isDone())
				throw new TimeoutException();
		}
		
		return this;
	}

	@Override
	public final boolean isCancelled() {
		return this.isCanceled;
	}
}
