package org.gearman.core;

public interface GearmanCallbackHandler<D,R extends GearmanCallbackResult> {
	public void onComplete(D data, R result);
}
