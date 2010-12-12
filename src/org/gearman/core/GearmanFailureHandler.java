package org.gearman.core;

public interface GearmanFailureHandler<A> {
	public void onFail(Throwable exc, A attachment);
}
