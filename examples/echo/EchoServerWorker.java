package echo;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanJob;
import org.gearman.GearmanJobResult;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;
import org.gearman.core.GearmanCompletionHandler;
import org.gearman.core.GearmanVariables;

public class EchoServerWorker implements GearmanFunction {
	
	public static void main(String[] args) throws IOException {
		// Create new Gearman object
		final Gearman gearman = new Gearman();
		
		// Create a local Gearman server
		final GearmanServer server = gearman.createGearmanServer();
		// Open the default port number
		try {
			server.openPort();
		} catch(IOException ioe) {
			ioe.printStackTrace();
			gearman.shutdown();
			return;
		}
		
		// Create a new Gearman worker
		final GearmanWorker worker = gearman.createGearmanWorker();
		// Tell the worker how to do the "echo" function
		worker.addFunction("echo", new EchoServerWorker());
		// Connect to the local server
		worker.addServer(server, null, new GearmanCompletionHandler<Object>() {
			public void onComplete(Object attachment) {}
			public void onFail(Throwable exc, Object attachment) {
				exc.printStackTrace();
				gearman.shutdown();
			}
		});
		
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

	@Override
	public GearmanJobResult work(GearmanJob job) {
		System.out.println(new String(job.getJobData(), GearmanVariables.UTF_8));
		return GearmanJobResult.workSuccessful(job.getJobData());
	}
}
