package org.gearman;

import java.lang.ref.WeakReference;
import java.util.UUID;

import org.gearman.GearmanJobStatus.StatusCallbackResult;
import org.gearman.GearmanJobStatus.StatusResult;
import org.gearman.JobServerPoolAbstract.ConnectionController;
import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanConnection;
import org.gearman.util.ByteArray;


public abstract class GearmanJob {
	
	/**
	 * Defines the priority of a GearmanJob. The priority defines where in the
	 * server's job queue this job will reside. Jobs with a high priority will
	 * be grabbed before all others, and jobs with a normal priority will be
	 * grabbed before those with a low priority.<br>
	 * <br>
	 * Note: The priority level is not visible to the worker.
	 * 
	 * @author isaiah.v
	 */
	public static enum Priority {
		LOW_PRIORITY, NORMAL_PRIORITY, HIGH_PRIORITY
	}
	
	/**
	 * Defines the current state of a GearmanJob.<br>
	 * <br>
	 * <b>GearmanJob States:</b><br>
	 * <b>* NEW:</b> The job has been created. Only in this state can jobs be
	 * submitted to clients <br>
	 * <b>* SUBMITTED:</b> The job has been submitted to a client, but not to a
	 * job server<br>
	 * <b>* WORKING:</b> The job has been submitted to a job server. A
	 * GearmanFunction will receive the job in this state. Only in this state
	 * will callback methods be called by a remote worker<br>
	 * <b>* COMPLETE:</b> The job has finished execution, and the result of the
	 * execution is available<br>
	 * 
	 * @author isaiah.v
	 */
	public static enum State {
		
		/**
		 * The job has been created, but not submitted.
		 */
		NEW,
		
		/**
		 * The job has been submitted to a client but not to a job server.
		 */
		SUBMITTED,
		
		/**
		 * The job has been sent to the server and 
		 */
		WORKING,
		
		/**
		 * The job has finished
		 */
		COMPLETED
	}



	/** The job's current state */
	private State state = State.NEW;

	/** The job's result. This value is null until the job is completed*/
	private GearmanJobResult result = null;

	/** The unique ID used to identify the job within a function*/
	private final byte[] uniqueID;
	/** The name of the function that can execute this job */
	private final String function;
	/** The job data in the form of a byte[] */
	private final byte[] jobData;
	/** The job priority level. The higher the level, the sooner it will be pulled from the job queue */
	private final Priority priority;

	private WeakReference<ConnectionController<?,?>> connection;
	private ByteArray jobHandle;

	/**
	 * Sets the connection information.<br>
	 * <br>
	 * This method should only be called internally by the gearman API
	 * 
	 * @param conn
	 * 		The GearmanConnection connected to the job server
	 * @param jobHandle
	 * 		The job handle for this specific job
	 */
	final void setConnection(final ConnectionController<?,?> conn, final ByteArray jobHandle) {
		this.state = State.WORKING;
		this.connection = new WeakReference<ConnectionController<?,?>>(conn);
		this.jobHandle = jobHandle;
	}
	
	final synchronized boolean submit() {
		 if(!(this.state.equals(State.NEW) || this.state.equals(State.COMPLETED))) return false;
		 
		 this.state = State.SUBMITTED;
		 return true;
	}

	/**
	 * Gets the GearmanConnection if one is available.<br>
	 * <br>
	 * This method should only be called internally by the gearman API
	 * @return
	 * 		The GearmanConnection
	 */
	final GearmanConnection<?> getConnection() { 
		final ConnectionController<?,?> conn = connection==null? null: connection.get();
		return conn==null? null: conn.getConnection();
	}
	
	/**
	 * Gets the job's unique ID.<br>
	 * <br>
	 * This method should only be called internally by the gearman API
	 * @return
	 * 		The specified unique ID
	 */
	final byte[] getUniqueID() {
		return this.uniqueID;
	}
	
	/**
	 * Retrieves the job handle if one is available. While in the WORKING state,
	 * the job handle is available by both on the client and worker sides. 
	 * @return
	 * 		The job handle if one is available. If not, null is returned
	 */
	public final ByteArray getJobHandle() {
		return this.jobHandle;
	}

	/**
	 * Tests if there is a connection to the job server. This information may be
	 * useful to a {@link GearmanFunction} that performs long running jobs. If
	 * the connection to the server has been lost, it may be better to abandon
	 * the execution and free up the thread resource.
	 * 
	 * This value can only be true in the WORKING state.
	 * 
	 * @return true if and only if there is a live connection to the job server.
	 */
	public final boolean isConnected() {
		ConnectionController<?,?> conn = this.connection==null? null: this.connection.get();
		return conn==null ? false : conn.isOpen();
	}

	/**
	 * Returns this job's priority. When a job is pulled from the server's job queue
	 * depends on both the order it was added and the job's priory. Jobs with a higher
	 * priority are polled before those with a lower priority<br>
	 * <br>
	 * This method is only accessible on the client side.<br>
	 * 
	 * @return
	 * 		The job's priority level.
	 */
	protected final Priority getJobPriority() {
		return this.priority;
	}

	/**
	 * Returns the job's current state.  The available states are as follows:<br>
	 * <b>* NEW:</b> The job has been created. Only in this state can jobs be
	 * submitted to clients <br>
	 * <b>* SUBMITTED:</b> The job has been submitted to a client, but not to a
	 * job server<br>
	 * <b>* WORKING:</b> The job has been submitted to a job server. A
	 * GearmanFunction will receive the job in this state. Only in this state
	 * will callback methods be called by a remote worker<br>
	 * <b>* COMPLETE:</b> The job has finished execution, and the result of the
	 * execution is available<br>
	 * @return
	 * 		The job's current state
	 */
	public final State getJobState() {
		return this.state;
	}

	/**
	 * Creates a GearmanJob
	 * @param function
	 * 		The name of the gearman function that can execute this job
	 * @param jobData
	 * 		The data sent to the gearman function to be processed.
	 */
	protected GearmanJob(final String function, final byte[] jobData) {
		this(function, jobData, UUID.randomUUID().toString().getBytes(), Priority.NORMAL_PRIORITY);
	}

	/**
	 * Creates a GearmanJob
	 * @param function
	 * 		The name of the gearman function that can execute this job
	 * @param jobData
	 * 		The data sent to the gearman function to be processed.
	 * @param uniqueID
	 * 		Specifies the unique ID.
	 */
	GearmanJob(final String function, final byte[] jobData, final byte[] uniqueID) {
		this(function, jobData, uniqueID, Priority.NORMAL_PRIORITY);
	}

	/**
	 * Creates a GearmanJob
	 * @param function
	 * 		The name of the gearman function that can execute this job
	 * @param jobData
	 * 		The data sent to the gearman function to be processed.
	 * @param uniqueID
	 * 		Specifies the unique ID.
	 * @param priority
	 * 		Specifies the jobs priority level
	 */
	GearmanJob(final String function, final byte[] jobData, final byte[] uniqueID, final Priority priority) {
		if (function == null || jobData == null || uniqueID == null || priority == null) {
			throw new IllegalArgumentException("Paramiter equals null");
		}

		this.function = function;
		this.jobData = jobData;
		this.uniqueID = uniqueID;
		this.priority = priority;
	}

	/**
	 * Creates a GearmanJob
	 * @param function
	 * 		The name of the gearman function that can execute this job
	 * @param jobData
	 * 		The data sent to the gearman function to be processed.
	 * @param priority
	 * 		Specifies the jobs priority level
	 */
	protected GearmanJob(final String function, final byte[] jobData,
			final Priority priority) {
		this(function, jobData, UUID.randomUUID().toString().getBytes(),
				priority);
	}

	/**
	 * The data callback channel.<br>
	 * <br>
	 * When the {@link GearmanFunction} receives a GearmanJob, this method can
	 * used to send information to the client while the job is executing<br>
	 * <br>
	 * Client Side: Override this method to define how information received on
	 * this callback channel should be handled. If this method is not
	 * overwritten, the information will be dropped.<br>
	 * <br>
	 * Worker Side: This method will throw a IllegalStateException if the job
	 * has completed. Only while the job is executing can the callback methods
	 * be used.<br>
	 * 
	 * @param data
	 *            Information to send on the data callback channel
	 */
	public abstract void callbackData(byte[] data);

/*	/**
	 * The exception callback channel. The exception callback channel should be
	 * used to notify the client of any exceptions. The exception channel is
	 * closed by default. To open it, specify with the {@link GearmanClient} that
	 * you would like to exceptions to be forwarded.<br>
	 * <br>
	 * When the {@link GearmanFunction} receives a GearmanJob, this method can
	 * used to send information to the client while the job is executing<br>
	 * <br>
	 * Client Side: Override this method to define how information received on
	 * this callback channel should be handled. If this method is not
	 * overwritten, the information will be dropped.<br>
	 * <br>
	 * Worker Side: This method will throw a IllegalStateException if the job
	 * has completed. Only while the job is executing can the callback methods
	 * be used.<br>
	 * 
	 * @param data
	 *            Information to send on the data callback channel
	 */
//	public abstract void callbackException(byte[] exception);

	/**
	 * The warning callback channel. This is just like the data callback channel,
	 * but its purpose is to send warning information.<br>
	 * <br>
	 * When the {@link GearmanFunction} receives a GearmanJob, this method can
	 * used to send information to the client while the job is executing<br>
	 * <br>
	 * Client Side: Override this method to define how information received on
	 * this callback channel should be handled. If this method is not
	 * overwritten, the information will be dropped.<br>
	 * <br>
	 * Worker Side: This method will throw a IllegalStateException if the job
	 * has completed. Only while the job is executing can the callback methods
	 * be used.<br>
	 * 
	 * @param data
	 *            Information to send on the data callback channel
	 */
	public abstract void callbackWarning(byte[] warning);

	/**
	 * Returns the name of the gearman function that can execute this job 
	 * @return
	 * 		The name of the gearman function that can execute this job
	 */
	public final String getFunctionName() {
		return function;
	}

	public final byte[] getJobData() {
		return this.jobData;
	}

	/**
	 * The onComplete method is triggered when the {@link GearmanJobResult} becomes
	 * available.
	 * 
	 * @param result
	 * 		The result from executing this GearmanJob
	 */
	protected abstract void onComplete(GearmanJobResult result);

	final void setResult(GearmanJobResult result) {
		this.result = result;
		this.state = State.COMPLETED;
		
		this.onComplete(result);
	}

	/**
	 * Updates the client of the job progress.<br>
	 * <br>
	 * When the {@link GearmanFunction} receives a GearmanJob, this method can
	 * used to send information about the jobs progress back to the client.<br>
	 * <br>
	 * Client Side: Override this method to define how information received on
	 * this callback channel should be handled. If this method is not
	 * overwritten, the information will be dropped.<br>
	 * <br>
	 * Worker Side: This method will throw a IllegalStateException if the job
	 * has completed. Only while the job is executing can the callback methods
	 * be used.<br>
	 * @param numerator
	 * 		A number typically specifying the numerator in the fraction work that's
	 * 		completed 
	 * @param denominator
	 * 		A number typically specifying the denominator in the fraction work that's
	 * 		completed
	 */
	public abstract void callbackStatus(long numerator, long denominator);

	/**
	 * Tests if this job is completed.
	 * @return
	 * 		returns true if this job is in the COMPLETED state
	 */
	public final boolean isComplete() {
		return this.state.equals(State.COMPLETED);
	}

	/**
	 * Attempts to return the result.  If the result is not yet available, null is returned.
	 * @return
	 * 		The result if available. null is returned if the result is not available. 
	 */
	public final GearmanJobResult getGearmanJobResult() {
		return this.result;
	}
	
	public interface JobStatusCallbackHandler extends GearmanCallbackHandler<GearmanJob, StatusResult> {
		
	}
	
	public final void getStatus(JobStatusCallbackHandler callback) {
		if(this.isComplete()) {
			callback.onComplete(this, new StatusResult() {
				public GearmanJobStatus getGearmanJobStatus() { return null; }
				public StatusCallbackResult getStatusCallbackResult() { return StatusCallbackResult.WORK_COMPLETE; }
				public boolean isSuccessful() { return false; }
			});
			return;
		}
		
		// Get the connection controller that is handling this server
		final ConnectionController<?,?> cc = this.connection==null? null: this.connection.get();
		
		if(cc==null) {
			callback.onComplete(this, new StatusResult() {
				public GearmanJobStatus getGearmanJobStatus() { return null; }
				public StatusCallbackResult getStatusCallbackResult() { return StatusCallbackResult.SERVER_NOT_AVAILABLE; }
				public boolean isSuccessful() { return false; }
			});
			return;
		}
		
		// If the connection is defined, then the job handle should be defined
		assert this.jobHandle != null;
		
		// Get the controller get the status. This method returns a GearmanJobStatus/Future object 
		cc.getStatus(this.getJobHandle(), new JobStatus(this, callback));
	}
}
