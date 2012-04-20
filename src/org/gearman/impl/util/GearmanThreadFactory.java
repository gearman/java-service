package org.gearman.impl.util;

import java.util.concurrent.ThreadFactory;

public class GearmanThreadFactory implements ThreadFactory {

	private static final String NAME_PREFIX = "gearman-";
	private int count = 1;
	
	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r,NAME_PREFIX+(count++));
	}

}
