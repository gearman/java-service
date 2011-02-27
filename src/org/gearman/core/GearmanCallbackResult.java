package org.gearman.core;

public interface GearmanCallbackResult {
	
	/**
	 * Tests if the operation completed successfully
	 * @return
	 * 		<code>true</code> if and only if the operation was successful
	 */
	public boolean isSuccessful();
}
