package echo;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJob;
import org.gearman.GearmanJobResult;
import org.gearman.core.GearmanSettings;

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
		client.addServer(new InetSocketAddress("localhost",GearmanSettings.DEFAULT_PORT));
		
		/*
		 * Submit a GearmanJob to be executed by a worker who knows how to execute the
		 * function "echo".
		 */
		client.submitJob(new GearmanJob("echo", "Hello World".getBytes(GearmanSettings.UTF_8)) {
			
			@Override
			public void callbackData(byte[] data) {
				/*
				 *  This method is used to send intermediate data from the worker to the
				 *  client while the job is executing.  
				 */
				
				// No data sent on the data callback channel in the echo function
				assert false;
			}
			
			@Override
			public void callbackException(byte[] exception) {
				/*
				 *  This method is used to send exception information from the worker to the
				 *  client while the job is executing.
				 *  
				 *  By default, the exception callback channel is closed. To open, call
				 *  client.setExceptionChannel(true);
				 */
				
				// No data sent on the exception callback channel in the echo function
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
					System.out.println(new String(result.getResultData(),GearmanSettings.UTF_8));
				} else {
					// If the job failed, print that it failed.  A job may fail for a few resins,
					// but the most likely would be due failing to send it to a job server
					System.out.println("job failed");
				}
				
				// We're done, shutdown
				gearman.shutdown();
			}
		});
	}
}
