package echo;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

public class EchoWorkerServer {
	public static void main(String...args) throws IOException {
		
		// Create a new gearman instance
		Gearman gearman = Gearman.createGearman();
		
		try {
			
			// Create the job server object. This call starts a local job server. Throws IOException
			GearmanServer server = gearman.startGearmanServer(EchoWorker.ECHO_PORT);

			// Create a gearman worker
			GearmanWorker worker = gearman.createGearmanWorker();
			
			// Tell the worker how to perform the echo function
			worker.addFunction(EchoWorker.ECHO_FUNCTION_NAME, new EchoWorker());
			
			// Tell the worker that it may communicate with the given job server
			worker.addServer(server);
			
		} catch (IOException ioe) {
			
			// If an exception occurs, make sure the gearman service is shutdown
			gearman.shutdown();
			
			// forward exception
			throw ioe;
		}
	}
}
