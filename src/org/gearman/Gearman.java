package org.gearman;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.gearman.core.GearmanConnectionManager;
import org.gearman.util.Scheduler;

/**
 * A <code>Gearman</code> object defines a gearman systems and creates gearman
 * services. These services include {@link GearmanWorker}s,
 * {@link GearmanClient}s, and {@link GearmanServer}s. All services created by
 * the same <code>Gearman</code> object are said to be in the same system, and
 * all services in the same system share system wide thread resources.
 * 
 * @author isaiah.v
 */
public final class Gearman implements GearmanService {
	private final GearmanConnectionManager gcm;
	private final LinkedBlockingQueue<GearmanService> services = new LinkedBlockingQueue<GearmanService>();
	
	private final ScheduledExecutorService pool;
	
	public Gearman() throws IOException {
		this.pool = new Scheduler(Executors.newCachedThreadPool());
		this.gcm = new GearmanConnectionManager(this.pool);
	}
	
	public Gearman(final int coreThreads) throws IOException {
		this.pool = new Scheduler(new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>()));
		this.gcm = new GearmanConnectionManager(this.pool);
	}
		
	public final GearmanServer createGearmanServer() {
		final GearmanServer server = new ServerImpl(this);
		this.services.add(server);
		return server;
	}
	public final GearmanClient createGearmanClient() {
		return null;
	}
	public final GearmanWorker createGearmanWorker() {
		final GearmanWorker worker = new WorkerImplNew(this);
		this.services.add(worker);
		
		return worker;
	}
	
	final GearmanConnectionManager getGearmanConnectionManager() {
		return gcm;		
	}

	@Override
	public Gearman getGearman() {
		return this;
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public void shutdown() {
		for(GearmanService service : this.services) {
			service.shutdown();
		}
		
		this.gcm.shutdown();
	}
	
	final ScheduledExecutorService getPool() {
		return this.pool;
	}
}
