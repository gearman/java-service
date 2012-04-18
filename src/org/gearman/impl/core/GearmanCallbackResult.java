/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.core;

public interface GearmanCallbackResult {
	
	/**
	 * Tests if the operation completed successfully
	 * @return
	 * 		<code>true</code> if and only if the operation was successful
	 */
	public boolean isSuccessful();
}
