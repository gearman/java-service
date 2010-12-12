package org.gearman;

import java.io.IOException;

/**
 * The class that starts the standalone server
 * @author isaiah.v
 */
class Main {
	
	/**
	 * Starts the standalone gearman job server.
	 * @throws IOException 
	 */
	public static void main(final String[] args) throws IOException {
		Gearman gearman = new Gearman();
		gearman.createGearmanServer().openPort();
	}
}
