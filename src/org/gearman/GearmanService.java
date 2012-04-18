/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

/**
 * All Gearman services implements the {@link GearmanService} interface
 * @author isaiah
 */
public interface GearmanService {
	
	/**
	 * Closes the gearman service and all services created or maintained by this service
	 */
	public void shutdown();
	
	/**
	 * Tests if this gearman service has been shutdown
	 * @return
	 * 		<code>true</code> if this gearman service is shutdown
	 */
	public boolean isShutdown();
	
	/**
	 * Returns the creating {@link Gearman} instance
	 * @return
	 * 		The creating {@link Gearman} instance
	 */
	public Gearman getGearman();
}
