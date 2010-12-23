package org.gearman;

import java.io.IOException;

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
	 * {@link GearmanBackgroundJob} object.
	 * 
	 * @param job
	 *            The job to be submitted
	 * @throws IOException
	 *             Thrown if the job cannot be sent to any of the listed job
	 *             servers
	 */
	public <A> void submitJob(GearmanBackgroundJob job, A att, GearmanCompletionHandler<A> callback);

	/**
	 * Submits a {@link GearmanJob} to be executed.<br>
	 * 
	 * @param job
	 *            The job being submitted
	 * @throws IOException
	 *             Thrown if the job cannot be sent to any of the listed job
	 *             servers
	 */
	public <A> void submitJob(GearmanJob job, A att, GearmanCompletionHandler<A> callback);
	
	public <A> void submitJob(GearmanJob job);

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
	public void setExceptionChannel(boolean isOpen);
}
