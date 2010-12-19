package echo;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

/**
 * 
 * @author isaiah
 *
 */
public class EchoWorkerServer {
	
	public static void main(String[] args) throws IOException {
		
		// Create new Gearman object
		final Gearman gearman = new Gearman();
		
		// Create a local Gearman server
		final GearmanServer server = gearman.createGearmanServer();
		
		// Open the default port number
		try {
			server.openPort();
		} catch(IOException ioe) {
			System.out.println(ioe);
			gearman.shutdown();
			return;
		}
		
		// Create a new Gearman worker
		final GearmanWorker worker = gearman.createGearmanWorker();
		
		// Connect to the local server
		worker.addServer(server);
		
		// Tell the worker how to do the "echo" function
		worker.addFunction("echo", new EchoWorker());
		
		// Create a shutdown hook for clean exit
		final Runnable shutdownHook = new Runnable() {
			@Override
			public void run() {
				gearman.shutdown();
			}
		};
		
		// Register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
	}
}
