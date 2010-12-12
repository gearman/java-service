package org.gearman;


/**
 * A GearmanFunction provides the interface to creates function that can be executed
 * by the {@link GearmanWorker}.
 * 
 * @author isaiah
 */
public interface GearmanFunction {

	/**
	 * The implementation of a gearman function. A GearmanFunction is registered
	 * with a {@link GearmanWorker} to let it know when a job for this function
	 * come in, to execute this method.<br>
	 * <br>
	 * If a runtime exception is thrown while executing the function. The job is
	 * automatically put into the "completed" state and a FAIL response is
	 * returned to the client.<br>
	 * <br>
	 * If a null value is returned, it is assumed the execution was successful
	 * but no data is to be sent back to the client.<br>
	 * <br>
	 * Once this method has returned, the GearmanJob parameter is put into a
	 * "completed" state, any attempt to use the callback methods after the job
	 * has been put into this state, an IllegalStateException will be thrown.
	 * 
	 * @param job
	 * <br>
	 *            The GearmanJob provides information needed to run the
	 *            function. It also provides callback functionality, allowing
	 *            the function to send data back to the client while the job is
	 *            running.<br>
	 * <br>
	 *            It is important to know that once the function has finished,
	 *            the GearmanJob is considered "completed" and the callback
	 *            functionality is no longer valid. Any attempt to send callback
	 *            data once in the completed state, and IllegalStateException is
	 *            thrown <br>
	 * @return Once this method has returned, the state of the job is
	 *         "completed" and the result is forwarded to the client. A return
	 *         value of null implies a successful execution with no data to
	 *         return.
	 */
	public GearmanJobResult work(GearmanJob job);
}
