package echo;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

/**
 * The echo worker/server starts a new server and polls jobs from it job server
 * 
 * The echo worker illustrates how to setup a basic worker
 */
public class EchoWorkerServer {
	public static void main(String... args) throws IOException {

		/*
		 * Create a Gearman instance
		 */
		Gearman gearman = Gearman.createGearman();

		try {

			/*
			 * Start a new job server. The resulting server will be running in
			 * the local address space.
			 * 
			 * Parameter 1: The port number to listen on
			 * 
			 * throws IOException
			 */
			GearmanServer server = gearman
					.startGearmanServer(EchoWorker.ECHO_PORT);

			/*
			 * Create a gearman worker. The worker poll jobs from the server and
			 * executes the corresponding GearmanFunction
			 */
			GearmanWorker worker = gearman.createGearmanWorker();

			/*
			 *  Tell the worker how to perform the echo function
			 */
			worker.addFunction(EchoWorker.ECHO_FUNCTION_NAME, new EchoWorker());

			/*
			 *  Tell the worker that it may communicate with the given job server
			 */
			worker.addServer(server);

		} catch (IOException ioe) {

			/*
			 * If an exception occurs, make sure the gearman service is shutdown
			 */
			gearman.shutdown();

			// forward exception
			throw ioe;
		}
	}
}
