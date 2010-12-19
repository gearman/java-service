package echo;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanJob;
import org.gearman.GearmanJobResult;
import org.gearman.GearmanLostConnectionPolicy;
import org.gearman.GearmanServer;
import org.gearman.GearmanJobServerPool;
import org.gearman.GearmanWorker;
import org.gearman.core.GearmanVariables;

/**
 * A simple worker implementation that echos Strings.
 * @author isaiah
 */
public class EchoWorker implements GearmanFunction {
	
	/**
	 * Starts a gearman worker that performs the function "echo"
	 * 
	 * @param args
	 * 		not applicable
	 * @throws IOException
	 * 		thrown if an IOException is thrown while creating the Gearman object 
	 */
	public static void main(String[] args) throws IOException {

		/*
		 *  Create a Gearman service provider.
		 *  
		 */
		final Gearman gearman = new Gearman();
		
		/*
		 *  Create a GearmanWorker
		 */
		final GearmanWorker worker = gearman.createGearmanWorker();
		
		worker.setLostConnectionPolicy(new GearmanLostConnectionPolicy() {

			@Override
			public void lostLocalServer(GearmanServer server, GearmanJobServerPool service, Grounds grounds) {
				assert false;
			}

			@Override
			public Action lostRemoteServer(InetSocketAddress adrs, GearmanJobServerPool service, Grounds grounds) {
				System.out.println(grounds);
				
				gearman.shutdown();
				return Action.dropServer();
			}
			
		});
		
		/*
		 *  This method tells the worker how to perform functions. Here we're telling 
		 */
		worker.addFunction("echo", new EchoWorker());
		
		// Connect to a Gearman server, no in the local address space, to poll GearmanJobs from 
		worker.addServer(new InetSocketAddress("localhost", 4730));
		
		// Create a shutdown hook for clean exit
		final Runnable shutdownHook = new Runnable() {
			@Override
			public void run() {
				System.out.println("Shutting Down");
				gearman.shutdown();
			}
		};
		
		// Register shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(shutdownHook));
	}
	
	
	/**
	 * Echos a string sent from a client
	 * 
	 * @param job
	 * 		The job sent from the client to be performed by this worker
	 * @return
	 * 		The job data sent from the client
	 */
	@Override
	public GearmanJobResult work(GearmanJob job) {
		// Print the string
		System.out.println(new String(job.getJobData(), GearmanVariables.UTF_8));
		// Return data to send back to client
		return GearmanJobResult.workSuccessful(job.getJobData());
	}

}
