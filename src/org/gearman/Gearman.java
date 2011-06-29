package org.gearman;

import java.io.IOException;
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
		this(1);
	}
	
	private Gearman(final int coreThreads) throws IOException {
		ThreadPoolExecutor exe = new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		exe.allowCoreThreadTimeOut(false);
		exe.prestartCoreThread();
		
		final Scheduler s = new Scheduler(exe);
		
		this.pool = s;
		
		this.gcm = new GearmanConnectionManager(this.pool);
	}
		
	public final GearmanServer createGearmanServer() {
		final GearmanServer server = new ServerImpl(this);
		this.services.add(server);
		return server;
	}
	public final GearmanClient createGearmanClient() {
		final GearmanClient client = new ClientImpl(this);
		this.services.add(client);
		return client;
	}
	public final GearmanWorker createGearmanWorker() {
		final GearmanWorker worker = new WorkerImpl(this);
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
	
	final void onServiceShutdown(GearmanService service) {
		this.services.remove(service);
	}
}
