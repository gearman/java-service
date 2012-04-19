package org.gearman;

public interface GearmanJobEventCallback<A> {
	public void onEvent(A attachment, GearmanJobEvent event);
}
