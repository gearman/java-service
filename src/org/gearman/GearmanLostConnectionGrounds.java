package org.gearman;

import java.io.Serializable;

/**
 * Defines why a method is being called
 * @author isaiah
 */
public enum GearmanLostConnectionGrounds implements Serializable {
	/**
	 * The server in question unexpectedly disconnected.
	 */
	UNEXPECTED_DISCONNECT,
	
	/**
	 * The gearman service failed to connect to a server registed with the service  
	 */
	FAILED_CONNECTION,
	
	/** 
	 * The connection was closed due to a server failing to respond
	 */
	RESPONSE_TIMEOUT,
}
