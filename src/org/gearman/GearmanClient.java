package org.gearman;

import org.gearman.core.GearmanCompletionHandler;

/**
 * A GearmanCliet submits {@link GearmanJob}s and {@link GearmanBackgroundJob}s
 * to gearman job servers
 * 
 * @author isaiah.v
 */
public interface GearmanClient extends GearmanJobServerPool {
	
	/**
	 * Submits a {@link GearmanBackgroundJob} to be executed. Background jobs
	 * are detached from the client, so no callback information or result will
	 * be received through the {@link GearmanBackgroundJob} object. However,
	 * status information can be polled from the server using the
	 * {@link GearmanBackgroundJob} object after the.
	 * 
	 * @param job
	 *            The job to be submitted
	 */
	public <A> void submitJob(GearmanBackgroundJob job, A att ,GearmanCompletionHandler<A> callback);

	/**
	 * Submits a {@link GearmanJob} to be executed.<br>
	 * If submitting fails, the user will be notified by failing the job.<br>
	 * 
	 * @param job
	 *            The job being submitted
	 */
	public void submitJob(GearmanJob job);
	public <A> void submitJob(GearmanJob job, A att ,GearmanCompletionHandler<A> callback);

	/**
	 * By default the exception callback channel is closed. The client will need
	 * to communicate to job servers that exceptions should be forwarded to the
	 * client.
	 * 
	 * @param isOpen
	 *            If set to true, the exception callback channel is opened. If
	 *            set to false the exception callback channel is closed. The
	 *            default value is false.
	 */
	public void setExceptionChannelOpen(boolean isOpen);
	
	public boolean isExceptionChannelOpen();
}
