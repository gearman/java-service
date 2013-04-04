package org.gearman.impl.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class GearmanThreadFactory implements ThreadFactory {

	private static final String NAME_PREFIX = "gearman-";
	private final ThreadGroup group;
	private final AtomicInteger count = new AtomicInteger(1);
	
	public GearmanThreadFactory() {
		SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
	}
	
	@Override
	public Thread newThread(Runnable r) {
		Thread value = new Thread(group,r,NAME_PREFIX+(count.getAndIncrement()));
		
		if (value.isDaemon())
			value.setDaemon(false);
		if (value.getPriority() != Thread.NORM_PRIORITY)
			value.setPriority(Thread.NORM_PRIORITY);
		
		return value;
	}

}
