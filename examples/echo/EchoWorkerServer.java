package echo;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

/**
 * 
 * @author isaiah
 */
public class EchoWorkerServer {
	
	public static void main(String[] args) throws IOException {
		
		/*
		 *  Create a Gearman instance
		 */
		final Gearman gearman = new Gearman();
		
		/*
		 * Create a new job server that runs in the local address space
		 */
		final GearmanServer server = gearman.createGearmanServer();
		
		try {
			
			/*
			 *  Tell the server to listen on the default port (4730)
			 */
			server.openPort();
			
		} catch(IOException ioe) {
			
			/*
			 *  If we fail to open the port, we'll terminate the application  
			 */
			
			// Print the problem
			System.err.println(ioe);
			
			// Shutdown the gearman service
			gearman.shutdown();
			
			// exit main thread
			return;
		}
		
		/*
		 *  Create a new GearmanWorker
		 */
		final GearmanWorker worker = gearman.createGearmanWorker();
		
		/*
		 *  Tell the worker that it can communicate with the server running in
		 *  the local address space. Communication is done locally, without a
		 *  TCP socket
		 *  
		 *  See the method "setLostConnectionPolicy(GearmanLostConnectionPolicy)"
		 *  for information about getting connection failure notification
		 */
		worker.addServer(server);
		
		/*
		 *  Tell the worker how to perform the function "echo" by passing it a
		 *  GearmanFunction that echos strings
		 */
		worker.addFunction("echo", new EchoWorker());
	}
}
