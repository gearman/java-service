package echo;

import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobReturn;
import org.gearman.GearmanServer;

/**
 * The echo client submits an "echo" job to a job server and prints the final
 * result. It's the "Hello World" of the java-geraman-service
 * 
 * The echo example illustrates how send a single job and get the result
 */
public class EchoClient {

	public static void main(String... args) throws InterruptedException {

		/*
		 * Create a Gearman instance
		 */
		Gearman gearman = Gearman.createGearman();

		/*
		 * Create a new gearman client.
		 * 
		 * The client is used to submit requests the job server.
		 */
		GearmanClient client = gearman.createGearmanClient();

		/*
		 * Create the job server object. This call creates an object represents
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
		 * Submit a job to a job server.
		 * 
		 * Parameter 1: the gearman function name
		 * Parameter 2: the data passed to the server and worker
		 * 
		 * The GearmanJobReturn is used to poll the job's result
		 */
		GearmanJobReturn jobReturn = client.submitJob(
				EchoWorker.ECHO_FUNCTION_NAME, ("Hello World").getBytes());

		/*
		 * Iterate through the job events until we hit the end-of-file
		 */
		while (!jobReturn.isEOF()) {

			// Poll the next job event (blocking operation)
			GearmanJobEvent event = jobReturn.poll();

			switch (event.getEventType()) {

			// success
			case GEARMAN_JOB_SUCCESS: // Job completed successfully
				// print the result
				System.out.println(new String(event.getData()));
				break;

			// failure
			case GEARMAN_SUBMIT_FAIL: // The job submit operation failed
			case GEARMAN_JOB_FAIL: // The job's execution failed
				System.err.println(event.getEventType() + ": "
						+ new String(event.getData()));
			default:
			}

		}

		/*
		 * Close the gearman service after it's no longer needed. (closes all
		 * sub-services, such as the client)
		 * 
		 * It's suggested that you reuse Gearman and GearmanClient instances
		 * rather recreating and closing new ones between submissions
		 */
		gearman.shutdown();
	}
}
