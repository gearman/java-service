package org.gearman;

import org.gearman.core.GearmanCompletionHandler;

class ClientJobSubmission<A> {
	public final GearmanJob job;
	public final A att;
	public final GearmanCompletionHandler<A> callback;
	
	public ClientJobSubmission(final GearmanJob job, final A att, final GearmanCompletionHandler<A> callback) {
		this.job = job;
		this.att = att;
		this.callback = callback;
	}
	
	public void fail(final Throwable t) {
		if(this.callback!=null) 
			this.callback.onFail(t, att);
	}
	public void success() {
		this.callback.onComplete(att);
	}
}
