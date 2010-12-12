package temp;

import java.io.IOException;

import org.gearman.Gearman;
import org.gearman.GearmanFunction;
import org.gearman.GearmanJob;
import org.gearman.GearmanJobResult;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;
import org.gearman.core.GearmanCompletionHandler;

public class Waiter implements GearmanFunction {
	public static void main(String[] args) throws IOException {
		final Gearman g = new Gearman();
		
		final GearmanServer s = g.createGearmanServer();
		s.openPort();
		
		final GearmanWorker w = g.createGearmanWorker();
		w.setMaximumConcurrency(Integer.MAX_VALUE);
		w.addFunction("wait", new Waiter());
		w.addServer(s, null, new GearmanCompletionHandler<Object>() {
			public void onComplete(Object attachment) { }
			public void onFail(Throwable exc, Object attachment) {
				exc.printStackTrace();
			}
		});
	}

	@Override
	public GearmanJobResult work(GearmanJob job) {
		synchronized(this) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	
	
}
