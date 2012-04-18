/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

import java.io.Serializable;

/**
 * An enumerator used to tell the framework what action to take when a remote server unexpectedly
 * disconnects. This enumerator is used by the {@link GearmanLostConnectionPolicy} interface 
 * @author isaiah
 *
 */
public enum GearmanLostConnectionAction implements Serializable {
	/** Drop the server from the service */
	DROP,
	
	/** Attempt to reconnect to the server */
	RECONNECT;
}
