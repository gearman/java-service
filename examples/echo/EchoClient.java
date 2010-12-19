package echo;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJob;
import org.gearman.GearmanJobResult;
import org.gearman.core.GearmanCompletionHandler;
import org.gearman.core.GearmanVariables;

public class EchoClient {
	
	public static void main(String[] args) throws IOException {
		
		// Create new Gearman instance
		final Gearman gearman = new Gearman();
		
		// Create client instance
		final GearmanClient client = gearman.createGearmanClient();
		
		// Add server
		client.addServer("localhost", 4730, null, new GearmanCompletionHandler<Object>(){
			@Override
			public void onComplete(Object attachment) {
				// Called after we have successfully connected to the specified job server.
				// If successful, there is nothing to do
			}
			@Override
			public void onFail(Throwable exc, Object attachment) {
				// Called if adding the server fails.

				// If we fail to connect to the server, we're done. So we need to shutdown
				gearman.shutdown();
			}
		});
		
		
		/*
		 * Submit a job to be executed by the function "echo" which will echo the given string,
		 * "Hello World"
		 * 
		 * TODO make sure that if a server is in the process of connecting, we don't fail the
		 * job until
		 * 
		 * Though the GearmanClient may still be connecting to a job server, the job will not fail
		 * until the attempt to connect fails.
		 */
		client.submitJob(new GearmanJob("echo", "Hello World".getBytes(GearmanVariables.UTF_8)) {
			@Override
			public void callbackData(byte[] data) {
				// Called when data is sent on the data callback channel
				// No data sent on the data callback channel in the echo function
			}
			@Override
			public void callbackException(byte[] exception) {
				// Called when data is sent on the exception callback channel
				// No data sent on the exception callback channel in the echo function
			}
			@Override
			public void callbackWarning(byte[] warning) {
				// Called when data is sent on the warning callback channel 
				// No data sent on the warning callback channel in the echo function
			}
			@Override
			protected void onComplete(GearmanJobResult result) {
				// Called when the result has been sent back to the client
				
				// If the job was successful
				if(result.isSuccessful()) {
					// If the job was successful, print the returned string
					System.out.println(new String(result.getResultData(),GearmanVariables.UTF_8));
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
