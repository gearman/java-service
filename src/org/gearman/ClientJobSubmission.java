package org.gearman;

import org.gearman.GearmanClient.SubmitCallbackResult;
import org.gearman.core.GearmanCallbackHandler;

class ClientJobSubmission {
	public final GearmanJob job;
	public final GearmanCallbackHandler<GearmanJob, SubmitCallbackResult> callback;
	public final boolean isBackground;
	
	public SubmitCallbackResult result;
	
	public ClientJobSubmission(final GearmanJob job, final GearmanCallbackHandler<GearmanJob, SubmitCallbackResult> callback, final boolean isBackground) {
		this.job = job;
		this.callback = callback;
		this.isBackground = isBackground;
	}
	
	public void onSubmissionComplete(final SubmitCallbackResult result) {
		synchronized(this) {
			if(this.result!=null) return;
			this.result = result;
			this.notifyAll();
		}
		
		if(this.callback!=null) 
			this.callback.onComplete(job, result);
	}
	
	public SubmitCallbackResult join() {
		/*
		 *  This method is a non-interrupting blocking method.
		 *  If interrupted, the current thread will be re-interrupted
		 *  to maintain some constancy.
		 */
		
		boolean interupted = false;
		
		try {
		synchronized(this) {
			while(this.result==null) {
				try {
					this.wait();
				} catch(InterruptedException e) {
					interupted = true;
					boolean test = Thread.interrupted();
					assert test == true;
				}
			}
			
			assert this.result!=null;
			return this.result;
		}
		} finally {
			if(interupted) Thread.currentThread().interrupt();
		}
	}
}
