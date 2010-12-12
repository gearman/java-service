package org.gearman.core;

public interface GearmanCompletionHandler<A> {
	public void onComplete(A attachment);
	public void onFail(Throwable exc, A attachment);
}
