package echo;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanJob;
import org.gearman.GearmanJobResult;
import org.gearman.GearmanWorker;
import org.gearman.core.GearmanSettings;

/**
 * An echo worker receives a string from a job server an immediately returns it.<br>
 * <br>
 * This class has two parts:<br>
 * <br>
 * 1) It implements the {@link GearmanFunction} interface to define how to echo a string.<br>
 * <br>
 * 2) The main method sets up a {@link GearmanWorker} to use the defined {@link GearmanFunction} to
 * echo strings. For simplicity, the {@link GearmanWorker} is only set to connect to the localhost.
 *  
 * @author isaiah
 */
public class EchoWorker implements GearmanFunction {
	
	/**
	 * sets up a {@link GearmanWorker} to use the defined {@link GearmanFunction} to
	 * echo strings.<br?
	 * 
	 * @param args
	 * 		Not applicable. 
	 * @throws IOException
	 * 		thrown if an IOException is thrown while creating the Gearman object 
	 */
	public static void main(String[] args) throws IOException {
		
		/*
		 *  Create a Gearman instance
		 */
		final Gearman gearman = new Gearman();
		
		/*
		 *  Create a new GearmanWorker
		 */
		final GearmanWorker worker = gearman.createGearmanWorker();
				
		/*
		 *  Tell the worker that it can connect to a job server on the localhost listening 
		 *  on the default port. The address is added to a list
		 *  
		 *  See the method "setLostConnectionPolicy(GearmanLostConnectionPolicy)"
		 *  for information about setting connection failure actions
		 */
		worker.addServer(new InetSocketAddress("localhost", GearmanSettings.DEFAULT_PORT));
		
		/*
		 *  Tell the worker how to perform the function "echo" by passing it a
		 *  GearmanFunction that echos strings
		 */
		worker.addFunction("echo", new EchoWorker());
	}
	
	/**
	 * Echos a string sent from a client
	 * 
	 * @param job
	 * 		The job sent from the client to be performed by this worker
	 * @return
	 * 		The result from executing this function
	 */
	@Override
	public GearmanJobResult work(GearmanJob job) {
		
		// Print the string
		System.out.println("Echo: "+new String(job.getJobData(), GearmanSettings.UTF_8));
		
		// Return data to send back to client
		return GearmanJobResult.workSuccessful(job.getJobData());
	}

}
