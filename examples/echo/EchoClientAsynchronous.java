package echo;

import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobEventCallback;
import org.gearman.GearmanJoin;
import org.gearman.GearmanServer;

/**
 * The echo client submits an "echo" job to a job server and prints the final
 * result. It's the "Hello World" of the java-geraman-service
 * 
 * The echo example illustrates how send a single job and get the result
 */
public class EchoClientAsynchronous implements GearmanJobEventCallback<String> {

	public static void main(String... args) throws InterruptedException {

		/*
		 * Create a Gearman instance
		 */
		final Gearman gearman = Gearman.createGearman();

		/*
		 * Create a new gearman client.
		 * 
		 * The client is used to submit requests the job server.
		 */
		GearmanClient client = gearman.createGearmanClient();

		/*
		 * Create the job server object. This call creates an object representing
		 * a remote job server.
		 * 
		 * Parameter 1: the host address of the job server.
		 * Parameter 2: the port number the job server is listening on.
		 * 
		 * A job server receives jobs from clients and distributes them to
		 * registered workers.
		 */
		GearmanServer server = gearman.createGearmanServer(
				EchoWorker.ECHO_HOST, EchoWorker.ECHO_PORT);

		/*
		 * Tell the client that it may connect to this server when submitting
		 * jobs.
		 */
		client.addServer(server);

		/*
		 * Submit a job to a job server. This submit method uses an
		 * asynchronous callback object to process the job's result 
		 * 
		 * Parameter 1: the gearman function name
		 * Parameter 2: the data passed to the server and worker
		 * Parameter 3: an attachment returned through the callback
		 * Parameter 4: the callback used to process the job events
		 * 
		 * The GearmanJoin object is used to block the current thread
		 * until the end-of-file has been reached.
		 */
		GearmanJoin<String> join = client.submitJob(
				EchoWorker.ECHO_FUNCTION_NAME, ("Hello World").getBytes(),
				EchoWorker.ECHO_FUNCTION_NAME, new EchoClientAsynchronous());

		
		/*
		 * Block the current thread until all events have been processed.
		 */
		join.join();

		
		/*
		 * After the job has been completely processed. We close the service
		 * 
		 * It's suggested that you reuse Gearman and GearmanClient instances
		 * rather recreating and closing new ones between submissions
		 */
		gearman.shutdown();
	}

	@Override
	public void onEvent(String attachment, GearmanJobEvent event) {

		/*
		 * This method is called by the client when an event is received
		 */
		
		switch (event.getEventType()) {
		case GEARMAN_JOB_SUCCESS: // Job completed successfully
			System.out.println(new String(event.getData()));
			break;
		case GEARMAN_SUBMIT_FAIL: // The job submit operation failed
		case GEARMAN_JOB_FAIL: // The job's execution failed
			System.err.println(event.getEventType() + ": "
					+ new String(event.getData()));
		default:
		}

	}
}
