package org.gearman;

import org.gearman.GearmanClient.SubmitHandler;
import org.gearman.GearmanClient.SubmitResult;

class ClientJobSubmission {
	public final GearmanJob job;
	public final SubmitHandler callback;
	public final boolean isBackground;
	
	public SubmitResult result;
	
	public ClientJobSubmission(final GearmanJob job, final SubmitHandler callback, final boolean isBackground) {
		this.job = job;
		this.callback = callback;
		this.isBackground = isBackground;
	}
	
	public void onSubmissionComplete(final SubmitResult result) {
		synchronized(this) {
			if(this.result!=null) return;
			this.result = result;
			this.notifyAll();
		}
		
		if(this.callback!=null) 
			this.callback.onSubmissionComplete(job, result);
	}
	
	public SubmitResult join() {
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
