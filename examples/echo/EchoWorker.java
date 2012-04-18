package echo;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;

/**
 * 
 * @author isaiah
 *
 */
public class EchoWorker implements GearmanFunction {
	
	/** The echo function name */
	public static final String ECHO_FUNCTION_NAME = "echo";
	
	/** The host address of the job server */
	public static final String ECHO_HOST = "localhost";
	
	/** The port number the job server is listening on */
	public static final int ECHO_PORT = 4730;

	/**
	 * Creates an echo worker. A worker receives jobs from the job server distributes them
	 * to its registered functions.
	 * 
	 * @param args
	 * 		N/A
	 * @throws IOException
	 * 		If an IO exception occurs during execution
	 */
	public static void main(String... args) {
		
		// Create a new gearman instance
		Gearman gearman = Gearman.createGearman();
		
		// Create the job server object. This call creates an object represents a remote job server
		GearmanServer server = gearman.createGearmanServer(EchoWorker.ECHO_HOST, EchoWorker.ECHO_PORT);
		
		// Create a gearman worker
		GearmanWorker worker = gearman.createGearmanWorker();
		
		// Tell the worker how to perform the echo function
		worker.addFunction(EchoWorker.ECHO_FUNCTION_NAME, new EchoWorker());
		
		
		// Tell the worker that it may communicate with the given job server
		worker.addServer(server);
	}
	
	@Override
	public byte[] work(String function, byte[] data, GearmanFunctionCallback callback) throws Exception {
		
		/*
		 * The work method performs the gearman function. In this case, the echo function simply returns
		 * the data it received
		 */
		
		return data;
	}

}
