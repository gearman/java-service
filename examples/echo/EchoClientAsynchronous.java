package echo;

import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventCallback;
import org.gearman.GearmanJoin;
import org.gearman.GearmanServer;

public class EchoClientAsynchronous implements GearmanJobEventCallback<String> {
	
	public static void main(String...args) throws InterruptedException {
		
		// Create a new gearman instance
		final Gearman gearman = Gearman.createGearman();
		
		// Create the job server object. This call creates an object represents a remote job server
		GearmanServer server = gearman.createGearmanServer(EchoWorker.ECHO_HOST, EchoWorker.ECHO_PORT);
		
		GearmanClient client = gearman.createGearmanClient();
		
		client.addServer(server);
		
		GearmanJoin<String> join = client.submitJob(
				EchoWorker.ECHO_FUNCTION_NAME,
				("Hello World").getBytes(),
				EchoWorker.ECHO_FUNCTION_NAME,
				new EchoClientAsynchronous()
			);
		
		join.join();
		
		gearman.shutdown();
	}

	@Override
	public void onEvent(String functionName, GearmanJobEvent event) {
		
		switch(event.getEventType()) {
		case GEARMAN_JOB_SUCCESS:	// Job completed successfully
			System.out.println(new String(event.getData()));
			break;
		case GEARMAN_SUBMIT_FAIL:	// The job submit operation failed
		case GEARMAN_JOB_FAIL:		// The job's execution failed
			System.err.println(event.getEventType() + ": " + new String(event.getData()));
		}
		
	}
}
