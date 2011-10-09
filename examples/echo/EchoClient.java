package echo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;

import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJob;
import org.gearman.GearmanJobResult;
import org.gearman.GearmanClient.GearmanSubmitHandler;
import org.gearman.GearmanClient.SubmitCallbackResult;
import org.gearman.core.GearmanConstants;

public class EchoClient {

	public static void main(String[] args) throws IOException {
		
		/*
		 * Create a Gearman instance
		 */
		final Gearman gearman = new Gearman();
		
		/*
		 * Create a new GearmanClient
		 */
		final GearmanClient client = gearman.createGearmanClient();
		
		/*
		 * Tell the client that it can connect to a job server on the localhost listening 
		 * on the default port. The address is only added to a list. The client
		 * implementation will decide when the connection needs to be active
		 *  
`		 * See the method "setLostConnectionPolicy(GearmanLostConnectionPolicy)"
		 * for information about setting connection failure actions
		 */
		client.addServer(new InetSocketAddress("localhost",GearmanConstants.DEFAULT_PORT));
		
		
		/*
		 * Define an asynchronous callback handler. The callback handler will tell the user if the
		 * job was successfully submitted to a job server or if it failed
		 */
		final GearmanSubmitHandler callback = new GearmanSubmitHandler() {
			@Override
			public void onComplete(GearmanJob job, SubmitCallbackResult result) {
				
				// If the submit was successfully submitted, then we just return
				if(result.isSuccessful()) return;
				
				/*
				 *  If the submit failed to submit, print an error and close the gearman
				 *  instance, and allow the application to shutdown
				 */
				System.err.println("job submission failed: "+result.name());
				gearman.shutdown();
			}
		};
		
		/*
		 * Define a GearmanJob to be executed by a GearmanWorker.
		 */
		final GearmanJob job = new GearmanJob("echo", "Hello World".getBytes()) {

			@Override
			public void callbackStatus(long numerator, long denominator) {
				/*
				 * This method is used to send status updates from the worker to the
				 * client while the job is executing
				 */
				
				// No information about the job's status is sent in the echo function 
				assert false;
			}
			
			@Override
			public void callbackData(byte[] data) {
				/*
				 * This method is used to send intermediate data from the worker to the
				 * client while the job is executing.  
				 */
				
				// No data sent on the data callback channel in the echo function
				assert false;
			}
			
			@Override
			public void callbackWarning(byte[] warning) {
				/*
				 *  This method is used to send warning information from the worker to the
				 *  client while the job is executing.
				 */

				// No data sent on the warning callback channel in the echo function
				assert false;
			}
			
			@Override
			protected void onComplete(GearmanJobResult result) {
				// Called when the result has been sent back to the client
				
				// If the job was successful
				if(result.isSuccessful()) {
					// If the job was successful, print the returned string
					System.out.println(new String(result.getData(),GearmanConstants.UTF_8));
				} else {
					// If the job failed, print that it failed.  A job may fail for a few resins,
					// but the most likely would be due to failing to send this job to a job server
					System.err.println("Job execution failed: "+result.getJobCallbackResult().name());
				}
				
				// We're done, shutdown
				gearman.shutdown();
			}
		};
		
		/*
		 * Submit the GearmanJob
		 */
		client.submitJob(job, callback);
		
	} // exit main thread
}
